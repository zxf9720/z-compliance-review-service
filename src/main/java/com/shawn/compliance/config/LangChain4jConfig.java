package com.shawn.compliance.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChain4jConfig {

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

    @Bean
    public ChatModel ollamaChatModel(
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.model}") String modelName
    ) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.2)
                .timeout(Duration.ofSeconds(30))
                .build();
    }
}