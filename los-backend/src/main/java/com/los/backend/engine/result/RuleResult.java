package com.los.backend.engine.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;




@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResult {

    
    private String ruleName;

    
    private String ruleType;

    
    private boolean passed;

    
    private String reason;

    
    private double weight;

    
    private String actualValue;

    
    private String thresholdValue;

    
    private double partialRiskScore;

    
    private String configSource;
}
