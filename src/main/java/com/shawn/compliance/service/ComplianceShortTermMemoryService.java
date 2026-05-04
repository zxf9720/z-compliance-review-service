package com.shawn.compliance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
public class ComplianceShortTermMemoryService {

    private static final int MAX_MESSAGES = 20;
    private static final Duration TTL = Duration.ofHours(2);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ComplianceShortTermMemoryService(StringRedisTemplate redisTemplate,
                                            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void addMessage(String sessionId, String role, String content) {
        try {
            String key = "agent-c:memory:" + sessionId;

            String value = objectMapper.writeValueAsString(Map.of(
                    "role", role,
                    "content", content,
                    "timestamp", Instant.now().toString()
            ));

            redisTemplate.opsForList().rightPush(key, value);
            redisTemplate.opsForList().trim(key, -MAX_MESSAGES, -1);
            redisTemplate.expire(key, TTL);

        } catch (Exception e) {
            throw new RuntimeException("Failed to write compliance short-term memory", e);
        }
    }
}