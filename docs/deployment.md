# ReconPilot Deployment

## Configuration

ReconPilot is configured via environment variables (see `src/main/resources/application.yml`):

- `RG_PORT`
- `RG_DB_URL`, `RG_DB_USERNAME`, `RG_DB_PASSWORD`, `RG_DB_DRIVER`
- `RG_JWT_SECRET` (min 32 bytes)
- `RG_TOKEN_TTL_MINUTES`
- `RG_EMAIL_VERIFICATION_TTL_MINUTES`
- `RG_PASSWORD_RESET_TTL_MINUTES`
- `RG_EXPOSE_VERIFICATION_TOKEN` (set `false` in production)
- `RG_RATE_LIMIT_PER_MINUTE`

## Free Cloud Deployment (Render.com)

You can deploy ReconPilot completely for free using [Render](https://render.com/). We have included a `render.yaml` blueprint that provisions both a free web service and a free PostgreSQL database.

**Steps:**
1. Push this code to a GitHub repository.
2. Sign in to Render and go to the **Blueprints** page.
3. Click **New Blueprint Instance** and connect your GitHub repository.
4. Render will automatically read the `render.yaml` file, provision the Postgres database, and build/deploy the Docker container.
5. Once deployed, Render will provide you with a public `onrender.com` URL.

*Note: The free tier spins down after 15 minutes of inactivity, so the first request after a period of inactivity may take ~50 seconds to respond.*

## Docker

Build and run:

```bash
docker compose up --build
```

Oracle profile (for integration testing with Oracle Free):

```bash
docker compose --profile oracle up --build
```

## Production Notes

- Terminate TLS at a load balancer or ingress and forward to the app over a trusted network.
- Store secrets in a proper secret manager (Vault/KMS/Kubernetes Secrets).
- Set `RG_EXPOSE_VERIFICATION_TOKEN=false` and send verification/reset links via an email provider.
- Export logs/metrics to your observability stack (SIEM + APM).
- Run Flyway migrations against a dedicated Oracle or Postgres environment and validate indexes with real query plans.
