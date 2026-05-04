package com.shawn.compliance.service;

import com.shawn.compliance.dto.ComplianceRequest;
import com.shawn.compliance.dto.ComplianceResponse;
import com.shawn.compliance.dto.ExplanationResult;
import com.shawn.compliance.dto.RuleCheckResult;
import org.springframework.stereotype.Service;

@Service
public class ComplianceService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String DECISION_APPROVED = "APPROVED";
    private static final String DECISION_REJECTED = "REJECTED";

    private final ComplianceExplanationService explanationService;

    public ComplianceService(ComplianceExplanationService explanationService) {
        this.explanationService = explanationService;
    }

    public ComplianceResponse check(ComplianceRequest request) {
        validateRequest(request);

        RuleCheckResult ruleResult = runRuleBasedCheck(request);

        ExplanationResult explanationResult = explanationService.explain(request, ruleResult);

        return new ComplianceResponse(
                STATUS_SUCCESS,
                ruleResult.decision(),
                ruleResult.reason(),
                explanationResult.explanation(),
                explanationResult.explanationSource()
        );
    }

    private RuleCheckResult runRuleBasedCheck(ComplianceRequest request) {
        var customer = request.customer();

        if (!"VERIFIED".equalsIgnoreCase(customer.kycStatus())) {
            return new RuleCheckResult(
                    DECISION_REJECTED,
                    "Customer KYC is not verified"
            );
        }

        if ("HIGH".equalsIgnoreCase(customer.riskLevel())) {
            return new RuleCheckResult(
                    DECISION_REJECTED,
                    "High risk customers are restricted for this product"
            );
        }

        if (customer.annualIncome() < 30000) {
            return new RuleCheckResult(
                    DECISION_REJECTED,
                    "Customer income is below the minimum threshold"
            );
        }

        if (request.policy() == null || request.policy().isBlank()) {
            return new RuleCheckResult(
                    DECISION_REJECTED,
                    "Policy information is missing"
            );
        }

        return new RuleCheckResult(
                DECISION_APPROVED,
                "Customer meets the basic compliance requirements"
        );
    }

    private void validateRequest(ComplianceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        if (request.customer() == null) {
            throw new IllegalArgumentException("Customer profile must not be null");
        }

        if (request.customer().customerId() == null || request.customer().customerId().isBlank()) {
            throw new IllegalArgumentException("customerId must not be empty");
        }

        if (request.customer().riskLevel() == null || request.customer().riskLevel().isBlank()) {
            throw new IllegalArgumentException("riskLevel must not be empty");
        }

        if (request.customer().kycStatus() == null || request.customer().kycStatus().isBlank()) {
            throw new IllegalArgumentException("kycStatus must not be empty");
        }
    }
}