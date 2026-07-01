package com.los.backend.exception;


public class BusinessRuleException extends RuntimeException {

    
    private final String ruleCode;

    public BusinessRuleException(String ruleCode, String message) {
        super(message);
        this.ruleCode = ruleCode;
    }

    public BusinessRuleException(String message) {
        super(message);
        this.ruleCode = "BUSINESS_RULE_VIOLATION";
    }

    public String getRuleCode() { return ruleCode; }
}
