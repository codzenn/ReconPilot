# ReconPilot User Guide

## Overview

ReconPilot is an operations dashboard for reconciliation and RCA workflows:

- Dashboard KPIs and channel exposure
- RCA queue for exception triage
- Transaction search and risk context
- Case resolution with operator audit trail

## Sign Up + Sign In

1. Open the landing page at `/` and click **Create account**.
2. Complete email verification (local dev can expose a token based on configuration).
3. Sign in at `/signin.html` to receive a JWT session token.

## Dashboard

- Review total transactions scanned, open cases, critical cases, and value at risk.
- Review channel exposure to identify where operational load is accumulating (UPI/IMPS/NEFT/CARD).
- Use the **Critical RCA Queue** to jump to high-risk cases quickly.

## RCA Queue

- Search by case ID, issue type, UTR, or customer.
- Resolve a case to close it and write an audit event.

## Transactions

- Search by UTR, customer, or branch.
- Filter by channel and risk.
- Inspect status across CBS, switch, and gateway.

## Audit Trail

- Review operator actions for compliance and production support handoffs.

## Operator Best Practices

- Use unique email accounts for each operator.
- Assign roles (OPS/ANALYST/AUDITOR/ADMIN) based on responsibilities.
