package com.los.backend.model;



import com.los.backend.model.enums.RuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credit_rules")
public class CreditRule {

    
    @Id
    @NotBlank(message = "Rule ID is required")
    private String id;

    
    @NotBlank(message = "Rule name is required")
    @Size(max = 200)
    @Field("rule_name")
    private String ruleName;

    
    @NotNull(message = "Rule type is required")
    @Indexed
    @Field("rule_type")
    private RuleType ruleType;

    
    @Size(max = 1000)
    @Field("description")
    private String description;

    
    @NotNull(message = "Rule parameters are required")
    @Field("parameters")
    private Map<String, Object> parameters;

    
    @Min(value = 1, message = "Rule weight must be at least 1")
    @Max(value = 100, message = "Rule weight cannot exceed 100")
    @NotNull
    @Field("weight")
    @Builder.Default
    private Integer weight = 10;

    
    @Indexed
    @Field("is_active")
    @Builder.Default
    private boolean isActive = true;

    
    @Min(value = 1)
    @NotNull
    @Field("version")
    @Builder.Default
    private Integer version = 1;

    
    @Field("applicable_product_types")
    private java.util.List<String> applicableProductTypes;

    

    
    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    
    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;

    
    @Field("modified_by")
    private String modifiedBy;
}
