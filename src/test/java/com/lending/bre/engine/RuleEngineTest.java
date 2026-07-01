package com.lending.bre.engine;

import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.DecisionResult;
import com.lending.bre.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RuleEngineTest {

    
    @Mock private Rule rule1;
    @Mock private Rule rule2;
    @Mock private Rule rule3;

    
    private RuleEngine engine;

    @BeforeEach
    void setUp() {
        
        engine = new RuleEngine(Arrays.asList(rule1, rule2, rule3));
    }

    @Test
    void evaluate_ShouldApprove_WhenScoreAbove70() {
        
        ApplicantEvaluationContext context = new ApplicantEvaluationContext("app1", 700, 30, 5000, 1000, 20000, "EMPLOYED");

        
        
        when(rule1.evaluate(context)).thenReturn(new RuleResult("Rule1", true, "Passed", 1.0));
        
        when(rule2.evaluate(context)).thenReturn(new RuleResult("Rule2", true, "Passed", 2.0));
        
        when(rule3.evaluate(context)).thenReturn(new RuleResult("Rule3", false, "Failed minor check", 1.0));

        
        DecisionResult result = engine.evaluate(context);

        
        
        
        
        assertTrue(result.isApproved(), "Engine should approve if score is 75%");
        assertEquals(75.0, result.getRiskScore(), "Score should be calculated as 75.0");
        assertEquals(3, result.getRuleResults().size(), "There should be 3 rule results in the list");
    }

    @Test
    void evaluate_ShouldReject_WhenHardFailTriggered() {
        
        ApplicantEvaluationContext context = new ApplicantEvaluationContext("app2", 700, 30, 5000, 1000, 20000, "EMPLOYED");

        
        when(rule1.evaluate(context)).thenReturn(new RuleResult("Rule1", true, "Passed", 1.0));
        
        when(rule2.evaluate(context)).thenReturn(new RuleResult("Rule2", false, "Failed critical check", 2.0));
        
        when(rule3.evaluate(context)).thenReturn(new RuleResult("Rule3", true, "Passed", 1.0));

        
        DecisionResult result = engine.evaluate(context);

        
        
        
        
        assertFalse(result.isApproved(), "Engine should reject when a high-weight rule fails");
    }
}