package com.shawn.compliance.service;

import com.shawn.compliance.dto.ComplianceRequest;
import com.shawn.compliance.dto.ComplianceResponse;
import org.springframework.stereotype.Service;

@Service
public class ComplianceService {

    public ComplianceResponse check(ComplianceRequest request) {

        var customer = request.customer();

        // rule 1: KYC 必须验证
        if (!"VERIFIED".equalsIgnoreCase(customer.kycStatus())) {
            return new ComplianceResponse(
                    "SUCCESS",
                    "REJECTED",
                    "Customer KYC is not verified"
            );
        }

        // rule 2: high risk 限制
        if ("HIGH".equalsIgnoreCase(customer.riskLevel())) {
            return new ComplianceResponse(
                    "SUCCESS",
                    "REJECTED",
                    "High risk customers are restricted for this product"
            );
        }

        // rule 3: 收入限制（示例）
        if (customer.annualIncome() < 30000) {
            return new ComplianceResponse(
                    "SUCCESS",
                    "REJECTED",
                    "Customer income below minimum threshold"
            );
        }

        return new ComplianceResponse(
                "SUCCESS",
                "APPROVED",
                "Customer meets basic compliance requirements"
        );
    }
}
