package com.lending.bre.repository;

import com.lending.bre.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

// Ask Spring Data MongoDB to provide standard database operations for Users.
public interface UserRepository extends MongoRepository<User, String> {
    // Custom query to find a user by their email address, returning an Optional in case they don't exist.
    Optional<User> findByEmail(String email);
}