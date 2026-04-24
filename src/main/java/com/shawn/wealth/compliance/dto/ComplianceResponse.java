package com.shawn.wealth.compliance.dto;

public record ComplianceResponse(
        String status,
        String decision,
        String reason
) {
}
