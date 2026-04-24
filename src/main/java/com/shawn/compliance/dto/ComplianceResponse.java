package com.shawn.compliance.dto;

public record ComplianceResponse(
        String status,
        String decision,
        String reason,
        String explanation,
        String explanationSource
) {
}