package com.shawn.compliance.dto;

public record RuleCheckResult(
        String decision,
        String reason
) {
}