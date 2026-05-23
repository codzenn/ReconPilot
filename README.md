# ReconPilot

ReconPilot is a production-style full-stack banking operations platform for digital payment reconciliation, customer complaint RCA, and SLA-driven support. It targets a real bank problem: customers see account debits, failed or delayed UPI/IMPS/NEFT/card outcomes, duplicate references, and branch teams manually compare CBS, switch, gateway, and complaint records before they can answer the customer.

## Why It Stands Out

- Solves a bank-relevant operational problem instead of a generic CRUD flow.
- Uses Java 21, Spring Boot, Spring Security (bcrypt + JWT), REST APIs, JPA, Flyway, SQL, and a responsive operations UI.
- Includes RCA generation, duplicate UTR detection, case resolution, audit history, health checks, rate limiting, request IDs, seed data, Docker, and CI.
- Maps directly to the South Indian Bank Full Stack Developer JD: Core Java, Spring Boot, REST APIs, React-style UI, PL/SQL/SQL design, secure coding, code reviews, testing, production support, RCA, UAT, and cross-team delivery.

## Features

- Dashboard for UPI, IMPS, NEFT, and card exception exposure.
- Automated reconciliation rules:
  - Customer debited but switch settlement failed or reversed.
  - Switch success while CBS posting is pending/timed out.
  - Gateway timeout without final callback.
  - Duplicate UTR/reference across payment records.
  - Customer complaint SLA breach risk.
- RCA queue with severity, SLA, owner queue, root cause, and recommended action.
- Case resolution workflow with operator audit trail.
- Email/password authentication with bcrypt hashing, email verification, JWT session tokens, RBAC enforcement, request IDs, security headers, and fixed-window API rate limiting.
- Flyway migrations with H2 local persistence and Oracle-ready schema design.
- Docker Compose for local deployment, plus optional Oracle Free profile.

## Tech Stack

- Backend: Java 21, Spring Boot 3, Spring Web, Spring Data JPA, Spring Security
- Data: H2 for local/demo, Oracle JDBC dependency, Flyway migrations, Oracle SQL/PLSQL reference schema
- Frontend: HTML, CSS, JavaScript operations dashboard
- Operations: Actuator health, Docker, GitHub Actions CI, request IDs, audit trail, rate limiting

## Run Locally With Maven

```powershell
mvn spring-boot:run
```

Open:

```text
http://localhost:8080
```

Sign in:

- - Create an account at `http://localhost:8080/signup.html`
- - Verify email (dev token is returned when `RG_EXPOSE_VERIFICATION_TOKEN=true`)
- - Sign in at `http://localhost:8080/signin.html`

## Run With Docker

```powershell
docker compose up --build
```

For real Oracle testing:

```powershell
docker compose --profile oracle up --build
```

Then set the app datasource to the Oracle service in `docker-compose.yml` or your deployment environment.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `RG_PORT` | `8080` | HTTP port |
| `RG_DB_URL` | H2 file DB | JDBC URL |
| `RG_DB_USERNAME` | `sa` | Database user |
| `RG_DB_PASSWORD` | empty | Database password |
| `RG_DB_DRIVER` | `org.h2.Driver` | JDBC driver |
| `RG_JWT_SECRET` | local value | JWT signing secret (min 32 bytes) |
| `RG_TOKEN_TTL_MINUTES` | `480` | JWT expiry |
| `RG_EMAIL_VERIFICATION_TTL_MINUTES` | `60` | Email verification token expiry |
| `RG_PASSWORD_RESET_TTL_MINUTES` | `30` | Password reset token expiry |
| `RG_EXPOSE_VERIFICATION_TOKEN` | `true` | Return verify/reset tokens in API responses (set `false` in production) |
| `RG_RATE_LIMIT_PER_MINUTE` | `120` | Protected API limit per operator/IP |

## API

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/signup` | Register a new user (email verification required) |
| GET | `/api/auth/verify` | Verify email via token |
| POST | `/api/auth/login` | Sign in and receive a JWT |
| GET | `/api/auth/me` | Current user profile |
| GET | `/api/health` | Public health status |
| GET | `/api/summary` | Dashboard KPIs and channel exposure |
| GET | `/api/transactions` | Filterable transaction feed |
| GET | `/api/cases` | RCA queue |
| GET | `/api/cases/{caseId}` | RCA case detail |
| POST | `/api/reconcile/run` | Run reconciliation rules |
| POST | `/api/cases/{caseId}/resolve` | Resolve case and write audit |
| GET | `/api/audit` | Operator audit history |

Protected calls require:

```text
Authorization: Bearer <JWT>
```

## Test

```powershell
mvn test
```

The service tests validate seeded transaction exposure, reconciliation rule execution, case resolution, and audit writeback.

## Legacy No-Dependency Demo

The original dependency-free Java runner is kept in `src/com/reconcileguard/Main.java` for environments without Maven:

```powershell
.\run.ps1
```

This fallback is useful for quick demos, while the main production-grade implementation lives under `src/main/java`.

## Resume Bullets

- Built **ReconPilot**, an end-to-end digital payment reconciliation and RCA platform using **Java 21, Spring Boot, Spring Security JWT, REST APIs, JPA, Flyway, SQL, and responsive UI**, addressing UPI/IMPS/NEFT/card exception handling for banking operations.
- Implemented reconciliation rules for **customer-debited failures, beneficiary-credit pending cases, gateway timeouts, duplicate UTRs, and SLA breach risks**, generating prioritized RCA cases with owner queues, recommended actions, and audit history.
- Designed a production-ready backend with **JWT authentication, request IDs, rate limiting, validation, standardized error responses, Actuator health checks, Docker deployment, CI tests, and database migrations**.
- Modeled the database layer with **indexed transaction/case/audit tables**, H2 local persistence, Oracle JDBC readiness, and an Oracle SQL/PLSQL reference schema for real banking deployment.
