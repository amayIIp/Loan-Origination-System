package com.lending.bre.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "loan_applications")
public class LoanApplication {
    
    @Id
    private String id;
    
    private int creditScore;
    
    private int age;
    
    private double monthlyIncome;
    
    private double monthlyDebt;
    
    private double requestedAmount;
    
    private String employmentStatus;
    
    private String status;

    public LoanApplication() {}

    
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