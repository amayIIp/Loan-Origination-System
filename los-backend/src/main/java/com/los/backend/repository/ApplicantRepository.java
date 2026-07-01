package com.los.backend.repository;



import com.los.backend.model.Applicant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


@Repository
public interface ApplicantRepository extends MongoRepository<Applicant, String> {

    
    
    Optional<Applicant> findByEmail(String email);

    
    
    Optional<Applicant> findByPanNumber(String panNumber);

    
    
    Page<Applicant> findByIsActiveTrue(Pageable pageable);

    
    
    List<Applicant> findByCreatedAtBetweenAndIsActiveTrue(Instant from, Instant to);

    
    
    @Query("{ 'kyc_documents': { $elemMatch: { 'verification_status': 'REJECTED' } } }")
    List<Applicant> findApplicantsWithRejectedKycDocuments();

    
    
    long countByIsActiveTrue();

    
    
    boolean existsByEmailOrPanNumber(String email, String panNumber);
}
