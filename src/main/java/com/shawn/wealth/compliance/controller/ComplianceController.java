package com.shawn.wealth.compliance.controller;

import com.shawn.wealth.compliance.dto.ComplianceRequest;
import com.shawn.wealth.compliance.dto.ComplianceResponse;
import com.shawn.wealth.compliance.service.ComplianceService;
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
