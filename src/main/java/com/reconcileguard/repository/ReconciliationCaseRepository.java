package com.reconcileguard.repository;

import com.reconcileguard.domain.CaseStatus;
import com.reconcileguard.domain.IssueType;
import com.reconcileguard.domain.PaymentTransaction;
import com.reconcileguard.domain.ReconciliationCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReconciliationCaseRepository extends JpaRepository<ReconciliationCase, String> {
    Optional<ReconciliationCase> findByTransactionAndIssueType(PaymentTransaction transaction, IssueType issueType);
    long countByStatus(CaseStatus status);
    List<ReconciliationCase> findByStatus(CaseStatus status);
}
