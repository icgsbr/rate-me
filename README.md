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
| `notification-function` | n/a | functional | **Azure Function**: escalates a critical event to the administrators by e-mail |

`notification-function` is packaged as an Azure Function rather than a standalone web app, so
it has no port of its own — the Functions host owns the listener. See
[Azure Function](#notification-function-azure-function) below.

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
| `POST` | `/courses` | ADMIN | register a course from `{name, description}` |
| `GET` | `/courses` | any authenticated | list courses (`id`, `name`, `description`) — the source of the `courseId` below |
| `POST` | `/feedback` | STUDENT | submit `{description, score, courseId}`; unknown `courseId` is a 404; `score ≤ 1` triggers an admin e-mail alert and sets `notified` |
| `GET` | `/feedback` | STUDENT | list own feedback |
| `GET` | `/feedback?page=&size=` | ADMIN | list all feedback, paginated |

Login is done **directly on the auth-service** (`POST /auth/login`). The JWT carries no
role claim, so the role is resolved locally (admin table first, then student) from the
`authId` claim. Swagger UI: `http://localhost:8086/q/swagger-ui`.

### report-service (8087)
| Method | Route | Description |
|---|---|---|
| `POST` | `/reports/weekly/run` | build last-7-days report, store it, e-mail it, return JSON |
| `GET` | `/reports/weekly` | preview last-7-days report as JSON, without storage or e-mail |

Report metrics: evaluations per day, low-score count + student ids, average evaluations
per week, average score over the period. The weekly cadence is a disabled
`@Scheduled` job (`app.report.cron`) standing in for the future Azure Timer Trigger.

### notification-function (Azure Function)
| Method | Route | Auth | Description |
|---|---|---|---|
| `GET` | `/api/health` | anonymous | liveness probe |
| `POST` | `/api/notifications/critical` | function key | escalate a critical event by e-mail |

The alert payload carries exactly the three fields the challenge asks for:

```json
{ "descricao": "Database unreachable", "urgencia": "CRITICA", "dataEnvio": "2026-07-24T00:30:00Z" }
```

`urgencia` is one of `BAIXA`, `MEDIA`, `ALTA`, `CRITICA`; `dataEnvio` is optional and
defaults to the moment the event is received. The key goes in the `x-functions-key` header
(or `?code=`), which is the access governance for this endpoint.

Built as a container on `mcr.microsoft.com/azure-functions/java:4-java21-appservice` and
deployed to the Function App `rate-me-notification-function`; the image lives in
`ratemeacr.azurecr.io/notification-function`.

## Running locally

Prerequisites: Java 21, Maven, Docker.

```bash
# 1. Dependencies (PostgreSQL + MailHog)
docker compose up -d postgres mailhog

# 2. auth-service (separate repo) on port 8085, with RSA keys and JWT_ISSUER=cheffy-auth
#    cd ../cheffy-microservices/auth-service && ./init-keys.sh && mvn spring-boot:run ...

# 3. feedback-service (applies the Flyway migrations on start)
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

# register a course (needs an ADMIN token) and pick its id
curl -X POST localhost:8086/courses -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Arquitetura de Software","description":"Padroes e estilos arquiteturais"}'

# list the courses to get the courseId (any authenticated caller)
COURSE_ID=$(curl -s localhost:8086/courses -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')

# submit a critical feedback (score 1 -> alert e-mail in MailHog)
curl -X POST localhost:8086/feedback -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"description\":\"Too fast\",\"score\":1,\"courseId\":\"$COURSE_ID\"}"

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
- **`.github/workflows/notification-function-cd.yml`** — on push to `main` touching
  `notification-function/`: builds the image, pushes both `:latest` and `:<commit>` to
  `ratemeacr` and restarts the Function App.

## Cloud deployment

Everything runs in the resource group `rg-dev-servless-FIAP` (East US):

| Component | Azure resource | Image |
|---|---|---|
| auth-service | Container App `auth-service-fiap` | `ratemeacr.azurecr.io/auth-service:observability` |
| feedback-service | Container App `feedback-service-fiap` | `ratemeacr.azurecr.io/feedback-service:observability` |
| report-service | Function App `rate-me-report-service` (Timer Trigger) | `ratemeacr.azurecr.io/report-service:latest` |
| notification-function | Function App `rate-me-notification-function` (HTTP Trigger) | `ratemeacr.azurecr.io/notification-function:latest` |

Both Function Apps share the `rate-me-dedicated-plan` (B1). A container image on Azure
Functions requires a Premium or Dedicated plan — Flex Consumption does not support custom
containers — and reusing the existing plan keeps the extra cost at zero.

### Monitoring

Each service has its own Application Insights component, all wired to the shared Log
Analytics workspace `workspacergdevservlessfiapb71d`. Instrumentation is the Application
Insights **Java agent 3.7.8** baked into every image, so telemetry is collected without
touching business code; `APPLICATIONINSIGHTS_ROLE_NAME` is what separates the services in
the portal. On the Function Apps the agent is attached through the `JAVA_OPTS` app setting,
which is the documented mechanism on a Dedicated plan.

### Remaining cloud work

- `ReportStoragePort` → **Azure Cosmos DB** (NoSQL); the mailer adapters → **Azure
  Communication Services**.
- SMTP credentials → **Azure Key Vault** (the database credentials already are).
