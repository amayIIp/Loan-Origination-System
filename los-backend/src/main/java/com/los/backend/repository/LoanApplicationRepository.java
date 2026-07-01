package com.los.backend.repository;

import com.los.backend.model.LoanApplication;
import com.los.backend.model.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;


@Repository
public interface LoanApplicationRepository extends MongoRepository<LoanApplication, String> {

    
    
    List<LoanApplication> findByApplicantIdOrderByCreatedAtDesc(String applicantId);

    
    
    Page<LoanApplication> findByStatus(LoanStatus status, Pageable pageable);

    
    
    List<LoanApplication> findByStatusAndCreatedAtBetween(
        LoanStatus status,
        Instant from,
        Instant to
    );

    
    
    Optional<LoanApplication> findByApplicantIdAndStatus(String applicantId, LoanStatus status);

    
    
    long countByStatus(LoanStatus status);

    
    
    Page<LoanApplication> findByAssignedOfficerIdAndStatus(
        String officerId,
        LoanStatus status,
        Pageable pageable
    );

    
    
    @Query("{ 'status': ?0, 'status_updated_at': { $lt: ?1 } }")
    List<LoanApplication> findStalledApplications(LoanStatus status, Instant staleBefore);

    
    
    @Aggregation(pipeline = {
        
        
        
        "{ $group: { _id: '$status', totalAmount: { $sum: '$loan_amount' }, count: { $sum: 1 } } }",
        
        "{ $sort: { totalAmount: -1 } }"
    })
    List<Object[]> aggregateTotalLoanAmountByStatus();

    
    
    boolean existsByApplicantIdAndStatusIn(String applicantId, List<LoanStatus> activeStatuses);
}
