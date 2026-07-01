package com.lending.bre.model;

/*
 * BUSINESS RULE ENGINE CONTEXT:
 * This context object gathers all the necessary information about an applicant and their requested loan
 * into one place. Instead of passing 20 different variables (age, income, credit score) to every rule,
 * we pass this single "Context" object. This makes adding new rules or new data fields much easier later.
 */
public class ApplicantEvaluationContext {
    // The unique identifier for this specific loan application.
    private String applicationId;
    // The applicant's credit score, pulled from a credit bureau (e.g., 300 to 850).
    private int creditScore;
    // The applicant's age in years.
    private int age;
    // The applicant's total monthly income.
    private double monthlyIncome;
    // The applicant's total monthly debt payments (used to calculate Debt-to-Income ratio).
    private double monthlyDebt;
    // The total amount of money the applicant is asking to borrow.
    private double requestedLoanAmount;
    // The applicant's current employment status (e.g., "EMPLOYED", "UNEMPLOYED", "SELF_EMPLOYED").
    private String employmentStatus;

    // A constructor to easily set up this context with all required data at once.
    public ApplicantEvaluationContext(String applicationId, int creditScore, int age, double monthlyIncome, double monthlyDebt, double requestedLoanAmount, String employmentStatus) {
        // Save the loan application ID.
        this.applicationId = applicationId;
        // Save the credit score.
        this.creditScore = creditScore;
        // Save the applicant's age.
        this.age = age;
        // Save the monthly income.
        this.monthlyIncome = monthlyIncome;
        // Save the monthly debt.
        this.monthlyDebt = monthlyDebt;
        // Save the loan amount requested.
        this.requestedLoanAmount = requestedLoanAmount;
        // Save the employment status.
        this.employmentStatus = employmentStatus;
    }

    // Standard getters to let rules read the context data securely.
    
    // Get the application ID.
    public String getApplicationId() { return applicationId; }
    // Get the credit score.
    public int getCreditScore() { return creditScore; }
    // Get the applicant's age.
    public int getAge() { return age; }
    // Get the monthly income.
    public double getMonthlyIncome() { return monthlyIncome; }
    // Get the monthly debt.
    public double getMonthlyDebt() { return monthlyDebt; }
    // Get the requested loan amount.
    public double getRequestedLoanAmount() { return requestedLoanAmount; }
    // Get the employment status.
    public String getEmploymentStatus() { return employmentStatus; }
}