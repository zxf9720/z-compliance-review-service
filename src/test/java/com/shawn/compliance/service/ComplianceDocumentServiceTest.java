package com.shawn.compliance.service;

import com.shawn.compliance.dto.ComplianceDocumentIngestResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComplianceDocumentServiceTest {

    @TempDir
    private Path tempDir;

    @Test
    void bootstrapDocumentsIndexesSupportedTextFilesIntoOverlappingChunks() throws Exception {
        String documentText = "a".repeat(650);
        Files.writeString(tempDir.resolve("policy.txt"), documentText);
        Files.writeString(tempDir.resolve("ignored.csv"), "unsupported");
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        @SuppressWarnings("unchecked")
        EmbeddingStore<TextSegment> embeddingStore = mock(EmbeddingStore.class);
        when(embeddingModel.embed(any(TextSegment.class)))
                .thenReturn(Response.from(Embedding.from(new float[]{0.1f, 0.2f})));
        ComplianceDocumentService service = new ComplianceDocumentService(
                embeddingModel,
                embeddingStore,
                tempDir.toString()
        );

        ComplianceDocumentIngestResponse response = service.bootstrapDocuments();

        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.indexedFiles()).isEqualTo(1);
        assertThat(response.totalChunks()).isEqualTo(2);
        ArgumentCaptor<TextSegment> segmentCaptor = ArgumentCaptor.forClass(TextSegment.class);
        verify(embeddingModel, times(2)).embed(segmentCaptor.capture());
        verify(embeddingStore, times(2)).add(any(Embedding.class), any(TextSegment.class));
        assertThat(segmentCaptor.getAllValues().getFirst().text()).contains("Source file: policy.txt");
    }

    @Test
    void bootstrapDocumentsReturnsZeroWhenNoSupportedFilesExist() throws Exception {
        Files.writeString(tempDir.resolve("ignored.csv"), "unsupported");
        ComplianceDocumentService service = new ComplianceDocumentService(
                mock(EmbeddingModel.class),
                mockEmbeddingStore(),
                tempDir.toString()
        );

        ComplianceDocumentIngestResponse response = service.bootstrapDocuments();

        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.indexedFiles()).isZero();
        assertThat(response.totalChunks()).isZero();
    }

    @Test
    void bootstrapDocumentsRejectsMissingDocsFolder() {
        ComplianceDocumentService service = new ComplianceDocumentService(
                mock(EmbeddingModel.class),
                mockEmbeddingStore(),
                tempDir.resolve("missing").toString()
        );

        assertThatThrownBy(service::bootstrapDocuments)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Docs folder not found:");
    }

    @Test
    void bootstrapDocumentsWrapsIndexingFailuresWithFileName() throws Exception {
        Files.writeString(tempDir.resolve("policy.txt"), "policy text");
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        when(embeddingModel.embed(any(TextSegment.class))).thenThrow(new IllegalStateException("embedding failed"));
        ComplianceDocumentService service = new ComplianceDocumentService(
                embeddingModel,
                mockEmbeddingStore(),
                tempDir.toString()
        );

        assertThatThrownBy(service::bootstrapDocuments)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to index compliance document: policy.txt")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @SuppressWarnings("unchecked")
    private static EmbeddingStore<TextSegment> mockEmbeddingStore() {
        return mock(EmbeddingStore.class);
    }
}
