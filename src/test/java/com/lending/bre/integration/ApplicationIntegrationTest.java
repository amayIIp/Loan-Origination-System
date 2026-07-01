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

/*
 * TESTCONTAINERS INTEGRATION TESTING
 * Unlike unit tests, integration tests run the ENTIRE application stack (Controllers, Services, Repositories).
 * To avoid polluting a real database, we use Testcontainers, which spins up a temporary, throwaway 
 * Docker container running a real MongoDB database just for this test, and destroys it when finished.
 */

// Tell Spring Boot to load the full application context.
@SpringBootTest
// Enable auto-configuration of MockMvc (a tool to simulate HTTP requests without starting a real web server).
@AutoConfigureMockMvc
// Tell JUnit we are using Testcontainers.
@Testcontainers
class ApplicationIntegrationTest {

    // Define the MongoDB Docker container. We use the official mongo:6.0 image.
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    // This tells Spring Boot to connect to our temporary Docker database instead of localhost:27017.
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    // A tool to simulate HTTP requests to our controllers.
    @Autowired
    private MockMvc mockMvc;

    // Real repositories connected to the Docker database.
    @Autowired
    private LoanApplicationRepository applicationRepository;

    @Autowired
    private CreditRuleRepository creditRuleRepository;

    private LoanApplication testApp;

    // Run before each test to put the database in a known state.
    @BeforeEach
    void setupDatabase() {
        // 1. Seed the Rule Engine Configuration in MongoDB.
        creditRuleRepository.save(new CreditRule("CreditScoreRule", 650, 1.0, true));
        creditRuleRepository.save(new CreditRule("DebtToIncomeRatioRule", 0.40, 1.5, true));

        // 2. Create a test application in the database.
        testApp = new LoanApplication();
        testApp.setCreditScore(700);        // Passes Credit rule (700 > 650)
        testApp.setMonthlyIncome(5000);
        testApp.setMonthlyDebt(1000);       // Passes DTI rule (1000/5000 = 0.20 <= 0.40)
        testApp.setRequestedAmount(10000);
        testApp.setEmploymentStatus("EMPLOYED");
        testApp.setStatus("SUBMITTED");
        testApp = applicationRepository.save(testApp);
    }

    // Clean up after every test so data doesn't leak between tests.
    @AfterEach
    void cleanUp() {
        applicationRepository.deleteAll();
        creditRuleRepository.deleteAll();
    }

    // END-TO-END TEST: Hit the REST endpoint and verify the database changes.
    @Test
    void evaluateApplication_ShouldApproveAndSaveToDatabase_WhenApplicantMeetsCriteria() throws Exception {
        // Act: Simulate an HTTP POST request to trigger the evaluation.
        mockMvc.perform(post("/api/applications/" + testApp.getId() + "/evaluate")
                .contentType(MediaType.APPLICATION_JSON))
                // Assert HTTP Level: Expect a 200 OK response.
                .andExpect(status().isOk())
                // Assert JSON Body: Expect the 'approved' flag to be true.
                .andExpect(jsonPath("$.approved").value(true));

        // Assert Database Level: Fetch the application directly from the database to ensure it updated.
        LoanApplication updatedApp = applicationRepository.findById(testApp.getId()).orElseThrow();
        // Verify the status transition actually happened.
        assert(updatedApp.getStatus().equals("APPROVED"));
    }
}