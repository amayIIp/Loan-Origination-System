package com.lending.bre.repository;

import com.lending.bre.model.DecisionLog;
import org.springframework.data.mongodb.repository.MongoRepository;

// Provide standard save/find/delete operations for the DecisionLog documents.
public interface DecisionLogRepository extends MongoRepository<DecisionLog, String> {
}