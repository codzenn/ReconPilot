package com.reconcileguard.dto;

import com.reconcileguard.domain.ReconciliationCase;

import java.time.LocalDateTime;

public record ReconciliationCaseResponse(
        String id,
        String transactionId,
        String issueType,
        String severity,
        String status,
        int slaHours,
        String rootCause,
        String recommendedAction,
        String owner,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String resolutionNote,
        PaymentTransactionResponse transaction
) {
    public static ReconciliationCaseResponse from(ReconciliationCase reconCase) {
        return new ReconciliationCaseResponse(
                reconCase.getId(),
                reconCase.getTransaction().getId(),
                reconCase.getIssueType().name(),
                reconCase.getSeverity().name(),
                reconCase.getStatus().name(),
                reconCase.getSlaHours(),
                reconCase.getRootCause(),
                reconCase.getRecommendedAction(),
                reconCase.getOwnerQueue(),
                reconCase.getCreatedAt(),
                reconCase.getUpdatedAt(),
                reconCase.getResolutionNote(),
                PaymentTransactionResponse.from(reconCase.getTransaction())
        );
    }
}
