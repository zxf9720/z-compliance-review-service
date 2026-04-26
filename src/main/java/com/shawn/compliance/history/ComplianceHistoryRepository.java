package com.shawn.compliance.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComplianceHistoryRepository extends JpaRepository<ComplianceHistoryEntity, Long> {

    Optional<ComplianceHistoryEntity> findByRequestId(String requestId);

    List<ComplianceHistoryEntity> findBySessionIdOrderByCreatedAtDesc(String sessionId);
}