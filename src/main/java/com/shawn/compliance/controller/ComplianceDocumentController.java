package com.shawn.compliance.controller;

import com.shawn.compliance.dto.ComplianceDocumentIngestResponse;
import com.shawn.compliance.service.ComplianceDocumentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/compliance/documents")
public class ComplianceDocumentController {

    private final ComplianceDocumentService complianceDocumentService;

    public ComplianceDocumentController(ComplianceDocumentService complianceDocumentService) {
        this.complianceDocumentService = complianceDocumentService;
    }

    @PostMapping("/bootstrap")
    public ComplianceDocumentIngestResponse bootstrapDocuments() {
        return complianceDocumentService.bootstrapDocuments();
    }
}