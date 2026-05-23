# Architecture

## Problem

Digital payment complaints often require manual triage across CBS ledger status, switch status, gateway callbacks, UTR/reference checks, and customer complaint SLAs. Without a unified RCA queue, branch and support teams can miss reversal windows, answer customers late, or risk duplicate settlement handling.

## System Flow

```text
CBS / Switch / Gateway / Complaint Feed
        |
        v
Flyway-managed transaction tables
        |
        v
Spring Boot reconciliation service
        |
        +-- Rule detection
        +-- Severity and SLA assignment
        +-- RCA case upsert
        +-- Operator audit
        |
        v
REST API + JWT security + rate limiting
        |
        v
Operations dashboard
        |
        +-- Risk metrics
        +-- RCA queue
        +-- Transaction filters
        +-- Case resolution
        +-- Audit trail
```

## Backend Modules

| Module | Responsibility |
|---|---|
| Security | JWT token issue/validation, stateless protected APIs, secure headers |
| Request filters | Request ID propagation and fixed-window rate limiting |
| Reconciliation service | Detects mismatch patterns and creates or refreshes RCA cases |
| Summary API | Aggregates open cases, critical cases, channel exposure, and value at risk |
| Transaction API | Provides searchable and filterable payment feed data |
| Case API | Exposes RCA queue, detail, and resolution workflow |
| Audit API | Tracks operator actions for compliance and production support |
| Database | JPA entities, repositories, Flyway migrations, H2 local persistence, Oracle-ready schema |

## Reconciliation Rules

| Rule | Trigger | Severity | Suggested action |
|---|---|---|---|
| Customer debited, payment failed | CBS success + switch failed/reversed | High/Critical | Validate reversal, hold customer communication, confirm switch settlement |
| Beneficiary credit pending | CBS pending/timeout + switch success | High/Critical | Verify CBS posting and beneficiary credit |
| Gateway timeout | Gateway timeout beyond monitoring window | Medium/High | Pull final status and replay callback safely |
| Duplicate UTR | Same reference used by multiple transactions | High | Freeze duplicate posting path and validate source feed |
| SLA breach risk | Complaint open beyond SLA and unresolved | Critical | Escalate with transaction evidence pack |

## Database Notes

The Spring Boot app uses Flyway migrations under `src/main/resources/db/migration` for local/demo runtime. The `database/oracle_schema.sql` file contains the Oracle SQL/PLSQL reference implementation for bank-style deployment, including transaction, case, and audit tables plus reconciliation package procedures.
