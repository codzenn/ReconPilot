# ReconPilot API

Base URL (local): `http://localhost:8080`

## Authentication

ReconPilot uses email/password login with bcrypt hashing and issues JWT access tokens.

Protected endpoints require:

- `Authorization: Bearer <JWT>`

## Endpoints

### Health

- `GET /api/health` (public)

### Auth

- `POST /api/auth/signup` (create account; verification required)
- `GET /api/auth/verify?token=...` (verify email)
- `POST /api/auth/login` (issue JWT)
- `GET /api/auth/me` (current user profile)

### Dashboard

- `GET /api/summary` (protected)

### Transactions

- `GET /api/transactions?q=&channel=&risk=` (protected)

### Cases

- `GET /api/cases?status=` (protected)
- `GET /api/cases/{caseId}` (protected)
- `POST /api/cases/{caseId}/resolve` (protected)

Resolve payload:

```json
{ "resolutionNote": "Matched reversal file and branch callback evidence." }
```

### Reconciliation run

- `POST /api/reconcile/run` (protected)

### Audit

- `GET /api/audit` (protected)

## Error Format

Errors use a consistent JSON structure:

```json
{
  "timestamp": "2026-01-01T00:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Valid bearer token required",
  "path": "/api/summary",
  "requestId": "..."
}
```
