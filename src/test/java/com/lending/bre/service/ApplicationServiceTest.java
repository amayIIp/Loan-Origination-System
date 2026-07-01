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




@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    
    @Mock
    private LoanApplicationRepository repository;

    
    @InjectMocks
    private ApplicationService applicationService;

    private LoanApplication mockApp;

    
    @BeforeEach
    void setUp() {
        mockApp = new LoanApplication();
        mockApp.setId("app-123");
        mockApp.setStatus("SUBMITTED");
        mockApp.setRequestedAmount(5000);
    }

    
    @Test
    void getApplication_ShouldReturnApplication_WhenFound() {
        
        when(repository.findById("app-123")).thenReturn(Optional.of(mockApp));

        
        LoanApplication result = applicationService.getApplication("app-123");

        
        assertNotNull(result, "Result should not be null");
        assertEquals("SUBMITTED", result.getStatus(), "Status should match");
        
        
        verify(repository, times(1)).findById("app-123");
    }

    
    @Test
    void getApplication_ShouldThrowException_WhenNotFound() {
        
        when(repository.findById("invalid-id")).thenReturn(Optional.empty());

        
        
        Exception exception = assertThrows(RuntimeException.class, () -> {
            applicationService.getApplication("invalid-id");
        });

        
        assertTrue(exception.getMessage().contains("not found"), "Error message should indicate not found");
    }

    
    @Test
    void updateStatus_ShouldThrowException_WhenInvalidStateTransition() {
        
        mockApp.setStatus("APPROVED");
        when(repository.findById("app-123")).thenReturn(Optional.of(mockApp));

        
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            applicationService.updateStatus("app-123", "SUBMITTED");
        });

        
        assertTrue(exception.getMessage().contains("Invalid state transition"), "Should block backwards state change");
        
        
        verify(repository, never()).save(any(LoanApplication.class));
    }
}