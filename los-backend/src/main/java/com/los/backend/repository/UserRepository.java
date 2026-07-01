package com.los.backend.repository;

import com.los.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends MongoRepository<User, String> {

    
    Optional<User> findByUsername(String username);

    
    Optional<User> findByEmail(String email);

    
    @Query("{ 'roles': { $in: [?0] }, 'is_active': true }")
    List<User> findActiveUsersByRole(String role);

    
    boolean existsByUsernameOrEmail(String username, String email);

    
    long countByIsActiveTrue();
}
