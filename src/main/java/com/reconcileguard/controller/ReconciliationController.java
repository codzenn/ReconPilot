package com.reconcileguard.controller;

import com.reconcileguard.dto.AuditEventResponse;
import com.reconcileguard.dto.PaymentTransactionResponse;
import com.reconcileguard.dto.ReconcileRunResponse;
import com.reconcileguard.dto.ReconciliationCaseResponse;
import com.reconcileguard.dto.ResolveCaseRequest;
import com.reconcileguard.dto.SummaryResponse;
import com.reconcileguard.security.AuthPrincipal;
import com.reconcileguard.service.ReconciliationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReconciliationController {
    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "ReconPilot",
                "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('OPS','ANALYST','AUDITOR','ADMIN')")
    public SummaryResponse summary() {
        return reconciliationService.summary();
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('OPS','ANALYST','AUDITOR','ADMIN')")
    public List<PaymentTransactionResponse> transactions(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String risk
    ) {
        return reconciliationService.transactions(q, channel, risk);
    }

    @GetMapping("/cases")
    @PreAuthorize("hasAnyRole('OPS','ANALYST','AUDITOR','ADMIN')")
    public List<ReconciliationCaseResponse> cases(@RequestParam(required = false) String status) {
        return reconciliationService.cases(status);
    }

    @GetMapping("/cases/{caseId}")
    @PreAuthorize("hasAnyRole('OPS','ANALYST','AUDITOR','ADMIN')")
    public ReconciliationCaseResponse caseById(@PathVariable String caseId) {
        return reconciliationService.caseById(caseId);
    }

    @PostMapping("/reconcile/run")
    @PreAuthorize("hasAnyRole('OPS','ADMIN')")
    public ReconcileRunResponse runReconciliation(
            @RequestHeader(value = "X-Operator-Id", required = false) String operator,
            Authentication authentication
    ) {
        return reconciliationService.runReconciliation(actor(operator, authentication));
    }

    @PostMapping("/cases/{caseId}/resolve")
    @PreAuthorize("hasAnyRole('OPS','ADMIN')")
    public ReconciliationCaseResponse resolveCase(
            @PathVariable String caseId,
            @Valid @RequestBody ResolveCaseRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) String operator,
            Authentication authentication
    ) {
        return reconciliationService.resolveCase(caseId, request, actor(operator, authentication));
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAnyRole('AUDITOR','ADMIN')")
    public List<AuditEventResponse> auditTrail() {
        return reconciliationService.auditTrail();
    }

    private static String actor(String operator, Authentication authentication) {
        if (StringUtils.hasText(operator)) {
            return operator.trim();
        }
        if (authentication != null && authentication.getPrincipal() instanceof AuthPrincipal principal) {
            if (StringUtils.hasText(principal.getEmail())) {
                return principal.getEmail();
            }
        }
        return "system";
    }
}
