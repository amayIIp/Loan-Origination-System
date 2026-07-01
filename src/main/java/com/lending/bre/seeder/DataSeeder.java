package com.lending.bre.seeder;

import com.lending.bre.model.LoanApplication;
import com.lending.bre.repository.LoanApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/*
 * DATABASE SEEDER (PERFORMANCE TESTING PREP)
 * This script only runs when the Spring Boot app is started with the "seed" profile 
 * (e.g., java -jar app.jar --spring.profiles.active=seed).
 * It injects 10,000 synthetic applications into MongoDB to simulate a heavy production 
 * workload, allowing us to perform realistic benchmarking and query optimization.
 */

// Register this as a Spring component.
@Component
// Ensure this ONLY runs if the "seed" profile is active so we don't accidentally wipe production!
@Profile("seed")
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);
    private final LoanApplicationRepository repository;

    // Ask Spring to give us the database repository.
    public DataSeeder(LoanApplicationRepository repository) {
        this.repository = repository;
    }

    // This method automatically executes exactly once when the application starts up.
    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting database seed process...");

        // Wipe existing test data to start fresh.
        repository.deleteAll();

        int targetVolume = 10000;
        int batchSize = 1000;
        List<LoanApplication> batch = new ArrayList<>();
        
        // Define possible statuses to randomize.
        String[] statuses = {"SUBMITTED", "UNDER_REVIEW", "APPROVED", "REJECTED"};

        // Loop to generate 10,000 fake records.
        for (int i = 1; i <= targetVolume; i++) {
            LoanApplication app = new LoanApplication();
            
            // Generate a random credit score between 400 and 850.
            app.setCreditScore(ThreadLocalRandom.current().nextInt(400, 851));
            // Random age between 18 and 80.
            app.setAge(ThreadLocalRandom.current().nextInt(18, 81));
            // Random monthly income between $2k and $15k.
            app.setMonthlyIncome(ThreadLocalRandom.current().nextDouble(2000, 15000));
            // Random monthly debt.
            app.setMonthlyDebt(ThreadLocalRandom.current().nextDouble(100, 3000));
            // Random requested loan amount.
            app.setRequestedAmount(ThreadLocalRandom.current().nextDouble(5000, 100000));
            // Pick a random status from our array.
            app.setStatus(statuses[ThreadLocalRandom.current().nextInt(statuses.length)]);

            batch.add(app);

            // Save in batches of 1,000 to prevent running out of RAM (OutOfMemoryError) during seeding.
            if (i % batchSize == 0) {
                repository.saveAll(batch);
                batch.clear();
                logger.info("Inserted {} records so far...", i);
            }
        }

        logger.info("Database seeding complete. Inserted {} records.", targetVolume);
    }
}