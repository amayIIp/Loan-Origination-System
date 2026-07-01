package com.lending.bre.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// MongoDB collection for loan applications.
@Document(collection = "loan_applications")
public class LoanApplication {
    // Unique ID.
    @Id
    private String id;
    // Applicant's credit score.
    private int creditScore;
    // Applicant's age.
    private int age;
    // Monthly income.
    private double monthlyIncome;
    // Monthly debt.
    private double monthlyDebt;
    // Requested amount.
    private double requestedAmount;
    // Employment status.
    private String employmentStatus;
    // Current status of the application (e.g., PENDING, APPROVED, REJECTED).
    private String status;

    public LoanApplication() {}

    // Getters and setters for all properties.
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public int getCreditScore() { return creditScore; }
    public void setCreditScore(int creditScore) { this.creditScore = creditScore; }
    
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    
    public double getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(double monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    
    public double getMonthlyDebt() { return monthlyDebt; }
    public void setMonthlyDebt(double monthlyDebt) { this.monthlyDebt = monthlyDebt; }
    
    public double getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(double requestedAmount) { this.requestedAmount = requestedAmount; }
    
    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}