package com.reconcileguard.service;

import com.reconcileguard.dto.ResolveCaseRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:reconcileguard-test;MODE=Oracle;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class ReconciliationServiceTest {
    @Autowired
    private ReconciliationService reconciliationService;

    @Test
    void summaryReflectsSeededOperationalQueue() {
        var summary = reconciliationService.summary();

        assertThat(summary.totalTransactions()).isEqualTo(40);
        assertThat(summary.openCases()).isGreaterThanOrEqualTo(8);
        assertThat(summary.valueAtRisk()).isPositive();
        assertThat(summary.channels()).containsKeys("UPI", "IMPS", "NEFT", "CARD");
    }

    @Test
    void reconciliationRunRefreshesExistingCasesWithoutDuplicates() {
        var result = reconciliationService.runReconciliation("ops.test");

        assertThat(result.scannedTransactions()).isEqualTo(40);
        assertThat(result.openCases()).isGreaterThanOrEqualTo(8);
        assertThat(result.openedCases() + result.refreshedCases()).isGreaterThan(0);
    }

    @Test
    void resolveCaseClosesQueueItemAndWritesAudit() {
        String caseId = reconciliationService.cases("OPEN").get(0).id();

        var resolved = reconciliationService.resolveCase(
                caseId,
                new ResolveCaseRequest("Matched reversal file and branch callback evidence."),
                "checker.test"
        );

        assertThat(resolved.status()).isEqualTo("RESOLVED");
        assertThat(reconciliationService.auditTrail())
                .anyMatch(event -> "CASE_RESOLVED".equals(event.action()) && caseId.equals(event.reference()));
    }
}
