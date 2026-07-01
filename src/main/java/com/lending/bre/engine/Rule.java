package com.lending.bre.engine;

import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.RuleResult;




public interface Rule {
    
    
    RuleResult evaluate(ApplicantEvaluationContext context);
    
    
    String getRuleType();
}