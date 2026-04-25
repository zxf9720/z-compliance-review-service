package com.shawn.compliance.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChain4jConfig {

    @Bean
    public ChatModel ollamaChatModel(
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.chat-model}") String modelName
    ) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.2)
                .timeout(Duration.ofSeconds(30))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.embedding-model}") String modelName
    ) {
        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(30))
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> complianceEmbeddingStore(
            EmbeddingModel embeddingModel,
            @Value("${pgvector.host}") String host,
            @Value("${pgvector.port}") int port,
            @Value("${pgvector.database}") String database,
            @Value("${pgvector.user}") String user,
            @Value("${pgvector.password}") String password,
            @Value("${pgvector.table}") String table
    ) {
        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(user)
                .password(password)
                .table(table)
                .dimension(embeddingModel.dimension())
                .createTable(true)
                .build();
    }

    //    @Bean
//    public ChatModel openAiChatModel(
//            @Value("${openai.api-key}") String apiKey,
//            @Value("${openai.model}") String modelName
//    ) {
//        if (apiKey == null || apiKey.isBlank()) {
//            return null;
//        }
//
//        return OpenAiChatModel.builder()
//                .apiKey(apiKey)
//                .modelName(modelName)
//                .temperature(0.2)
//                .timeout(Duration.ofSeconds(30))
//                .build();
//    }
}


