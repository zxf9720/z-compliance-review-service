package com.shawn.compliance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComplianceShortTermMemoryServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void addMessageWritesJsonAndMaintainsWindowAndTtl() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ListOperations<String, String> listOperations = mock(ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        ComplianceShortTermMemoryService service = new ComplianceShortTermMemoryService(redisTemplate, objectMapper);

        service.addMessage("session-1", "USER", "hello");

        String key = "agent-c:memory:session-1";
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(listOperations).rightPush(org.mockito.ArgumentMatchers.eq(key), valueCaptor.capture());
        JsonNode savedMessage = objectMapper.readTree(valueCaptor.getValue());
        assertThat(savedMessage.get("role").asText()).isEqualTo("USER");
        assertThat(savedMessage.get("content").asText()).isEqualTo("hello");
        assertThat(savedMessage.get("timestamp").asText()).isNotBlank();
        verify(listOperations).trim(key, -20, -1);
        verify(redisTemplate).expire(key, Duration.ofHours(2));
    }

    @Test
    void addMessageWrapsRedisFailures() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForList()).thenThrow(new IllegalStateException("redis unavailable"));
        ComplianceShortTermMemoryService service = new ComplianceShortTermMemoryService(redisTemplate, objectMapper);

        assertThatThrownBy(() -> service.addMessage("session-1", "USER", "hello"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to write compliance short-term memory")
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
