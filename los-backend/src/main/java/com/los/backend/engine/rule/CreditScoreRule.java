package com.los.backend.engine.rule;

import com.los.backend.engine.context.ApplicantEvaluationContext;
import com.los.backend.engine.result.RuleResult;
import com.los.backend.model.CreditRule;
import com.los.backend.model.enums.RuleType;
import com.los.backend.repository.CreditRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Component  
@Slf4j
public class CreditScoreRule implements Rule {

    
    
    private static final int DEFAULT_MINIMUM_SCORE = 650;

    
    private static final double DEFAULT_WEIGHT = 30.0;

    
    private static final int DEFAULT_NTC_PENALTY_SCORE = 550;

    
    private final CreditRuleRepository creditRuleRepository;

    public CreditScoreRule(CreditRuleRepository creditRuleRepository) {
        this.creditRuleRepository = creditRuleRepository;
    }

    

    @Override
    public RuleType getRuleType() {
        
        return RuleType.CREDIT_SCORE;
    }

    @Override
    public String getRuleName() {
        return "Minimum CIBIL Credit Score Check";
    }

    
    @Override
    public RuleResult evaluate(ApplicantEvaluationContext context) {
        log.debug("[CreditScoreRule] Evaluating applicantId={} creditScore={}",
                  context.getApplicantId(), context.getCreditScore());

        
        
        
        
        List<CreditRule> activeRules = creditRuleRepository.findByRuleTypeAndIsActiveTrue(RuleType.CREDIT_SCORE);

        int minimumScore;
        double weight;
        int ntcPenaltyScore;
        String configSource;

        if (activeRules.isEmpty()) {
            
            
            minimumScore   = DEFAULT_MINIMUM_SCORE;
            weight         = DEFAULT_WEIGHT;
            ntcPenaltyScore = DEFAULT_NTC_PENALTY_SCORE;
            configSource   = "DEFAULT";
            log.warn("[CreditScoreRule] No active CreditRule found for CREDIT_SCORE — using defaults");
        } else {
            
            Map<String, Object> params = activeRules.get(0).getParameters();
            
            minimumScore   = params.containsKey("minimumScore")
                ? ((Number) params.get("minimumScore")).intValue() : DEFAULT_MINIMUM_SCORE;
            ntcPenaltyScore = params.containsKey("ntcPenaltyScore")
                ? ((Number) params.get("ntcPenaltyScore")).intValue() : DEFAULT_NTC_PENALTY_SCORE;
            weight         = activeRules.get(0).getWeight() != null
                ? activeRules.get(0).getWeight().doubleValue() : DEFAULT_WEIGHT;
            configSource   = "MONGODB";
        }

        
        Integer score = context.getCreditScore();
        if (score == null) {
            
            boolean ntcPasses = ntcPenaltyScore >= minimumScore;
            String reason = String.format(
                "Credit score not available (New-To-Credit applicant). " +
                "Applying NTC penalty score of %d. Required minimum: %d. %s",
                ntcPenaltyScore, minimumScore, ntcPasses ? "BORDERLINE PASS." : "FAIL — manual review required."
            );
            log.debug("[CreditScoreRule] NTC applicant applicantId={} → ntcScore={} threshold={} passed={}",
                      context.getApplicantId(), ntcPenaltyScore, minimumScore, ntcPasses);

            return RuleResult.builder()
                .ruleName(getRuleName())
                .ruleType(getRuleType().name())
                .passed(ntcPasses)
                .reason(reason)
                .weight(weight)
                .actualValue("NTC/" + ntcPenaltyScore)
                .thresholdValue(String.valueOf(minimumScore))
                .partialRiskScore(ntcPasses ? 0 : weight)
                .configSource(configSource)
                .build();
        }

        
        boolean passed = score >= minimumScore;
        String reason = passed
            ? String.format("Credit score %d ≥ minimum threshold %d. PASS.", score, minimumScore)
            : String.format("Credit score %d is below the minimum required score of %d. FAIL.", score, minimumScore);

        log.debug("[CreditScoreRule] applicantId={} score={} threshold={} passed={}",
                  context.getApplicantId(), score, minimumScore, passed);

        return RuleResult.builder()
            .ruleName(getRuleName())
            .ruleType(getRuleType().name())
            .passed(passed)
            .reason(reason)
            .weight(weight)
            .actualValue(String.valueOf(score))
            .thresholdValue(String.valueOf(minimumScore))
            .partialRiskScore(passed ? 0.0 : weight)
            .configSource(configSource)
            .build();
    }
}
