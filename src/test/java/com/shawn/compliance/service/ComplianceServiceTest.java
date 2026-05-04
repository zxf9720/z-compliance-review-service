package com.shawn.compliance.service;

import com.shawn.compliance.dto.ComplianceRequest;
import com.shawn.compliance.dto.ComplianceResponse;
import com.shawn.compliance.dto.ExplanationResult;
import com.shawn.compliance.dto.RuleCheckResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {

    @Mock
    private ComplianceExplanationService explanationService;

    private ComplianceService complianceService;

    @BeforeEach
    void setUp() {
        complianceService = new ComplianceService(explanationService);
    }

    @Test
    void checkApprovesEligibleCustomerAndIncludesExplanation() {
        ComplianceRequest request = request("Standard policy applies", "MEDIUM", "VERIFIED", 90000);
        when(explanationService.explain(eq(request), any()))
                .thenReturn(new ExplanationResult("Customer satisfies the policy.", "TEST"));

        ComplianceResponse response = complianceService.check(request);

        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.decision()).isEqualTo("APPROVED");
        assertThat(response.reason()).isEqualTo("Customer meets the basic compliance requirements");
        assertThat(response.explanation()).isEqualTo("Customer satisfies the policy.");
        assertThat(response.explanationSource()).isEqualTo("TEST");

        ArgumentCaptor<RuleCheckResult> ruleResult = ArgumentCaptor.forClass(RuleCheckResult.class);
        verify(explanationService).explain(eq(request), ruleResult.capture());
        assertThat(ruleResult.getValue().decision()).isEqualTo("APPROVED");
    }

    @Test
    void checkRejectsUnverifiedKycBeforeOtherRules() {
        ComplianceRequest request = request("Standard policy applies", "HIGH", "PENDING", 10000);
        when(explanationService.explain(eq(request), any()))
                .thenReturn(new ExplanationResult("KYC is not verified.", "TEST"));

        ComplianceResponse response = complianceService.check(request);

        assertThat(response.decision()).isEqualTo("REJECTED");
        assertThat(response.reason()).isEqualTo("Customer KYC is not verified");
    }

    @Test
    void checkRejectsHighRiskCustomer() {
        ComplianceRequest request = request("Standard policy applies", "HIGH", "VERIFIED", 90000);
        when(explanationService.explain(eq(request), any()))
                .thenReturn(new ExplanationResult("High risk customers are restricted.", "TEST"));

        ComplianceResponse response = complianceService.check(request);

        assertThat(response.decision()).isEqualTo("REJECTED");
        assertThat(response.reason()).isEqualTo("High risk customers are restricted for this product");
    }

    @Test
    void checkRejectsIncomeBelowMinimumThreshold() {
        ComplianceRequest request = request("Standard policy applies", "LOW", "VERIFIED", 29999.99);
        when(explanationService.explain(eq(request), any()))
                .thenReturn(new ExplanationResult("Income is below threshold.", "TEST"));

        ComplianceResponse response = complianceService.check(request);

        assertThat(response.decision()).isEqualTo("REJECTED");
        assertThat(response.reason()).isEqualTo("Customer income is below the minimum threshold");
    }

    @Test
    void checkRejectsMissingPolicy() {
        ComplianceRequest request = request("  ", "LOW", "VERIFIED", 30000);
        when(explanationService.explain(eq(request), any()))
                .thenReturn(new ExplanationResult("Policy is missing.", "TEST"));

        ComplianceResponse response = complianceService.check(request);

        assertThat(response.decision()).isEqualTo("REJECTED");
        assertThat(response.reason()).isEqualTo("Policy information is missing");
    }

    @Test
    void checkRejectsInvalidRequestBeforeCallingExplanationService() {
        assertThatThrownBy(() -> complianceService.check(request("Policy", "LOW", " ", 30000)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("kycStatus must not be empty");
        verifyNoInteractions(explanationService);
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void checkValidatesRequiredRequestFields(ComplianceRequest request, String expectedMessage) {
        assertThatThrownBy(() -> complianceService.check(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
        verifyNoInteractions(explanationService);
    }

    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of(null, "Request must not be null"),
                Arguments.of(new ComplianceRequest("Policy", null), "Customer profile must not be null"),
                Arguments.of(new ComplianceRequest("Policy", new ComplianceRequest.CustomerProfile(
                        " ", 42, 30000, "LOW", "RETIREMENT", "VERIFIED"
                )), "customerId must not be empty"),
                Arguments.of(new ComplianceRequest("Policy", new ComplianceRequest.CustomerProfile(
                        "customer-123", 42, 30000, " ", "RETIREMENT", "VERIFIED"
                )), "riskLevel must not be empty")
        );
    }

    private static ComplianceRequest request(String policy, String riskLevel, String kycStatus, double annualIncome) {
        return new ComplianceRequest(
                policy,
                new ComplianceRequest.CustomerProfile(
                        "customer-123",
                        42,
                        annualIncome,
                        riskLevel,
                        "RETIREMENT",
                        kycStatus
                )
        );
    }
}
