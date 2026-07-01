package com.los.backend.model;



import com.los.backend.model.enums.FinalDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "decision_logs")

@CompoundIndex(
    name = "idx_app_evaluated",
    def = "{'loan_application_id': 1, 'evaluated_at': -1}",
    background = true
)
public class DecisionLog {

    
    @Id
    private String id;

    
    @NotBlank(message = "Loan application ID is required")
    @Indexed
    @Field("loan_application_id")
    private String loanApplicationId;

    
    @NotBlank
    @Field("applicant_id")
    private String applicantId;

    
    @NotNull
    @Field("evaluated_at")
    private Instant evaluatedAt;

    
    @Field("rule_results")
    private List<RuleResult> ruleResults;

    
    @Field("overall_risk_score")
    private Integer overallRiskScore;

    
    @NotNull
    @Field("final_decision")
    private FinalDecision finalDecision;

    
    @NotBlank
    @Field("evaluator_version")
    private String evaluatorVersion;

    
    @Field("rules_snapshot_version")
    private String rulesSnapshotVersion;

    
    @Field("triggered_by_user_id")
    private String triggeredByUserId;

    
    @Field("duration_ms")
    private Long durationMs;

    
    @Field("decision_comment")
    private String decisionComment;

    
    
    

    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleResult {

        
        @Field("rule_id")
        private String ruleId;

        
        @Field("rule_name")
        private String ruleName;

        
        @Field("rule_type")
        private String ruleType;

        
        @Field("rule_version")
        private Integer ruleVersion;

        
        @Field("passed")
        private boolean passed;

        
        @Field("actual_value")
        private String actualValue;

        
        @Field("threshold_value")
        private String thresholdValue;

        
        @Field("reason")
        private String reason;

        
        @Field("rule_weight")
        private Integer ruleWeight;

        
        @Field("partial_risk_score")
        private Integer partialRiskScore;
    }
}
