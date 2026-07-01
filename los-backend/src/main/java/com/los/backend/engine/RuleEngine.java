package com.los.backend.engine;

import com.los.backend.engine.context.ApplicantEvaluationContext;
import com.los.backend.engine.registry.RuleRegistry;
import com.los.backend.engine.result.EngineDecision;
import com.los.backend.engine.result.RuleResult;
import com.los.backend.engine.rule.Rule;
import com.los.backend.model.enums.FinalDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Component
@Slf4j
public class RuleEngine {

    
    public static final String ENGINE_VERSION = "1.0.0";

    
    @Value("${los.engine.approval-threshold:40.0}")
    private double approvalThreshold;

    
    @Value("${los.engine.review-band-width:15.0}")
    private double reviewBandWidth;

    private final RuleRegistry ruleRegistry;

    public RuleEngine(RuleRegistry ruleRegistry) {
        this.ruleRegistry = ruleRegistry;
    }

    
    public EngineDecision evaluate(ApplicantEvaluationContext context) {
        long startMs = System.currentTimeMillis(); 

        log.info("[RuleEngine] START evaluation | applicationId={} | applicantId={} | engineVersion={}",
                 context.getApplicationId(), context.getApplicantId(), ENGINE_VERSION);

        
        
        
        
        List<Rule> sortedRules = ruleRegistry.getAllRules().stream()
            .sorted(Comparator.comparingDouble(r -> -r.evaluate(
                
                
                buildWeightProbeContext()
            ).getWeight()))
            .collect(Collectors.toList());

        
        List<RuleResult> results = new ArrayList<>();
        List<String> missingDataFields = new ArrayList<>();

        for (Rule rule : sortedRules) {
            RuleResult result;
            try {
                result = rule.evaluate(context);
            } catch (Exception ex) {
                
                
                
                
                log.error("[RuleEngine] Rule {} threw exception — treating as FAIL: {}",
                          rule.getRuleName(), ex.getMessage(), ex);
                result = RuleResult.builder()
                    .ruleName(rule.getRuleName())
                    .ruleType(rule.getRuleType().name())
                    .passed(false)
                    .reason("Rule evaluation failed due to an internal error: " + ex.getMessage())
                    .weight(10.0)   
                    .partialRiskScore(10.0)
                    .configSource("ERROR")
                    .build();
            }

            results.add(result);

            
            if ("DEFAULT".equals(result.getConfigSource()) && !result.isPassed()) {
                missingDataFields.add(rule.getRuleType().name() + " config");
            }

            log.debug("[RuleEngine] Rule '{}' | passed={} | reason='{}'",
                      result.getRuleName(), result.isPassed(), result.getReason());

            
            
            
            
            if (rule.isCritical() && !result.isPassed()) {
                log.warn("[RuleEngine] CRITICAL RULE FAILED: '{}' — SHORT-CIRCUIT → REJECTED | applicationId={}",
                         rule.getRuleName(), context.getApplicationId());

                long durationMs = System.currentTimeMillis() - startMs;
                return buildDecision(
                    FinalDecision.REJECTED,
                    100.0,              
                    approvalThreshold,
                    results,
                    missingDataFields,
                    durationMs,
                    "CRITICAL RULE FAILED: " + rule.getRuleName() + ". " + result.getReason()
                );
            }
        }

        
        
        
        
        double totalWeight = results.stream().mapToDouble(RuleResult::getWeight).sum();
        double rawRiskScore;

        if (totalWeight == 0) {
            
            rawRiskScore = 100.0;
            log.error("[RuleEngine] totalWeight=0 — no rules were evaluated! Returning max risk score.");
        } else {
            double totalPartialRisk = results.stream().mapToDouble(RuleResult::getPartialRiskScore).sum();
            
            rawRiskScore = (totalPartialRisk / totalWeight) * 100.0;
        }

        log.info("[RuleEngine] Aggregation | applicationId={} | rawRiskScore={:.2f} | approvalThreshold={} | reviewBand={}",
                 context.getApplicationId(), rawRiskScore, approvalThreshold, reviewBandWidth);

        
        FinalDecision finalDecision;
        if (rawRiskScore <= approvalThreshold) {
            finalDecision = FinalDecision.APPROVED;
        } else if (rawRiskScore <= approvalThreshold + reviewBandWidth) {
            
            
            finalDecision = FinalDecision.REVIEW;
        } else {
            finalDecision = FinalDecision.REJECTED;
        }

        long durationMs = System.currentTimeMillis() - startMs;

        log.info("[RuleEngine] COMPLETE | applicationId={} | decision={} | riskScore={:.1f} | rulesEvaluated={} | durationMs={}",
                 context.getApplicationId(), finalDecision, rawRiskScore, results.size(), durationMs);

        
        return buildDecision(finalDecision, rawRiskScore, approvalThreshold,
                             results, missingDataFields, durationMs, null);
    }

    

    
    private EngineDecision buildDecision(FinalDecision finalDecision, double rawRiskScore,
                                          double thresholdUsed, List<RuleResult> results,
                                          List<String> missingDataFields, long durationMs,
                                          String overrideComment) {
        int rulesPassed = (int) results.stream().filter(RuleResult::isPassed).count();

        
        String comment = overrideComment != null ? overrideComment : buildComment(finalDecision, rawRiskScore, results);

        return EngineDecision.builder()
            .finalDecision(finalDecision)
            .riskScore(rawRiskScore)
            .roundedRiskScore((int) Math.round(rawRiskScore))
            .approvalThresholdUsed(thresholdUsed)
            .ruleResults(results)
            .evaluatedAt(Instant.now())
            .engineVersion(ENGINE_VERSION)
            .totalRulesEvaluated(results.size())
            .rulesPassed(rulesPassed)
            .overallComment(comment)
            .missingDataFields(missingDataFields.isEmpty() ? null : missingDataFields)
            .build();
    }

    
    private String buildComment(FinalDecision decision, double riskScore, List<RuleResult> results) {
        List<String> failReasons = results.stream()
            .filter(r -> !r.isPassed())
            .map(r -> String.format("[%s] %s", r.getRuleType(), r.getReason()))
            .collect(Collectors.toList());

        if (failReasons.isEmpty()) {
            return String.format("%s. Risk score: %.0f/100. All %d rules passed.",
                                 decision, riskScore, results.size());
        }
        return String.format("%s. Risk score: %.0f/100. %d of %d rules failed: %s",
                             decision, riskScore,
                             failReasons.size(), results.size(),
                             String.join("; ", failReasons));
    }

    
    private ApplicantEvaluationContext buildWeightProbeContext() {
        return ApplicantEvaluationContext.builder()
            .applicantId("__weight_probe__")
            .applicationId("__weight_probe__")
            .build();
    }
}
