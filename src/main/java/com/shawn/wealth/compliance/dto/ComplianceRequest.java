package com.shawn.wealth.compliance.dto;

public record ComplianceRequest(
        String policy,
        CustomerProfile customer
) {
    public record CustomerProfile(
            String customerId,
            int age,
            double annualIncome,
            String riskLevel,
            String investmentObjective,
            String kycStatus
    ) {
    }
}
