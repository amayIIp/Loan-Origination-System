package com.lending.bre.repository;

import com.lending.bre.model.DecisionLog;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface DecisionLogRepository extends MongoRepository<DecisionLog, String> {
}