package com.shawn.compliance.service;

import com.shawn.compliance.dto.ComplianceDocumentIngestResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class ComplianceDocumentService {

    private static final String LOCAL_DOCS_PATH = "src/main/resources/docs";
    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 100;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public ComplianceDocumentService(EmbeddingModel embeddingModel,
                                     EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    public ComplianceDocumentIngestResponse bootstrapDocuments() {
        File docsFolder = new File(LOCAL_DOCS_PATH);

        if (!docsFolder.exists() || !docsFolder.isDirectory()) {
            throw new IllegalArgumentException("Docs folder not found: " + docsFolder.getAbsolutePath());
        }

        File[] files = docsFolder.listFiles(this::isSupportedFile);
        if (files == null || files.length == 0) {
            return new ComplianceDocumentIngestResponse("SUCCESS", 0, 0);
        }

        int indexedFiles = 0;
        int totalChunks = 0;

        for (File file : files) {
            try {
                String rawText = extractText(file);
                List<TextSegment> chunks = toChunks(rawText, file.getName());

                for (TextSegment chunk : chunks) {
                    Embedding embedding = embeddingModel.embed(chunk).content();
                    embeddingStore.add(embedding, chunk);
                }

                indexedFiles++;
                totalChunks += chunks.size();

            } catch (Exception e) {
                throw new RuntimeException("Failed to index compliance document: " + file.getName(), e);
            }
        }

        return new ComplianceDocumentIngestResponse("SUCCESS", indexedFiles, totalChunks);
    }

    private List<TextSegment> toChunks(String text, String fileName) {
        List<TextSegment> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            String chunkText = text.substring(start, end);

            chunks.add(TextSegment.from("""
                    Source file: %s
                    Content:
                    %s
                    """.formatted(fileName, chunkText)));

            if (end == text.length()) {
                break;
            }

            start = Math.max(0, end - CHUNK_OVERLAP);
        }

        return chunks;
    }

    private boolean isSupportedFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }

        String fileName = file.getName().toLowerCase();

        return fileName.endsWith(".txt")
                || fileName.endsWith(".md")
                || fileName.endsWith(".pdf")
                || fileName.endsWith(".docx");
    }

    private String extractText(File file) throws Exception {
        String fileName = file.getName().toLowerCase();

        if (fileName.endsWith(".txt") || fileName.endsWith(".md")) {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        }

        if (fileName.endsWith(".pdf")) {
            try (PDDocument pdf = Loader.loadPDF(file)) {
                return new PDFTextStripper().getText(pdf);
            }
        }

        if (fileName.endsWith(".docx")) {
            try (InputStream inputStream = new FileInputStream(file);
                 XWPFDocument docx = new XWPFDocument(inputStream);
                 XWPFWordExtractor extractor = new XWPFWordExtractor(docx)) {
                return extractor.getText();
            }
        }

        throw new IllegalArgumentException("Unsupported file type: " + file.getName());
    }
}