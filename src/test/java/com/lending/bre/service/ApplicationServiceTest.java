package com.lending.bre.service;

import com.lending.bre.model.ApplicationResponseDto;
import com.lending.bre.model.LoanApplication;
import com.lending.bre.repository.LoanApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/*
 * UNIT TESTING WITH MOCKITO
 * Unit tests focus on exactly one class at a time. We use Mockito to create "fake" (mock) versions 
 * of our database repositories so we don't need a real database running to test our business logic.
 */

// Enable Mockito features in this JUnit 5 test class.
@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    // Create a fake, programmable version of the database repository.
    @Mock
    private LoanApplicationRepository repository;

    // Create a real instance of our service, and inject the fake repository into it.
    @InjectMocks
    private ApplicationService applicationService;

    private LoanApplication mockApp;

    // This runs before every single test to give us a fresh setup.
    @BeforeEach
    void setUp() {
        mockApp = new LoanApplication();
        mockApp.setId("app-123");
        mockApp.setStatus("SUBMITTED");
        mockApp.setRequestedAmount(5000);
    }

    // HAPPY PATH: Test that finding an application works when it actually exists in the database.
    @Test
    void getApplication_ShouldReturnApplication_WhenFound() {
        // Arrange: Program the fake repository to return our mock application when asked for "app-123".
        when(repository.findById("app-123")).thenReturn(Optional.of(mockApp));

        // Act: Call the real service method.
        LoanApplication result = applicationService.getApplication("app-123");

        // Assert: Verify the result is exactly what we expected.
        assertNotNull(result, "Result should not be null");
        assertEquals("SUBMITTED", result.getStatus(), "Status should match");
        
        // Verify that the service actually asked the repository for the data exactly once.
        verify(repository, times(1)).findById("app-123");
    }

    // EDGE CASE 1: Test how the service handles a request for an ID that doesn't exist.
    @Test
    void getApplication_ShouldThrowException_WhenNotFound() {
        // Arrange: Program the fake repository to return an empty result (simulating "not found").
        when(repository.findById("invalid-id")).thenReturn(Optional.empty());

        // Act & Assert: Verify that calling the method throws a RuntimeException.
        // We expect a crash here, so throwing an exception means the test PASSES.
        Exception exception = assertThrows(RuntimeException.class, () -> {
            applicationService.getApplication("invalid-id");
        });

        // Verify the error message is correct.
        assertTrue(exception.getMessage().contains("not found"), "Error message should indicate not found");
    }

    // EDGE CASE 2: Test State Transition rules (e.g., you cannot change an APPROVED loan back to PENDING).
    @Test
    void updateStatus_ShouldThrowException_WhenInvalidStateTransition() {
        // Arrange: Start with an application that is already APPROVED.
        mockApp.setStatus("APPROVED");
        when(repository.findById("app-123")).thenReturn(Optional.of(mockApp));

        // Act & Assert: Attempting to downgrade it to SUBMITTED should trigger our business logic protection.
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            applicationService.updateStatus("app-123", "SUBMITTED");
        });

        // Verify it failed for the right reason.
        assertTrue(exception.getMessage().contains("Invalid state transition"), "Should block backwards state change");
        
        // Verify we NEVER called the database 'save' method because it was blocked.
        verify(repository, never()).save(any(LoanApplication.class));
    }
}