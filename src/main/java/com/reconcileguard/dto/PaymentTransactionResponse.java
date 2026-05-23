package com.reconcileguard.dto;

import com.reconcileguard.domain.PaymentTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentTransactionResponse(
        String id,
        String channel,
        String utr,
        String customerName,
        String branch,
        BigDecimal amount,
        LocalDateTime initiatedAt,
        String cbsStatus,
        String switchStatus,
        String gatewayStatus,
        boolean customerComplaint,
        int riskScore,
        String riskBand
) {
    public static PaymentTransactionResponse from(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(
                transaction.getId(),
                transaction.getChannel().name(),
                transaction.getUtr(),
                transaction.getCustomerName(),
                transaction.getBranch(),
                transaction.getAmount(),
                transaction.getInitiatedAt(),
                transaction.getCbsStatus().name(),
                transaction.getSwitchStatus().name(),
                transaction.getGatewayStatus().name(),
                transaction.isCustomerComplaint(),
                transaction.getRiskScore(),
                riskBand(transaction.getRiskScore())
        );
    }

    private static String riskBand(int score) {
        if (score >= 85) return "CRITICAL";
        if (score >= 70) return "HIGH";
        if (score >= 50) return "MEDIUM";
        return "LOW";
    }
}
