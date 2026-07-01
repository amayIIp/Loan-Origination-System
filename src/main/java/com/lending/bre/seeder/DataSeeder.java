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




@Component

@Profile("seed")
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);
    private final LoanApplicationRepository repository;

    
    public DataSeeder(LoanApplicationRepository repository) {
        this.repository = repository;
    }

    
    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting database seed process...");

        
        repository.deleteAll();

        int targetVolume = 10000;
        int batchSize = 1000;
        List<LoanApplication> batch = new ArrayList<>();
        
        
        String[] statuses = {"SUBMITTED", "UNDER_REVIEW", "APPROVED", "REJECTED"};

        
        for (int i = 1; i <= targetVolume; i++) {
            LoanApplication app = new LoanApplication();
            
            
            app.setCreditScore(ThreadLocalRandom.current().nextInt(400, 851));
            
            app.setAge(ThreadLocalRandom.current().nextInt(18, 81));
            
            app.setMonthlyIncome(ThreadLocalRandom.current().nextDouble(2000, 15000));
            
            app.setMonthlyDebt(ThreadLocalRandom.current().nextDouble(100, 3000));
            
            app.setRequestedAmount(ThreadLocalRandom.current().nextDouble(5000, 100000));
            
            app.setStatus(statuses[ThreadLocalRandom.current().nextInt(statuses.length)]);

            batch.add(app);

            
            if (i % batchSize == 0) {
                repository.saveAll(batch);
                batch.clear();
                logger.info("Inserted {} records so far...", i);
            }
        }

        logger.info("Database seeding complete. Inserted {} records.", targetVolume);
    }
}