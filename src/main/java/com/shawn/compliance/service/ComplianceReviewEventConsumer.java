package com.shawn.compliance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shawn.compliance.dto.ComplianceRequest;
import com.shawn.compliance.dto.ComplianceResponse;
import com.shawn.compliance.dto.CustomerProfileResponse;
import com.shawn.compliance.history.ComplianceHistoryEntity;
import com.shawn.compliance.history.ComplianceHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ComplianceReviewEventConsumer {

    private final ObjectMapper objectMapper;
    private final CustomerDataClient customerDataClient;
    private final ComplianceService complianceService;
    private final ComplianceShortTermMemoryService memoryService;
    private final ComplianceHistoryRepository historyRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String completedTopic;

    public ComplianceReviewEventConsumer(ObjectMapper objectMapper,
                                         CustomerDataClient customerDataClient,
                                         ComplianceService complianceService,
                                         ComplianceShortTermMemoryService memoryService,
                                         ComplianceHistoryRepository historyRepository,
                                         KafkaTemplate<String, String> kafkaTemplate,
                                         @Value("${app.kafka.topics.compliance-completed}") String completedTopic) {
        this.objectMapper = objectMapper;
        this.customerDataClient = customerDataClient;
        this.complianceService = complianceService;
        this.memoryService = memoryService;
        this.historyRepository = historyRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.completedTopic = completedTopic;
    }

    @KafkaListener(topics = "${app.kafka.topics.compliance-requested}")
    public void consume(String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);

            String requestId = json.get("requestId").asText();
            String sessionId = json.get("sessionId").asText();
            String customerId = json.get("customerId").asText();
            String question = json.get("question").asText();
            String policyAnswer = json.get("policyAnswer").asText();

            CustomerProfileResponse customer = customerDataClient.getCustomer(customerId);

            ComplianceRequest request = new ComplianceRequest(
                    policyAnswer,
                    new ComplianceRequest.CustomerProfile(
                            customer.customerId(),
                            customer.age(),
                            customer.annualIncome(),
                            customer.riskLevel(),
                            customer.investmentObjective(),
                            customer.kycStatus()
                    )
            );

            ComplianceResponse response = complianceService.check(request);

            String finalAnswer = """
                    Compliance review completed.

                    Customer ID: %s
                    Decision: %s
                    Reason: %s
                    Explanation: %s
                    """.formatted(
                    customerId,
                    response.decision(),
                    response.reason(),
                    response.explanation()
            );

            memoryService.addMessage(sessionId, "USER", question);
            memoryService.addMessage(sessionId, "ASSISTANT", finalAnswer);

            historyRepository.save(new ComplianceHistoryEntity(
                    requestId,
                    sessionId,
                    customerId,
                    response.decision(),
                    response.reason(),
                    response.explanation(),
                    response.status()
            ));

            String completedPayload = objectMapper.writeValueAsString(Map.of(
                    "requestId", requestId,
                    "sessionId", sessionId,
                    "customerId", customerId,
                    "status", response.status(),
                    "decision", response.decision(),
                    "reason", response.reason(),
                    "explanation", response.explanation(),
                    "finalAnswer", finalAnswer
            ));

            kafkaTemplate.send(completedTopic, requestId, completedPayload);

        } catch (Exception e) {
            throw new RuntimeException("Failed to process compliance review event", e);
        }
    }
}