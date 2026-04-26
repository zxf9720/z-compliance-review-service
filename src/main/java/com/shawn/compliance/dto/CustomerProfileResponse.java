package com.shawn.compliance.dto;

public record CustomerProfileResponse(
        String customerId,
        String name,
        int age,
        double annualIncome,
        String riskLevel,
        String investmentObjective,
        String kycStatus
) {
}