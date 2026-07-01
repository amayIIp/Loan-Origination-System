package com.los.backend.engine.rule;

import com.los.backend.engine.context.ApplicantEvaluationContext;
import com.los.backend.engine.result.RuleResult;
import com.los.backend.model.enums.RuleType;


public interface Rule {

    
    RuleResult evaluate(ApplicantEvaluationContext context);

    
    RuleType getRuleType();

    
    default String getRuleName() {
        return this.getClass().getSimpleName();
    }

    
    default boolean isCritical() {
        return false;
    }
}
