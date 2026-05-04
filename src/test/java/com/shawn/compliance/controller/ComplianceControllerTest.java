package com.shawn.compliance.controller;

import com.shawn.compliance.dto.ComplianceDocumentIngestResponse;
import com.shawn.compliance.dto.ComplianceRequest;
import com.shawn.compliance.dto.ComplianceResponse;
import com.shawn.compliance.history.ComplianceHistoryEntity;
import com.shawn.compliance.history.ComplianceHistoryRepository;
import com.shawn.compliance.service.ComplianceDocumentService;
import com.shawn.compliance.service.ComplianceService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComplianceControllerTest {

    @Test
    void complianceCheckDelegatesToService() {
        ComplianceService service = mock(ComplianceService.class);
        ComplianceController controller = new ComplianceController(service);
        ComplianceRequest request = new ComplianceRequest("Policy", new ComplianceRequest.CustomerProfile(
                "customer-1", 40, 100000, "LOW", "GROWTH", "VERIFIED"
        ));
        ComplianceResponse expected = new ComplianceResponse("SUCCESS", "APPROVED", "Reason", "Explanation", "TEST");
        when(service.check(request)).thenReturn(expected);

        ComplianceResponse response = controller.check(request);

        assertThat(response).isSameAs(expected);
        verify(service).check(request);
    }

    @Test
    void documentBootstrapDelegatesToService() {
        ComplianceDocumentService service = mock(ComplianceDocumentService.class);
        ComplianceDocumentController controller = new ComplianceDocumentController(service);
        ComplianceDocumentIngestResponse expected = new ComplianceDocumentIngestResponse("SUCCESS", 2, 5);
        when(service.bootstrapDocuments()).thenReturn(expected);

        ComplianceDocumentIngestResponse response = controller.bootstrapDocuments();

        assertThat(response).isSameAs(expected);
        verify(service).bootstrapDocuments();
    }

    @Test
    void historyLookupReturnsEntityByRequestId() {
        ComplianceHistoryRepository repository = mock(ComplianceHistoryRepository.class);
        ComplianceHistoryController controller = new ComplianceHistoryController(repository);
        ComplianceHistoryEntity expected = history("request-1", "session-1");
        when(repository.findByRequestId("request-1")).thenReturn(Optional.of(expected));

        ComplianceHistoryEntity response = controller.getByRequestId("request-1");

        assertThat(response).isSameAs(expected);
    }

    @Test
    void historyLookupThrowsWhenRequestIsMissing() {
        ComplianceHistoryRepository repository = mock(ComplianceHistoryRepository.class);
        ComplianceHistoryController controller = new ComplianceHistoryController(repository);
        when(repository.findByRequestId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getByRequestId("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Request not found: missing");
    }

    @Test
    void historyLookupReturnsSessionHistory() {
        ComplianceHistoryRepository repository = mock(ComplianceHistoryRepository.class);
        ComplianceHistoryController controller = new ComplianceHistoryController(repository);
        List<ComplianceHistoryEntity> expected = List.of(history("request-2", "session-1"));
        when(repository.findBySessionIdOrderByCreatedAtDesc("session-1")).thenReturn(expected);

        List<ComplianceHistoryEntity> response = controller.getBySessionId("session-1");

        assertThat(response).isSameAs(expected);
    }

    private static ComplianceHistoryEntity history(String requestId, String sessionId) {
        return new ComplianceHistoryEntity(
                requestId,
                sessionId,
                "customer-1",
                "APPROVED",
                "Reason",
                "Explanation",
                "SUCCESS"
        );
    }
}
