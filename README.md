# rate-me — feedback platform

FIAP/POSTECH Tech Challenge (Fase 4). A platform where **students rate lessons**
(`description` + `score` 0–10) and **administrators** consume reports. Built with
**Java 21 + Quarkus**, **hexagonal architecture**, **PostgreSQL + Flyway**, distributed
**JWT** validation and **SLF4J + MDC + Logback** logging.

Authentication is delegated to an external **auth-service** (Spring Boot, repo
`cheffy-microservices`, port 8085): it registers users, issues RS256 JWTs and exposes a
JWKS endpoint for distributed validation.

## Modules

| Module | Port | Status | Responsibility |
|---|---|---|---|
| `feedback-service` | 8086 | functional | registration (via auth-service), feedback submission/listing, low-score e-mail alert |
| `report-service` | 8087 | functional | weekly report (metrics + e-mail); reads the feedback DB read-only |
| `notification-function` | 8088 | scaffold | critical-event alert — real trigger is Azure Monitor (cloud only) |

## Architecture (hexagonal)

Each service is split into `domain` (pure models), `application` (use-case input ports +
output ports + services) and `adapter` (input: web/security/schedule; output:
persistence/auth/notification/storage). Dependencies always point inward, so adapters
(e.g. the SMTP notifier or the NoSQL store) can be swapped for cloud equivalents without
touching business logic.

```
adapter.input  (REST, security/MDC, scheduler)
       │  ↓ input ports
application    (use cases + services)
       │  ↓ output ports
adapter.output (JPA, auth REST client, mailer, storage)
domain         (Feedback, Student, Admin, Course, WeeklyReport)
```

## Endpoints

### feedback-service (8086)
| Method | Route | Role | Description |
|---|---|---|---|
| `POST` | `/cadastro` | public | register a student/admin (calls auth-service, persists `auth_id`) |
| `POST` | `/feedback` | STUDENT | submit `{description, score}`; `score ≤ 1` triggers an admin e-mail alert and sets `notified` |
| `GET` | `/feedback` | STUDENT | list own feedback |
| `GET` | `/feedback?page=&size=` | ADMIN | list all feedback, paginated |

Login is done **directly on the auth-service** (`POST /auth/login`). The JWT carries no
role claim, so the role is resolved locally (admin table first, then student) from the
`authId` claim. Swagger UI: `http://localhost:8086/q/swagger-ui`.

### report-service (8087)
| Method | Route | Description |
|---|---|---|
| `POST` | `/reports/weekly/run` | build last-7-days report, e-mail it, return JSON |
| `GET` | `/reports/weekly` | same (preview) |

Report metrics: evaluations per day, low-score count + student ids, average evaluations
per week, average score over the period. The weekly cadence is a disabled
`@Scheduled` job (`app.report.cron`) standing in for the future Azure Timer Trigger.

## Running locally

Prerequisites: Java 21, Maven, Docker.

```bash
# 1. Dependencies (PostgreSQL + MailHog)
docker compose up -d postgres mailhog

# 2. auth-service (separate repo) on port 8085, with RSA keys and JWT_ISSUER=cheffy-auth
#    cd ../cheffy-microservices/auth-service && ./init-keys.sh && mvn spring-boot:run ...

# 3. feedback-service (applies Flyway V1 on start)
cd feedback-service && mvn quarkus:dev      # http://localhost:8086

# 4. report-service
cd report-service && mvn quarkus:dev        # http://localhost:8087
```

Inspect captured e-mails (low-score alerts and weekly reports) in MailHog at
`http://localhost:8025`.

To run everything in containers (services included): `docker compose up -d --build`.

### Example flow

```bash
# register a student
curl -X POST localhost:8086/cadastro -H 'Content-Type: application/json' \
  -d '{"name":"Alice","login":"alice","password":"Password@1234","role":"STUDENT"}'

# get a token from the auth-service
TOKEN=$(curl -s -X POST localhost:8085/auth/login -H 'Content-Type: application/json' \
  -d '{"login":"alice","password":"Password@1234"}' | jq -r .token)

# submit a critical feedback (score 1 -> alert e-mail in MailHog)
curl -X POST localhost:8086/feedback -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"description":"Too fast","score":1}'

# list my feedback
curl localhost:8086/feedback -H "Authorization: Bearer $TOKEN"

# generate the weekly report (e-mail in MailHog)
curl -X POST localhost:8087/reports/weekly/run
```

## CI/CD

- **`.github/workflows/ci.yml`** — on PRs to `main`/`develop`: `mvn -B verify` for all
  modules (SonarQube optional, behind `SONAR_TOKEN`).
- **`.github/workflows/cd.yml`** — on push to `main`: per-service matrix builds and pushes
  Docker images to Docker Hub. Azure deployment steps are commented placeholders.

## Cloud readiness (next phase)

The structure is ready for the serverless/cloud pieces without business-logic changes:

- **report-service** → Azure **Timer Trigger** Function (weekly); `ReportStoragePort` →
  **Azure Cosmos DB** (NoSQL); `ReportNotificationPort`/`NotificationPort` → **Azure
  Communication Services**.
- **notification-function** → Azure **Function** fired by **Azure Monitor + Application
  Insights** on `error` logs (the MDC-structured logs are the foundation for this).
- Secrets (admin e-mail, SMTP, DB) → **Azure Key Vault**.
