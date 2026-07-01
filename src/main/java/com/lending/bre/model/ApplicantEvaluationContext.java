package com.lending.bre.model;


public class ApplicantEvaluationContext {
    
    private String applicationId;
    
    private int creditScore;
    
    private int age;
    
    private double monthlyIncome;
    
    private double monthlyDebt;
    
    private double requestedLoanAmount;
    
    private String employmentStatus;

    
    public ApplicantEvaluationContext(String applicationId, int creditScore, int age, double monthlyIncome, double monthlyDebt, double requestedLoanAmount, String employmentStatus) {
        
        this.applicationId = applicationId;
        
        this.creditScore = creditScore;
        
        this.age = age;
        
        this.monthlyIncome = monthlyIncome;
        
        this.monthlyDebt = monthlyDebt;
        
        this.requestedLoanAmount = requestedLoanAmount;
        
        this.employmentStatus = employmentStatus;
    }

    
    
    
    public String getApplicationId() { return applicationId; }
    
    public int getCreditScore() { return creditScore; }
    
    public int getAge() { return age; }
    
    public double getMonthlyIncome() { return monthlyIncome; }
    
    public double getMonthlyDebt() { return monthlyDebt; }
    
    public double getRequestedLoanAmount() { return requestedLoanAmount; }
    
    public String getEmploymentStatus() { return employmentStatus; }
}