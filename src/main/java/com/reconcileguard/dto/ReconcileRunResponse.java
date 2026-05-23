package com.reconcileguard.dto;

public record ReconcileRunResponse(
        int scannedTransactions,
        int openedCases,
        int refreshedCases,
        long openCases,
        String message
) {
}
