package com.lending.bre.repository;

import com.lending.bre.model.LoanApplication;
import org.springframework.data.mongodb.repository.MongoRepository;

// Provide standard database operations for LoanApplication documents.
public interface LoanApplicationRepository extends MongoRepository<LoanApplication, String> {
}