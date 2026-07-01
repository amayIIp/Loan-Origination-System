package com.lending.bre.repository;

import com.lending.bre.model.LoanApplication;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface LoanApplicationRepository extends MongoRepository<LoanApplication, String> {
}