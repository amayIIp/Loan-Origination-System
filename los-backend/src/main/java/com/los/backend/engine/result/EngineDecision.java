package com.los.backend.engine.result;

import com.los.backend.model.enums.FinalDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngineDecision {

    
    private FinalDecision finalDecision;

    
    private double riskScore;

    
    private int roundedRiskScore;

    
    private double approvalThresholdUsed;

    
    private List<RuleResult> ruleResults;

    
    private Instant evaluatedAt;

    
    private String engineVersion;

    
    private int totalRulesEvaluated;

    
    private int rulesPassed;

    
    private String overallComment;

    
    private List<String> missingDataFields;
}
