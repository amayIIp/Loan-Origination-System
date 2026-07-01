package com.los.backend.statemachine;

import com.los.backend.exception.InvalidStateTransitionException;
import com.los.backend.model.enums.LoanStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * LoanStatusStateMachine — enforces the loan application lifecycle state machine.
 *
 * What is a State Machine? (beginner explanation)
 * ────────────────────────────────────────────────
 * A state machine is a model that defines:
 *   - A fixed set of states (e.g., SUBMITTED, KYC_PENDING, APPROVED, etc.)
 *   - Which transitions between states are legal (allowed moves)
 *   - Which transitions are illegal (forbidden moves)
 *
 * Why enforce this in code and not just let the service update status freely?
 *   Without enforcement, a bug or a bad API call could move an application from
 *   SUBMITTED directly to DISBURSED (skipping KYC, review, and approval) —
 *   this would be a regulatory and financial disaster in a real lending system.
 *   The state machine is the guard that prevents impossible lifecycle leaps.
 *
 * VALID TRANSITIONS (from → to):
 * ──────────────────────────────────────────────────────────────────────────
 *   SUBMITTED    → KYC_PENDING              (system: after basic validation)
 *   SUBMITTED    → REJECTED                 (officer: immediate rejection)
 *   KYC_PENDING  → UNDER_REVIEW             (system: all KYC docs verified)
 *   KYC_PENDING  → REJECTED                 (officer: fraudulent/insufficient docs)
 *   UNDER_REVIEW → CREDIT_CHECK             (officer: triggers BRE evaluation)
 *   UNDER_REVIEW → REJECTED                 (officer: manual rejection)
 *   CREDIT_CHECK → APPROVED                 (system: BRE all rules passed)
 *   CREDIT_CHECK → REJECTED                 (system: BRE one or more rules failed)
 *   CREDIT_CHECK → UNDER_REVIEW             (officer: sends back for re-review)
 *   APPROVED     → DISBURSED                (officer: funds transferred)
 *   APPROVED     → REJECTED                 (officer: late rejection before disbursal)
 *
 * TERMINAL STATES (no outgoing transitions):
 *   DISBURSED — the loan is live; further state changes are in loan servicing system
 *   REJECTED  — final decision; applicant must submit a new application
 * ──────────────────────────────────────────────────────────────────────────
 *
 * Implementation choice:
 * We use an EnumMap<LoanStatus, EnumSet<LoanStatus>> — a Map where each source
 * status maps to the Set of legal target statuses. EnumMap and EnumSet are both
 * backed by arrays internally (not hash tables) making them the most memory-efficient
 * and fastest collections available in Java for enum keys/values.
 */
// @Component — registers this as a Spring-managed singleton bean that can be
// injected into services via @Autowired or constructor injection.
@Component
public class LoanStatusStateMachine {

    /**
     * ALLOWED_TRANSITIONS — the complete, authoritative transition table.
     * Declared as a static final constant so it is built ONCE at class load time
     * and shared across all threads — no synchronisation overhead, no re-creation
     * on every request.
     *
     * EnumMap (beginner explanation):
     * A regular HashMap<LoanStatus, ...> works, but EnumMap is specialised for
     * enum keys — it uses an array indexed by the enum's ordinal value instead
     * of computing hash codes. Faster and uses less memory.
     */
    private static final Map<LoanStatus, Set<LoanStatus>> ALLOWED_TRANSITIONS;

    static {
        // Initialise the transition table in a static block — runs once when
        // the class is first loaded by the JVM.
        ALLOWED_TRANSITIONS = new EnumMap<>(LoanStatus.class);

        // From SUBMITTED: can move to KYC_PENDING (normal flow) or REJECTED (immediate)
        ALLOWED_TRANSITIONS.put(
            LoanStatus.SUBMITTED,
            EnumSet.of(LoanStatus.KYC_PENDING, LoanStatus.REJECTED)
        );

        // From KYC_PENDING: move to UNDER_REVIEW once all docs verified,
        // or REJECTED if docs are fraudulent / insufficient
        ALLOWED_TRANSITIONS.put(
            LoanStatus.KYC_PENDING,
            EnumSet.of(LoanStatus.UNDER_REVIEW, LoanStatus.REJECTED)
        );

        // From UNDER_REVIEW: trigger BRE (CREDIT_CHECK) or manually reject.
        // Can also send back to KYC_PENDING if new documents are needed.
        ALLOWED_TRANSITIONS.put(
            LoanStatus.UNDER_REVIEW,
            EnumSet.of(LoanStatus.CREDIT_CHECK, LoanStatus.REJECTED, LoanStatus.KYC_PENDING)
        );

        // From CREDIT_CHECK: BRE either passes (APPROVED) or fails (REJECTED).
        // Can also send back to UNDER_REVIEW if a data issue requires human review.
        ALLOWED_TRANSITIONS.put(
            LoanStatus.CREDIT_CHECK,
            EnumSet.of(LoanStatus.APPROVED, LoanStatus.REJECTED, LoanStatus.UNDER_REVIEW)
        );

        // From APPROVED: either disburse (DISBURSED) or late-reject (REJECTED).
        // Cannot go back to earlier states — that would require a new application.
        ALLOWED_TRANSITIONS.put(
            LoanStatus.APPROVED,
            EnumSet.of(LoanStatus.DISBURSED, LoanStatus.REJECTED)
        );

        // DISBURSED is a terminal state — NO outgoing transitions allowed.
        // An empty EnumSet means zero legal transitions from this state.
        ALLOWED_TRANSITIONS.put(
            LoanStatus.DISBURSED,
            EnumSet.noneOf(LoanStatus.class)
        );

        // REJECTED is a terminal state — NO outgoing transitions allowed.
        // The applicant must submit a brand-new application to try again.
        ALLOWED_TRANSITIONS.put(
            LoanStatus.REJECTED,
            EnumSet.noneOf(LoanStatus.class)
        );
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * validate — checks whether transitioning from currentStatus to newStatus is legal.
     * Throws InvalidStateTransitionException if the transition is not in the table.
     *
     * This is a "guard" method — call it BEFORE persisting the status change.
     * If it does not throw, the transition is safe to persist.
     *
     * @param currentStatus the application's current LoanStatus
     * @param newStatus     the requested target LoanStatus
     * @throws InvalidStateTransitionException if the transition is illegal
     */
    public void validate(LoanStatus currentStatus, LoanStatus newStatus) {
        // Look up the set of legal targets for the current status
        Set<LoanStatus> legalTargets = ALLOWED_TRANSITIONS.get(currentStatus);

        if (legalTargets == null) {
            // This should never happen because we've mapped every enum value above.
            // If it does, it means a new LoanStatus enum value was added without
            // updating this state machine — programmer error, throw clearly.
            throw new InvalidStateTransitionException(
                currentStatus, newStatus,
                "No transition rules are defined for source status '" + currentStatus +
                "'. Update LoanStatusStateMachine.ALLOWED_TRANSITIONS."
            );
        }

        if (!legalTargets.contains(newStatus)) {
            // The requested transition is not in the allowed set — throw with details
            throw new InvalidStateTransitionException(
                currentStatus, newStatus,
                "Allowed next statuses from '" + currentStatus + "' are: " + legalTargets
            );
        }

        // If we reach here, the transition is valid — the caller can proceed.
    }

    /**
     * isTerminal — returns true if the given status has no outgoing transitions.
     *
     * Use this in service code to short-circuit checks:
     *   if (stateMachine.isTerminal(application.getStatus())) {
     *       throw new BusinessRuleException("Application is already in a final state");
     *   }
     *
     * @param status the LoanStatus to check
     * @return true if this status is DISBURSED or REJECTED (no further transitions)
     */
    public boolean isTerminal(LoanStatus status) {
        Set<LoanStatus> targets = ALLOWED_TRANSITIONS.get(status);
        // A status is terminal if its allowed transitions set is empty
        return targets == null || targets.isEmpty();
    }

    /**
     * getAllowedTransitions — returns the set of legal next statuses from a given status.
     * Useful for building "what can I do with this application?" UI menus.
     *
     * @param currentStatus the application's current status
     * @return Set of LoanStatus values that are legal next steps (empty set if terminal)
     */
    public Set<LoanStatus> getAllowedTransitions(LoanStatus currentStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(
            currentStatus,
            EnumSet.noneOf(LoanStatus.class)
        );
    }
}
