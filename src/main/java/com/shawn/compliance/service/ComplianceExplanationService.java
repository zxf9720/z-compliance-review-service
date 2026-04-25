package com.shawn.compliance.service;

import com.shawn.compliance.dto.ComplianceRequest;
import com.shawn.compliance.dto.ExplanationResult;
import com.shawn.compliance.dto.RuleCheckResult;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComplianceExplanationService {

    private static final String SOURCE_OPENAI = "RULE_BASED_WITH_OPENAI_AND_VECTOR_CONTEXT";
    private static final String SOURCE_OLLAMA = "RULE_BASED_WITH_OLLAMA_AND_VECTOR_CONTEXT";
    private static final String SOURCE_UNAVAILABLE = "RULE_BASED_EXPLANATION_UNAVAILABLE";

    private static final int TOP_K = 3;
    private static final double MIN_SCORE = 0.65d;

    private final String openAiApiKey;
    private final String openAiModelName;
    private final ChatModel ollamaChatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public ComplianceExplanationService(
            @Value("${openai.api-key:}") String openAiApiKey,
            @Value("${openai.model:gpt-4.1-mini}") String openAiModelName,
            ChatModel ollamaChatModel,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore
    ) {
        this.openAiApiKey = openAiApiKey;
        this.openAiModelName = openAiModelName;
        this.ollamaChatModel = ollamaChatModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    public ExplanationResult explain(ComplianceRequest request, RuleCheckResult ruleResult) {
        String vectorContext = retrieveVectorContext(request, ruleResult);
        String prompt = buildPrompt(request, ruleResult, vectorContext);

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

    private String retrieveVectorContext(ComplianceRequest request, RuleCheckResult ruleResult) {
        try {
            String query = """
                    Policy:
                    %s

                    Customer risk level: %s
                    Customer investment objective: %s
                    KYC status: %s
                    Rule decision: %s
                    Rule reason: %s
                    """.formatted(
                    request.policy(),
                    request.customer().riskLevel(),
                    request.customer().investmentObjective(),
                    request.customer().kycStatus(),
                    ruleResult.decision(),
                    ruleResult.reason()
            );

            Embedding queryEmbedding = embeddingModel.embed(query).content();

            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(TOP_K)
                    .minScore(MIN_SCORE)
                    .build();

            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();

            if (matches == null || matches.isEmpty()) {
                return "No additional compliance context was found in the vector database.";
            }

            return matches.stream()
                    .map(match -> match.embedded().text())
                    .collect(Collectors.joining("\n\n"));

        } catch (Exception e) {
            return "Vector database context is unavailable.";
        }
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

    private String buildPrompt(ComplianceRequest request,
                               RuleCheckResult ruleResult,
                               String vectorContext) {
        return """
                You are a compliance assistant for a banking and wealth management system.

                The final compliance decision has already been made by deterministic business rules.
                Do not change the decision.
                Your task is only to explain the decision clearly and professionally.

                Policy from request:
                %s

                Additional compliance context retrieved from pgvector:
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
                Do not change the rule-based decision.
                """.formatted(
                request.policy(),
                vectorContext,
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