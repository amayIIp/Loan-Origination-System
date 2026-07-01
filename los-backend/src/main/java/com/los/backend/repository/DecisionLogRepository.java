package com.los.backend.repository;

import com.los.backend.model.DecisionLog;
import com.los.backend.model.enums.FinalDecision;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


@Repository
public interface DecisionLogRepository extends MongoRepository<DecisionLog, String> {

    
    
    List<DecisionLog> findByLoanApplicationIdOrderByEvaluatedAtDesc(String loanApplicationId);

    
    
    Optional<DecisionLog> findTopByLoanApplicationIdOrderByEvaluatedAtDesc(String loanApplicationId);

    
    
    List<DecisionLog> findByApplicantIdAndFinalDecision(String applicantId, FinalDecision finalDecision);

    
    
    List<DecisionLog> findByEvaluatedAtBetweenAndFinalDecision(
        Instant from,
        Instant to,
        FinalDecision finalDecision
    );

    
    
    @Query("{ 'evaluator_version': ?0 }")
    List<DecisionLog> findByEvaluatorVersion(String evaluatorVersion);

    
    
    @Aggregation(pipeline = {
        
        "{ $group: { _id: '$final_decision', count: { $sum: 1 } } }",
        
        "{ $sort: { count: -1 } }"
    })
    List<Object[]> countDecisionsByFinalDecision();

    
    
    long countByFinalDecisionAndEvaluatedAtBetween(
        FinalDecision finalDecision,
        Instant from,
        Instant to
    );
}
