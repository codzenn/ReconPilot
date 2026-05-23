package com.reconcileguard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "RG_PAYMENT_TXN")
public class PaymentTransaction {
    @Id
    @Column(name = "TXN_ID", length = 40)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "CHANNEL", nullable = false, length = 20)
    private PaymentChannel channel;

    @Column(name = "UTR", nullable = false, length = 60)
    private String utr;

    @Column(name = "CUSTOMER_NAME", nullable = false, length = 120)
    private String customerName;

    @Column(name = "BRANCH", nullable = false, length = 80)
    private String branch;

    @Column(name = "AMOUNT", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "INITIATED_AT", nullable = false)
    private LocalDateTime initiatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "CBS_STATUS", nullable = false, length = 20)
    private PaymentStatus cbsStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "SWITCH_STATUS", nullable = false, length = 20)
    private PaymentStatus switchStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "GATEWAY_STATUS", nullable = false, length = 20)
    private PaymentStatus gatewayStatus;

    @Column(name = "COMPLAINT_FLAG", nullable = false)
    private boolean customerComplaint;

    @Column(name = "RISK_SCORE", nullable = false)
    private int riskScore;

    protected PaymentTransaction() {
    }

    public PaymentTransaction(String id, PaymentChannel channel, String utr, String customerName, String branch,
                              BigDecimal amount, LocalDateTime initiatedAt, PaymentStatus cbsStatus,
                              PaymentStatus switchStatus, PaymentStatus gatewayStatus,
                              boolean customerComplaint, int riskScore) {
        this.id = id;
        this.channel = channel;
        this.utr = utr;
        this.customerName = customerName;
        this.branch = branch;
        this.amount = amount;
        this.initiatedAt = initiatedAt;
        this.cbsStatus = cbsStatus;
        this.switchStatus = switchStatus;
        this.gatewayStatus = gatewayStatus;
        this.customerComplaint = customerComplaint;
        this.riskScore = riskScore;
    }

    public String getId() { return id; }
    public PaymentChannel getChannel() { return channel; }
    public String getUtr() { return utr; }
    public String getCustomerName() { return customerName; }
    public String getBranch() { return branch; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getInitiatedAt() { return initiatedAt; }
    public PaymentStatus getCbsStatus() { return cbsStatus; }
    public PaymentStatus getSwitchStatus() { return switchStatus; }
    public PaymentStatus getGatewayStatus() { return gatewayStatus; }
    public boolean isCustomerComplaint() { return customerComplaint; }
    public int getRiskScore() { return riskScore; }
}
