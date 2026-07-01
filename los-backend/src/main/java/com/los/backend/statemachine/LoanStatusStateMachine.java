package com.los.backend.statemachine;

import com.los.backend.exception.InvalidStateTransitionException;
import com.los.backend.model.enums.LoanStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;




@Component
public class LoanStatusStateMachine {

    
    private static final Map<LoanStatus, Set<LoanStatus>> ALLOWED_TRANSITIONS;

    static {
        
        
        ALLOWED_TRANSITIONS = new EnumMap<>(LoanStatus.class);

        
        ALLOWED_TRANSITIONS.put(
            LoanStatus.SUBMITTED,
            EnumSet.of(LoanStatus.KYC_PENDING, LoanStatus.REJECTED)
        );

        
        
        ALLOWED_TRANSITIONS.put(
            LoanStatus.KYC_PENDING,
            EnumSet.of(LoanStatus.UNDER_REVIEW, LoanStatus.REJECTED)
        );

        
        
        ALLOWED_TRANSITIONS.put(
            LoanStatus.UNDER_REVIEW,
            EnumSet.of(LoanStatus.CREDIT_CHECK, LoanStatus.REJECTED, LoanStatus.KYC_PENDING)
        );

        
        
        ALLOWED_TRANSITIONS.put(
            LoanStatus.CREDIT_CHECK,
            EnumSet.of(LoanStatus.APPROVED, LoanStatus.REJECTED, LoanStatus.UNDER_REVIEW)
        );

        
        
        ALLOWED_TRANSITIONS.put(
            LoanStatus.APPROVED,
            EnumSet.of(LoanStatus.DISBURSED, LoanStatus.REJECTED)
        );

        
        
        ALLOWED_TRANSITIONS.put(
            LoanStatus.DISBURSED,
            EnumSet.noneOf(LoanStatus.class)
        );

        
        
        ALLOWED_TRANSITIONS.put(
            LoanStatus.REJECTED,
            EnumSet.noneOf(LoanStatus.class)
        );
    }

    

    
    public void validate(LoanStatus currentStatus, LoanStatus newStatus) {
        
        Set<LoanStatus> legalTargets = ALLOWED_TRANSITIONS.get(currentStatus);

        if (legalTargets == null) {
            
            
            
            throw new InvalidStateTransitionException(
                currentStatus, newStatus,
                "No transition rules are defined for source status '" + currentStatus +
                "'. Update LoanStatusStateMachine.ALLOWED_TRANSITIONS."
            );
        }

        if (!legalTargets.contains(newStatus)) {
            
            throw new InvalidStateTransitionException(
                currentStatus, newStatus,
                "Allowed next statuses from '" + currentStatus + "' are: " + legalTargets
            );
        }

        
    }

    
    public boolean isTerminal(LoanStatus status) {
        Set<LoanStatus> targets = ALLOWED_TRANSITIONS.get(status);
        
        return targets == null || targets.isEmpty();
    }

    
    public Set<LoanStatus> getAllowedTransitions(LoanStatus currentStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(
            currentStatus,
            EnumSet.noneOf(LoanStatus.class)
        );
    }
}
