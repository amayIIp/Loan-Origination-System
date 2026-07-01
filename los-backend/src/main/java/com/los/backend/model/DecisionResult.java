package com.los.backend.model;



import com.los.backend.model.enums.FinalDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionResult {

    
    @Field("final_decision")
    private FinalDecision finalDecision;

    
    @Field("risk_score")
    private Integer riskScore;

    
    @Field("evaluated_at")
    private Instant evaluatedAt;

    
    @Field("evaluator_version")
    private String evaluatorVersion;

    
    @Field("rule_outcomes")
    private List<RuleOutcomeSummary> ruleOutcomes;

    
    @Field("overall_comment")
    private String overallComment;

    
    
    
    

    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleOutcomeSummary {

        
        @Field("rule_name")
        private String ruleName;

        
        @Field("passed")
        private boolean passed;

        
        @Field("reason")
        private String reason;

        
        @Field("actual_value")
        private String actualValue;

        
        @Field("threshold_value")
        private String thresholdValue;
    }
}
