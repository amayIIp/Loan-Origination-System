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

/*
 * UNIT TESTING WITH MOCKITO
 * A Unit Test checks a single class in isolation. To test a Rule without needing a real MongoDB database 
 * running, we use "Mockito" to create a "Mock" (fake) repository that returns whatever we tell it to.
 */

// Tell JUnit to enable Mockito features for this test class.
@ExtendWith(MockitoExtension.class)
class RuleTests {

    // Create a fake version of the CreditRuleRepository.
    @Mock
    private CreditRuleRepository repository;

    // The rules we want to test.
    private CreditScoreRule creditScoreRule;
    private DebtToIncomeRatioRule dtiRule;
    private EmploymentStatusRule employmentRule;

    // This method runs before every single @Test, ensuring a clean slate.
    @BeforeEach
    void setUp() {
        // Initialize the rules with the fake repository.
        creditScoreRule = new CreditScoreRule(repository);
        dtiRule = new DebtToIncomeRatioRule(repository);
        employmentRule = new EmploymentStatusRule(repository);
    }

    @Test
    void creditScoreRule_ShouldPass_WhenScoreAboveThreshold() {
        // Arrange: Tell the fake repository to return a rule requiring a 650 score.
        when(repository.findByRuleTypeAndActiveTrue("CreditScoreRule"))
            .thenReturn(Optional.of(new CreditRule("CreditScoreRule", 650.0, 1.0, true)));
        
        // Create a context where the applicant has a score of 700.
        ApplicantEvaluationContext context = new ApplicantEvaluationContext("app1", 700, 30, 5000, 1000, 20000, "EMPLOYED");

        // Act: Run the rule.
        RuleResult result = creditScoreRule.evaluate(context);

        // Assert: Verify the result is what we expect (it should pass).
        assertTrue(result.isPassed(), "Rule should pass when score is above 650");
        assertEquals(1.0, result.getWeight(), "Weight should match the config");
    }

    @Test
    void dtiRule_ShouldFail_WhenDtiAboveThreshold() {
        // Arrange: Tell the fake repository to return a rule requiring DTI <= 0.40.
        when(repository.findByRuleTypeAndActiveTrue("DebtToIncomeRatioRule"))
            .thenReturn(Optional.of(new CreditRule("DebtToIncomeRatioRule", 0.40, 1.5, true)));
        
        // Create a context where the applicant has 5000 income and 3000 debt (DTI = 0.60, which is bad).
        ApplicantEvaluationContext context = new ApplicantEvaluationContext("app2", 700, 30, 5000, 3000, 20000, "EMPLOYED");

        // Act: Run the rule.
        RuleResult result = dtiRule.evaluate(context);

        // Assert: Verify it fails.
        assertFalse(result.isPassed(), "Rule should fail when DTI is above 0.40");
    }
    
    @Test
    void dtiRule_EdgeCase_ShouldFail_WhenIncomeIsZero() {
        // Arrange: Use the default config if the repository returns empty.
        when(repository.findByRuleTypeAndActiveTrue("DebtToIncomeRatioRule")).thenReturn(Optional.empty());
        
        // Context with 0 income to test division by zero protection.
        ApplicantEvaluationContext context = new ApplicantEvaluationContext("app3", 700, 30, 0, 1000, 20000, "EMPLOYED");

        // Act: Run the rule.
        RuleResult result = dtiRule.evaluate(context);

        // Assert: Verify it safely fails without crashing.
        assertFalse(result.isPassed(), "Rule should fail safely when income is 0");
        assertTrue(result.getReason().contains("zero"), "Reason should mention zero income");
    }

    @Test
    void employmentRule_ShouldPass_WhenSelfEmployed() {
        // Arrange: Return empty optional to trigger the default configuration logic.
        when(repository.findByRuleTypeAndActiveTrue("EmploymentStatusRule")).thenReturn(Optional.empty());
        
        // Context with SELF_EMPLOYED status.
        ApplicantEvaluationContext context = new ApplicantEvaluationContext("app4", 700, 30, 5000, 1000, 20000, "SELF_EMPLOYED");

        // Act
        RuleResult result = employmentRule.evaluate(context);

        // Assert
        assertTrue(result.isPassed(), "Rule should pass for SELF_EMPLOYED");
    }
}