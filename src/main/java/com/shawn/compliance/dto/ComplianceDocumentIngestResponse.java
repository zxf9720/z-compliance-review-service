package com.shawn.compliance.dto;

public record ComplianceDocumentIngestResponse(
        String status,
        int indexedFiles,
        int totalChunks
) {
}