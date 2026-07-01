package com.lending.bre.integration;

import com.lending.bre.model.ApplicantEvaluationContext;
import com.lending.bre.model.CreditRule;
import com.lending.bre.model.LoanApplication;
import com.lending.bre.repository.CreditRuleRepository;
import com.lending.bre.repository.LoanApplicationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;




@SpringBootTest

@AutoConfigureMockMvc

@Testcontainers
class ApplicationIntegrationTest {

    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    
    @Autowired
    private MockMvc mockMvc;

    
    @Autowired
    private LoanApplicationRepository applicationRepository;

    @Autowired
    private CreditRuleRepository creditRuleRepository;

    private LoanApplication testApp;

    
    @BeforeEach
    void setupDatabase() {
        
        creditRuleRepository.save(new CreditRule("CreditScoreRule", 650, 1.0, true));
        creditRuleRepository.save(new CreditRule("DebtToIncomeRatioRule", 0.40, 1.5, true));

        
        testApp = new LoanApplication();
        testApp.setCreditScore(700);        
        testApp.setMonthlyIncome(5000);
        testApp.setMonthlyDebt(1000);       
        testApp.setRequestedAmount(10000);
        testApp.setEmploymentStatus("EMPLOYED");
        testApp.setStatus("SUBMITTED");
        testApp = applicationRepository.save(testApp);
    }

    
    @AfterEach
    void cleanUp() {
        applicationRepository.deleteAll();
        creditRuleRepository.deleteAll();
    }

    
    @Test
    void evaluateApplication_ShouldApproveAndSaveToDatabase_WhenApplicantMeetsCriteria() throws Exception {
        
        mockMvc.perform(post("/api/applications/" + testApp.getId() + "/evaluate")
                .contentType(MediaType.APPLICATION_JSON))
                
                .andExpect(status().isOk())
                
                .andExpect(jsonPath("$.approved").value(true));

        
        LoanApplication updatedApp = applicationRepository.findById(testApp.getId()).orElseThrow();
        
        assert(updatedApp.getStatus().equals("APPROVED"));
    }
}