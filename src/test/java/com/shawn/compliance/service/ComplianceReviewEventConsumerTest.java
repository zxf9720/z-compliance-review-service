package com.shawn.compliance.service;

import com.shawn.compliance.dto.ComplianceRequest;
import com.shawn.compliance.dto.ComplianceResponse;
import com.shawn.compliance.dto.CustomerProfileResponse;
import com.shawn.compliance.history.ComplianceHistoryEntity;
import com.shawn.compliance.history.ComplianceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceReviewEventConsumerTest {

    private static final String COMPLETED_TOPIC = "wealth.compliance.review.completed";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CustomerDataClient customerDataClient;

    @Mock
    private ComplianceService complianceService;

    @Mock
    private ComplianceShortTermMemoryService memoryService;

    @Mock
    private ComplianceHistoryRepository historyRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ComplianceReviewEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ComplianceReviewEventConsumer(
                objectMapper,
                customerDataClient,
                complianceService,
                memoryService,
                historyRepository,
                kafkaTemplate,
                COMPLETED_TOPIC
        );
    }

    @Test
    void consumeBuildsComplianceRequestPersistsHistoryAndPublishesCompletedEvent() throws Exception {
        String payload = """
                {
                  "requestId": "request-1",
                  "sessionId": "session-1",
                  "customerId": "customer-1",
                  "question": "Can this customer buy the product?",
                  "policyAnswer": "Only verified non-high-risk customers qualify."
                }
                """;
        CustomerProfileResponse customer = new CustomerProfileResponse(
                "customer-1",
                "Alex Chen",
                36,
                85000,
                "LOW",
                "GROWTH",
                "VERIFIED"
        );
        ComplianceResponse complianceResponse = new ComplianceResponse(
                "SUCCESS",
                "APPROVED",
                "Customer meets the basic compliance requirements",
                "The customer satisfies the deterministic checks.",
                "TEST"
        );
        when(customerDataClient.getCustomer("customer-1")).thenReturn(customer);
        when(complianceService.check(any(ComplianceRequest.class))).thenReturn(complianceResponse);

        consumer.consume(payload);

        ArgumentCaptor<ComplianceRequest> requestCaptor = ArgumentCaptor.forClass(ComplianceRequest.class);
        verify(complianceService).check(requestCaptor.capture());
        ComplianceRequest request = requestCaptor.getValue();
        assertThat(request.policy()).isEqualTo("Only verified non-high-risk customers qualify.");
        assertThat(request.customer().customerId()).isEqualTo("customer-1");
        assertThat(request.customer().annualIncome()).isEqualTo(85000);
        assertThat(request.customer().riskLevel()).isEqualTo("LOW");
        assertThat(request.customer().kycStatus()).isEqualTo("VERIFIED");

        verify(memoryService).addMessage("session-1", "USER", "Can this customer buy the product?");
        ArgumentCaptor<String> finalAnswerCaptor = ArgumentCaptor.forClass(String.class);
        verify(memoryService).addMessage(eq("session-1"), eq("ASSISTANT"), finalAnswerCaptor.capture());
        assertThat(finalAnswerCaptor.getValue()).contains("Decision: APPROVED");
        assertThat(finalAnswerCaptor.getValue()).contains("Explanation: The customer satisfies the deterministic checks.");

        ArgumentCaptor<ComplianceHistoryEntity> historyCaptor = ArgumentCaptor.forClass(ComplianceHistoryEntity.class);
        verify(historyRepository).save(historyCaptor.capture());
        ComplianceHistoryEntity history = historyCaptor.getValue();
        assertThat(history.getRequestId()).isEqualTo("request-1");
        assertThat(history.getSessionId()).isEqualTo("session-1");
        assertThat(history.getCustomerId()).isEqualTo("customer-1");
        assertThat(history.getDecision()).isEqualTo("APPROVED");
        assertThat(history.getStatus()).isEqualTo("SUCCESS");
        assertThat(history.getCreatedAt()).isNotNull();

        ArgumentCaptor<String> completedPayloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(COMPLETED_TOPIC), eq("request-1"), completedPayloadCaptor.capture());
        JsonNode completedPayload = objectMapper.readTree(completedPayloadCaptor.getValue());
        assertThat(completedPayload.get("requestId").asText()).isEqualTo("request-1");
        assertThat(completedPayload.get("sessionId").asText()).isEqualTo("session-1");
        assertThat(completedPayload.get("customerId").asText()).isEqualTo("customer-1");
        assertThat(completedPayload.get("status").asText()).isEqualTo("SUCCESS");
        assertThat(completedPayload.get("decision").asText()).isEqualTo("APPROVED");
        assertThat(completedPayload.get("finalAnswer").asText()).contains("Compliance review completed.");
    }

    @Test
    void consumeWrapsProcessingFailures() {
        when(customerDataClient.getCustomer("customer-1")).thenThrow(new IllegalStateException("customer service down"));

        assertThatThrownBy(() -> consumer.consume("""
                {
                  "requestId": "request-1",
                  "sessionId": "session-1",
                  "customerId": "customer-1",
                  "question": "Can this customer buy the product?",
                  "policyAnswer": "Policy text"
                }
                """))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to process compliance review event")
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
