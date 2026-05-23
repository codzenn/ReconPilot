package com.reconcileguard.dto;

import java.math.BigDecimal;
import java.util.Map;

public record SummaryResponse(
        long totalTransactions,
        long openCases,
        long criticalCases,
        BigDecimal valueAtRisk,
        Map<String, Long> channels
) {
}
