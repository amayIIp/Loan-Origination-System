package com.lending.bre.rules;

import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.CreditRule;
import com.lending.bre.model.RuleResult;
import com.lending.bre.repository.CreditRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;




@ExtendWith(MockitoExtension.class)
class RuleTests {

    
    @Mock
    private CreditRuleRepository repository;

    
    private CreditScoreRule creditScoreRule;
    private DebtToIncomeRatioRule dtiRule;
    private EmploymentStatusRule employmentRule;

    
    @BeforeEach
    void setUp() {
        
        creditScoreRule = new CreditScoreRule(repository);
        dtiRule = new DebtToIncomeRatioRule(repository);
        employmentRule = new EmploymentStatusRule(repository);
    }

    @Test
    void creditScoreRule_ShouldPass_WhenScoreAboveThreshold() {
        
        when(repository.findByRuleTypeAndActiveTrue("CreditScoreRule"))
            .thenReturn(Optional.of(new CreditRule("CreditScoreRule", 650.0, 1.0, true)));
        
        
        ApplicantEvaluationContext context = new ApplicantEvaluationContext("app1", 700, 30, 5000, 1000, 20000, "EMPLOYED");

        
        RuleResult result = creditScoreRule.evaluate(context);

        
        assertTrue(result.isPassed(), "Rule should pass when score is above 650");
        assertEquals(1.0, result.getWeight(), "Weight should match the config");
    }

    @Test
    void dtiRule_ShouldFail_WhenDtiAboveThreshold() {
        
        when(repository.findByRuleTypeAndActiveTrue("DebtToIncomeRatioRule"))
            .thenReturn(Optional.of(new CreditRule("DebtToIncomeRatioRule", 0.40, 1.5, true)));
        
        
        ApplicantEvaluationContext context = new ApplicantEvaluationContext("app2", 700, 30, 5000, 3000, 20000, "EMPLOYED");

        
        RuleResult result = dtiRule.evaluate(context);

        
        assertFalse(result.isPassed(), "Rule should fail when DTI is above 0.40");
    }
    
    @Test
    void dtiRule_EdgeCase_ShouldFail_WhenIncomeIsZero() {
        
        when(repository.findByRuleTypeAndActiveTrue("DebtToIncomeRatioRule")).thenReturn(Optional.empty());
        
        
        ApplicantEvaluationContext context = new ApplicantEvaluationContext("app3", 700, 30, 0, 1000, 20000, "EMPLOYED");

        
        RuleResult result = dtiRule.evaluate(context);

        
        assertFalse(result.isPassed(), "Rule should fail safely when income is 0");
        assertTrue(result.getReason().contains("zero"), "Reason should mention zero income");
    }

    @Test
    void employmentRule_ShouldPass_WhenSelfEmployed() {
        
        when(repository.findByRuleTypeAndActiveTrue("EmploymentStatusRule")).thenReturn(Optional.empty());
        
        
        ApplicantEvaluationContext context = new ApplicantEvaluationContext("app4", 700, 30, 5000, 1000, 20000, "SELF_EMPLOYED");

        
        RuleResult result = employmentRule.evaluate(context);

        
        assertTrue(result.isPassed(), "Rule should pass for SELF_EMPLOYED");
    }
}