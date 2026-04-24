package com.shawn.compliance.service;

import com.shawn.compliance.dto.ComplianceRequest;
import com.shawn.compliance.dto.ExplanationResult;
import com.shawn.compliance.dto.RuleCheckResult;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ComplianceExplanationService {

    private static final String SOURCE_OPENAI = "RULE_BASED_WITH_OPENAI_EXPLANATION";
    private static final String SOURCE_OLLAMA = "RULE_BASED_WITH_OLLAMA_EXPLANATION";
    private static final String SOURCE_UNAVAILABLE = "RULE_BASED_EXPLANATION_UNAVAILABLE";

    private final String openAiApiKey;
    private final String openAiModelName;
    private final ChatModel ollamaChatModel;

    public ComplianceExplanationService(
            @Value("${openai.api-key:}") String openAiApiKey,
            @Value("${openai.model:gpt-4.1-mini}") String openAiModelName,
            @Qualifier("ollamaChatModel") ChatModel ollamaChatModel
    ) {
        this.openAiApiKey = openAiApiKey;
        this.openAiModelName = openAiModelName;
        this.ollamaChatModel = ollamaChatModel;
    }

    public ExplanationResult explain(ComplianceRequest request, RuleCheckResult ruleResult) {
        String prompt = buildPrompt(request, ruleResult);

        ExplanationResult openAiResult = tryGenerateWithOpenAi(prompt);
        if (openAiResult != null) {
            return openAiResult;
        }

        ExplanationResult ollamaResult = tryGenerateWithOllama(prompt);
        if (ollamaResult != null) {
            return ollamaResult;
        }

        return new ExplanationResult(
                "LLM explanation is unavailable. Rule-based reason: " + ruleResult.reason(),
                SOURCE_UNAVAILABLE
        );
    }

    private ExplanationResult tryGenerateWithOpenAi(String prompt) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return null;
        }

        try {
            ChatModel openAiChatModel = OpenAiChatModel.builder()
                    .apiKey(openAiApiKey)
                    .modelName(openAiModelName)
                    .temperature(0.2)
                    .timeout(Duration.ofSeconds(30))
                    .build();

            String explanation = openAiChatModel.chat(prompt);

            if (explanation == null || explanation.isBlank()) {
                return null;
            }

            return new ExplanationResult(explanation, SOURCE_OPENAI);
        } catch (Exception e) {
            return null;
        }
    }

    private ExplanationResult tryGenerateWithOllama(String prompt) {
        try {
            String explanation = ollamaChatModel.chat(prompt);

            if (explanation == null || explanation.isBlank()) {
                return null;
            }

            return new ExplanationResult(explanation, SOURCE_OLLAMA);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildPrompt(ComplianceRequest request, RuleCheckResult ruleResult) {
        return """
                You are a compliance assistant for a banking and wealth management system.

                The final compliance decision has already been made by deterministic business rules.
                Do not change the decision.
                Your task is only to explain the decision clearly and professionally.

                Policy:
                %s

                Customer:
                - Customer ID: %s
                - Age: %d
                - Annual Income: %.2f
                - Risk Level: %s
                - Investment Objective: %s
                - KYC Status: %s

                Rule-based decision:
                - Decision: %s
                - Reason: %s

                Generate a concise explanation in 2 to 4 sentences.
                Avoid legal advice.
                Do not add facts that are not provided.
                """.formatted(
                request.policy(),
                request.customer().customerId(),
                request.customer().age(),
                request.customer().annualIncome(),
                request.customer().riskLevel(),
                request.customer().investmentObjective(),
                request.customer().kycStatus(),
                ruleResult.decision(),
                ruleResult.reason()
        );
    }
}