package com.shawn.compliance.controller;

import com.shawn.compliance.history.ComplianceHistoryEntity;
import com.shawn.compliance.history.ComplianceHistoryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/compliance/history")
public class ComplianceHistoryController {

    private final ComplianceHistoryRepository historyRepository;

    public ComplianceHistoryController(ComplianceHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @GetMapping("/requests/{requestId}")
    public ComplianceHistoryEntity getByRequestId(@PathVariable String requestId) {
        return historyRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));
    }

    @GetMapping("/sessions/{sessionId}")
    public List<ComplianceHistoryEntity> getBySessionId(@PathVariable String sessionId) {
        return historyRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
    }
}