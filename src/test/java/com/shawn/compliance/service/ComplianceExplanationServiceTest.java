package com.shawn.compliance.service;

import com.shawn.compliance.dto.ComplianceRequest;
import com.shawn.compliance.dto.ExplanationResult;
import com.shawn.compliance.dto.RuleCheckResult;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComplianceExplanationServiceTest {

    @Test
    void explainUsesOllamaWithRetrievedVectorContextWhenOpenAiKeyIsMissing() {
        ChatModel ollamaChatModel = mock(ChatModel.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        @SuppressWarnings("unchecked")
        EmbeddingStore<TextSegment> embeddingStore = mock(EmbeddingStore.class);
        Embedding embedding = Embedding.from(new float[]{0.1f, 0.2f});
        TextSegment context = TextSegment.from("KYC policy context");
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(embedding));
        when(embeddingStore.search(any(EmbeddingSearchRequest.class))).thenReturn(new EmbeddingSearchResult<>(List.of(
                new EmbeddingMatch<>(0.9, "embedding-1", embedding, context)
        )));
        when(ollamaChatModel.chat(anyString())).thenReturn("Generated explanation");
        ComplianceExplanationService service = new ComplianceExplanationService(
                "",
                "gpt-4.1-mini",
                ollamaChatModel,
                embeddingModel,
                embeddingStore
        );

        ExplanationResult result = service.explain(request(), new RuleCheckResult("APPROVED", "Allowed"));

        assertThat(result.explanation()).isEqualTo("Generated explanation");
        assertThat(result.explanationSource()).isEqualTo("RULE_BASED_WITH_OLLAMA_AND_VECTOR_CONTEXT");
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(ollamaChatModel).chat(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("KYC policy context");
        assertThat(promptCaptor.getValue()).contains("Decision: APPROVED");
    }

    @Test
    void explainFallsBackWhenVectorSearchAndOllamaAreUnavailable() {
        ChatModel ollamaChatModel = mock(ChatModel.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        @SuppressWarnings("unchecked")
        EmbeddingStore<TextSegment> embeddingStore = mock(EmbeddingStore.class);
        when(embeddingModel.embed(anyString())).thenThrow(new IllegalStateException("vector unavailable"));
        when(ollamaChatModel.chat(anyString())).thenThrow(new IllegalStateException("ollama unavailable"));
        ComplianceExplanationService service = new ComplianceExplanationService(
                "",
                "gpt-4.1-mini",
                ollamaChatModel,
                embeddingModel,
                embeddingStore
        );

        ExplanationResult result = service.explain(request(), new RuleCheckResult("REJECTED", "Denied"));

        assertThat(result.explanation()).isEqualTo("LLM explanation is unavailable. Rule-based reason: Denied");
        assertThat(result.explanationSource()).isEqualTo("RULE_BASED_EXPLANATION_UNAVAILABLE");
    }

    @Test
    void explainIncludesNoContextMessageWhenVectorSearchHasNoMatches() {
        ChatModel ollamaChatModel = mock(ChatModel.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        @SuppressWarnings("unchecked")
        EmbeddingStore<TextSegment> embeddingStore = mock(EmbeddingStore.class);
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[]{0.1f})));
        when(embeddingStore.search(any(EmbeddingSearchRequest.class))).thenReturn(new EmbeddingSearchResult<>(List.of()));
        when(ollamaChatModel.chat(anyString())).thenReturn("No context explanation");
        ComplianceExplanationService service = new ComplianceExplanationService(
                "",
                "gpt-4.1-mini",
                ollamaChatModel,
                embeddingModel,
                embeddingStore
        );

        ExplanationResult result = service.explain(request(), new RuleCheckResult("APPROVED", "Allowed"));

        assertThat(result.explanation()).isEqualTo("No context explanation");
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(ollamaChatModel).chat(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("No additional compliance context was found in the vector database.");
    }

    private static ComplianceRequest request() {
        return new ComplianceRequest(
                "Policy text",
                new ComplianceRequest.CustomerProfile(
                        "customer-1",
                        40,
                        100000,
                        "LOW",
                        "GROWTH",
                        "VERIFIED"
                )
        );
    }
}
