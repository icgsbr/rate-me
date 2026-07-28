# rate-me — feedback platform

Students rate the lessons they attend, administrators get told when something goes wrong and
receive a periodic report with the numbers. Built for the FIAP/POSTECH Tech Challenge (Fase 4)
with **Java 21 + Quarkus 3.15**, hexagonal architecture, **PostgreSQL + Flyway**, RS256 **JWT**
validated against a remote JWKS, and two **Azure Functions** for everything that is triggered by
time rather than by a user.

## Architecture at a glance

```
             ┌──────────────────┐        ┌──────────────────────────┐
 student ───▶│ feedback-service │───────▶│  PostgreSQL (feedback)   │
 admin   ───▶│  Container App   │        └──────────────────────────┘
             └────────┬─────────┘                     ▲ read-only
                      │ score <= 1                    │
                      ▼ e-mail                ┌───────┴──────────┐
                  administrator ◀─── e-mail ──│  report-service  │ timer
                      ▲                       │   Function App   │
                      │ e-mail                └──────────────────┘
             ┌────────┴───────────────┐
             │ notification-function  │ timer + HTTP
             │      Function App      │◀── queries Application Insights
             └────────────────────────┘
```

Each module is cut the same way: `domain` holds plain models with no framework in them,
`application` holds the use cases plus the input/output ports, and `adapter` holds everything that
talks to the outside world (REST resources, function triggers, JPA, SMTP, the auth REST client,
the Application Insights query client). Dependencies only point inward, which is why the SMTP
notifier or the report storage can be replaced without the use cases noticing, and why the whole
test suite runs with no database and no mail server.

Authentication is delegated to an external **auth-service** (Spring Boot, repository
`cheffy-microservices`, port 8085 locally). It registers users, issues RS256 tokens and publishes
a JWKS endpoint the other services validate against.

## Modules

| Module | Runs as | Responsibility |
|---|---|---|
| `feedback-service` | Container App (port 8086) | registration, course catalogue, feedback submission and listing, low-score alert |
| `report-service` | Function App (Timer Trigger) | weekly metrics over the feedback data, stored and e-mailed |
| `notification-function` | Function App (HTTP + Timer Trigger) | escalates a critical event by e-mail; sweeps the feedback-service error logs |

## Endpoints

### feedback-service

| Method | Route | Who | Description |
|---|---|---|---|
| `POST` | `/cadastro` | public | registers a student or admin: calls the auth-service and stores the returned `auth_id` |
| `POST` | `/courses` | ADMIN | registers a course from `{name, description}` |
| `GET` | `/courses` | any valid JWT | lists courses; this is where the `courseId` below comes from |
| `POST` | `/feedback` | STUDENT | submits `{description, score, courseId}`; an unknown course is a 404, `score <= 1` sends the admin alert and sets `notified` |
| `GET` | `/feedback` | STUDENT | lists the caller's own feedback |
| `GET` | `/feedback?page=&size=` | ADMIN | lists all feedback, paginated (defaults `page=0`, `size=20`) |

Login happens directly on the auth-service (`POST /auth/login`). Its token carries no role claim,
so the role is resolved locally from the `authId` claim: the admin table is checked first, then the
student table. Swagger UI: `http://localhost:8086/q/swagger-ui`.

### report-service

`generateWeeklyReport` is a Timer Trigger driven by the `CRON_SCHEDULE` app setting. It builds the
report over the last 7 days, stores it and e-mails it to `REPORT_EMAIL`. The same use case is
exposed over HTTP for inspection when the service runs as a plain web app:

| Method | Route | Description |
|---|---|---|
| `POST` | `/reports/weekly/run` | builds the report, stores it, e-mails it and returns it as JSON |
| `GET` | `/reports/weekly` | returns the same report as JSON without storing or e-mailing it |

The report carries evaluations per day, total evaluations, low-score count with the student ids
behind it, average evaluations per week and the average score over the period.

### notification-function

| Function | Trigger | Auth | Description |
|---|---|---|---|
| `notifyCritical` | `POST /api/notifications/critical` | function key | e-mails a critical event to the administrators |
| `health` | `GET /api/health` | anonymous | liveness probe |
| `scanFeedbackErrors` | Timer (`LOG_SCAN_SCHEDULE`) | n/a | queries the feedback-service Application Insights for ERROR records and e-mails a digest |

The alert payload carries exactly the three fields the challenge asks for:

```json
{ "descricao": "Database unreachable", "urgencia": "CRITICA", "dataEnvio": "2026-07-24T00:30:00Z" }
```

`urgencia` is one of `BAIXA`, `MEDIA`, `ALTA`, `CRITICA`; `dataEnvio` is optional and defaults to
the moment the event is received. The key goes in the `x-functions-key` header (or `?code=`).

`scanFeedbackErrors` scans the window `(previous run − lag, now − lag]`, the lag covering
Application Insights ingestion latency. It keeps no state of its own: the Functions host persists
the schedule status in the storage account.

## Data model

Owned by `feedback-service` and versioned with Flyway (`quarkus.flyway.migrate-at-start=true`);
`report-service` reads the same database and never writes DDL.

| Table | Columns |
|---|---|
| `student` | `id` (UUID PK), `name`, `registration_number`, `auth_id` (indexed) |
| `admin` | `id` (UUID PK), `name`, `auth_id` (indexed), `role` |
| `course` | `id` (UUID PK), `name`, `description` |
| `feedback` | `id` (identity PK), `student_id` → `student`, `course_id` → `course`, `score` (0–10, checked), `review_description`, `review_date`, `notified`, `notified_date` |

`V1__init_schema.sql` creates the four tables. `V2__course_description_and_required_feedback_course.sql`
adds `course.description`, backfills existing rows onto the seeded course and makes
`feedback.course_id` mandatory.

## Running locally

Prerequisites: Java 21, Maven, Docker.

```bash
# 1. dependencies: PostgreSQL + MailHog
docker compose up -d postgres mailhog

# 2. auth-service (separate repository) on port 8085, with RSA keys and JWT_ISSUER=cheffy-auth
#    cd ../cheffy-microservices/auth-service && mvn spring-boot:run

# 3. feedback-service (applies the Flyway migrations on startup)
cd feedback-service && mvn quarkus:dev      # http://localhost:8086

# 4. report-service
cd report-service && mvn quarkus:dev        # http://localhost:8087
```

`JWT_ISSUER` here must match the auth-service `JWT_ISSUER`, otherwise every authenticated call
fails validation with a 401. Outgoing mail (low-score alerts and weekly reports) is captured by
MailHog at `http://localhost:8025`. To run everything in containers instead:
`docker compose up -d --build`.

### End-to-end flow

```bash
# register a student
curl -X POST localhost:8086/cadastro -H 'Content-Type: application/json' \
  -d '{"name":"Alice","login":"alice","password":"Password@1234","role":"STUDENT"}'

# get a token from the auth-service
TOKEN=$(curl -s -X POST localhost:8085/auth/login -H 'Content-Type: application/json' \
  -d '{"login":"alice","password":"Password@1234"}' | jq -r .token)

# register a course (ADMIN token required)
curl -X POST localhost:8086/courses -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Arquitetura de Software","description":"Padroes e estilos arquiteturais"}'

# any authenticated caller can list the courses to pick a courseId
COURSE_ID=$(curl -s localhost:8086/courses -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')

# submit a critical feedback: score 1 triggers the alert e-mail
curl -X POST localhost:8086/feedback -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"description\":\"Too fast\",\"score\":1,\"courseId\":\"$COURSE_ID\"}"

# list my feedback, then generate the report
curl localhost:8086/feedback -H "Authorization: Bearer $TOKEN"
curl -X POST localhost:8087/reports/weekly/run
```

## Configuration

Every property has a local default and an environment variable override, so the same build runs
from the IDE, from docker-compose and in the cloud.

| Variable | Default | Used by |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | local PostgreSQL | feedback-service, report-service |
| `AUTH_SERVICE_URL` | `http://localhost:8085` | feedback-service (registration) |
| `AUTH_JWKS_URL` | `http://localhost:8085/.well-known/jwks.json` | feedback-service (token validation) |
| `JWT_ISSUER` | `auth-service` | feedback-service; must match the auth-service value |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | Gmail SMTP | all three modules |
| `ADMIN_EMAIL` | `rate.me.fiap@gmail.com` | low-score alert and `notifyCritical` recipient |
| `REPORT_EMAIL` | `rate.me.fiap@gmail.com` | weekly report recipient |
| `ALERT_ADMIN_EMAIL` | falls back to `ADMIN_EMAIL` | `scanFeedbackErrors` digest recipient |
| `CRON_SCHEDULE` | app setting | `generateWeeklyReport` schedule, read by the Functions host |
| `LOG_SCAN_SCHEDULE` | app setting (`0 */5 * * * *` in the image) | `scanFeedbackErrors` schedule |
| `LOG_SCAN_ENABLED`, `LOG_SCAN_LAG_SECONDS` | `true`, `120` | `scanFeedbackErrors` behaviour |
| `FEEDBACK_INSIGHTS_APP_ID`, `FEEDBACK_INSIGHTS_API_KEY` | empty | Application Insights query API credentials |

See `.env.example` for a ready-to-copy local set.

## Tests

```bash
mvn -B verify
```

Runs the unit suites of all three modules and the JaCoCo gate, which fails the build when a module
drops below 85% line coverage. HTML report per module in `target/site/jacoco/index.html`. The
suites are plain JUnit + Mockito: because every external system sits behind a port, none of them
needs a container.

## CI/CD

| Workflow | Trigger | What it does |
|---|---|---|
| `ci.yml` | push/PR to `main` and `develop` | `mvn -B verify`, coverage gate, coverage table on the run summary, reports as artifacts |
| `cd.yml` | push to `main` | matrix build of the three images, pushed to Docker Hub |
| `report-service-cd.yml` | push to `main` touching `report-service/` | builds the image, pushes `:<commit>` and `:latest` to ACR, restarts the Function App |
| `notification-function-cd.yml` | push to `main` touching `notification-function/` | same flow for the notification Function App |

## Cloud deployment

Everything lives in the resource group `rg-dev-servless-FIAP`.

| Component | Azure resource | Image |
|---|---|---|
| auth-service | Container App `auth-service-fiap` | `ratemeacr.azurecr.io/auth-service:observability` |
| feedback-service | Container App `feedback-service-fiap` | `ratemeacr.azurecr.io/feedback-service:courses-917dc2c` |
| report-service | Function App `rate-me-report-service` | `ratemeacr.azurecr.io/report-service:latest` |
| notification-function | Function App `rate-me-notification-function` | `ratemeacr.azurecr.io/notification-function:latest` |
| database | PostgreSQL Flexible Server `tc-database` | — |

Both Function Apps share the `rate-me-dedicated-plan` (B1). A custom container on Azure Functions
requires a Premium or Dedicated plan — Flex Consumption does not support bring-your-own-container —
and reusing one plan for both keeps the extra cost at zero.

### Monitoring

Each service has its own Application Insights component, all reporting into the shared Log
Analytics workspace `workspacergdevservlessfiapb71d`. Instrumentation is the Application Insights
Java agent 3.7.8, attached without touching business code: baked into the image with `-javaagent`
for the Container Apps, through the `JAVA_OPTS` app setting on the containerized notification
Function App, and through `APPLICATIONINSIGHTS_ENABLE_AGENT` on the report Function App.
`APPLICATIONINSIGHTS_ROLE_NAME` is what separates the services in the portal. On top of that,
application logs carry `correlationId`, `authId` and `role` in the MDC, and `scanFeedbackErrors`
turns ERROR-level telemetry into an e-mail to the administrators.

## Team

Igor Costa · Leandro Fita · Thiago Soares · Victor Reis · Rodrigo Ferreira
