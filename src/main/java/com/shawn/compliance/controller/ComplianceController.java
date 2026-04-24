package com.shawn.compliance.controller;

import com.shawn.compliance.dto.ComplianceRequest;
import com.shawn.compliance.dto.ComplianceResponse;
import com.shawn.compliance.service.ComplianceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/compliance")
public class ComplianceController {

    private final ComplianceService complianceService;

    public ComplianceController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @PostMapping("/check")
    public ComplianceResponse check(@RequestBody ComplianceRequest request) {
        return complianceService.check(request);
    }
}
