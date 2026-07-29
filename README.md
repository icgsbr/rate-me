# rate-me — plataforma de feedback

Alunos avaliam as aulas que assistem, administradores são avisados quando algo vai mal e recebem
um relatório periódico com os números. Construído para o Tech Challenge da Fase 4 (FIAP/POSTECH)
com **Java 21 + Quarkus 3.15**, arquitetura hexagonal, **PostgreSQL + Flyway**, **JWT** RS256
validado contra um JWKS remoto e duas **Azure Functions** para tudo que é disparado por tempo em
vez de por um usuário.

## Arquitetura em um relance

```
             ┌──────────────────┐        ┌──────────────────────────┐
 aluno   ───▶│ feedback-service │───────▶│  PostgreSQL (feedback)   │
 admin   ───▶│  Container App   │        └──────────────────────────┘
             └────────┬─────────┘                     ▲ somente leitura
                      │ nota <= 1                     │
                      ▼ e-mail                ┌───────┴──────────┐
                 administrador ◀─── e-mail ───│  report-service  │ timer
                      ▲                       │   Function App   │
                      │ e-mail                └──────────────────┘
             ┌────────┴───────────────┐
             │ notification-function  │ timer + HTTP
             │      Function App      │◀── consulta o Application Insights
             └────────────────────────┘
```

Os módulos seguem o mesmo corte interno: `domain` guarda modelos puros, sem framework;
`application` guarda os casos de uso e as portas de entrada e saída; `adapter` guarda tudo que
conversa com o mundo externo (recursos REST, gatilhos de função, JPA, SMTP, o cliente REST do
auth-service, o cliente de consulta do Application Insights). As dependências só apontam para
dentro — é por isso que o notificador SMTP ou o armazenamento do relatório podem ser trocados sem
que os casos de uso percebam, e por isso a suíte de testes inteira roda sem banco e sem servidor
de e-mail.

A autenticação é delegada a um **auth-service** externo (Spring Boot, repositório
`cheffy-microservices`, porta 8085 localmente). Ele cadastra usuários, emite tokens RS256 e
publica um endpoint JWKS contra o qual os demais serviços validam.

## Módulos

| Módulo | Roda como | Responsabilidade |
|---|---|---|
| `feedback-service` | Container App (porta 8086) | cadastro, catálogo de cursos, submissão e listagem de avaliações, alerta de nota baixa |
| `report-service` | Function App (Timer Trigger) | métricas do período sobre as avaliações, armazenadas e enviadas por e-mail |
| `notification-function` | Function App (HTTP + Timer Trigger) | escala um evento crítico por e-mail; varre os logs de erro do feedback-service |

## Endpoints

### feedback-service

| Método | Rota | Quem | Descrição |
|---|---|---|---|
| `POST` | `/cadastro` | público | cadastra um aluno ou administrador: chama o auth-service e guarda o `auth_id` retornado |
| `POST` | `/courses` | ADMIN | cadastra um curso a partir de `{name, description}` |
| `GET` | `/courses` | qualquer JWT válido | lista os cursos; é daqui que sai o `courseId` abaixo |
| `POST` | `/feedback` | STUDENT | envia `{description, score, courseId}`; curso inexistente resulta em 404, `score <= 1` dispara o alerta ao administrador e marca `notified` |
| `GET` | `/feedback` | STUDENT | lista as avaliações do próprio chamador |
| `GET` | `/feedback?page=&size=` | ADMIN | lista todas as avaliações, paginadas (padrões `page=0`, `size=20`) |

O login é feito diretamente no auth-service (`POST /auth/login`). O token dele não carrega
reivindicação de papel, então o papel é resolvido localmente a partir da reivindicação `authId`:
a tabela de administradores é consultada primeiro, depois a de alunos. Swagger UI:
`http://localhost:8086/q/swagger-ui`.

### report-service

`generateWeeklyReport` é um Timer Trigger acionado pela configuração `CRON_SCHEDULE` da Function
App. Ele monta o relatório dos últimos 7 dias, armazena e envia para `REPORT_EMAIL`. O mesmo caso
de uso fica exposto por HTTP para inspeção quando o serviço roda como aplicação web:

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/reports/weekly/run` | monta o relatório, armazena, envia por e-mail e devolve em JSON |
| `GET` | `/reports/weekly` | devolve o mesmo relatório em JSON, sem armazenar nem enviar |

O relatório traz avaliações por dia, total de avaliações, quantidade de notas críticas com os
identificadores dos alunos correspondentes, média de avaliações por semana e média das notas no
período.

### notification-function

| Função | Gatilho | Autorização | Descrição |
|---|---|---|---|
| `notifyCritical` | `POST /api/notifications/critical` | chave de função | envia um evento crítico por e-mail aos administradores |
| `health` | `GET /api/health` | anônimo | sonda de disponibilidade |
| `scanFeedbackErrors` | Timer (`LOG_SCAN_SCHEDULE`) | — | consulta o Application Insights do feedback-service por registros de erro e envia um resumo por e-mail |

O payload do alerta carrega exatamente os três campos que o desafio pede:

```json
{ "descricao": "Database unreachable", "urgencia": "CRITICA", "dataEnvio": "2026-07-24T00:30:00Z" }
```

`urgencia` aceita `BAIXA`, `MEDIA`, `ALTA` ou `CRITICA`; `dataEnvio` é opcional e assume o
instante do recebimento quando ausente. A chave vai no cabeçalho `x-functions-key` (ou no
parâmetro `?code=`).

O `scanFeedbackErrors` examina a janela `(execução anterior − atraso, agora − atraso]`, sendo o
atraso a folga para a latência de ingestão do Application Insights. Ele não guarda estado próprio:
o host das Functions persiste a situação do agendamento na conta de armazenamento.

## Modelo de dados

Pertence ao `feedback-service` e é versionado com Flyway
(`quarkus.flyway.migrate-at-start=true`); o `report-service` lê o mesmo banco e nunca escreve DDL.

| Tabela | Colunas |
|---|---|
| `student` | `id` (UUID PK), `name`, `registration_number`, `auth_id` (indexado) |
| `admin` | `id` (UUID PK), `name`, `auth_id` (indexado), `role` |
| `course` | `id` (UUID PK), `name`, `description` |
| `feedback` | `id` (identity PK), `student_id` → `student`, `course_id` → `course`, `score` (0–10, com check), `review_description`, `review_date`, `notified`, `notified_date` |

O `V1__init_schema.sql` cria as quatro tabelas. O
`V2__course_description_and_required_feedback_course.sql` acrescenta `course.description`,
preenche as linhas antigas com o curso semeado e torna `feedback.course_id` obrigatório.

## Executando localmente

Pré-requisitos: Java 21, Maven, Docker.

```bash
# 1. dependências: PostgreSQL + MailHog
docker compose up -d postgres mailhog

# 2. auth-service (repositório separado) na porta 8085, com as chaves RSA e JWT_ISSUER=cheffy-auth
#    cd ../cheffy-microservices/auth-service && mvn spring-boot:run

# 3. feedback-service (aplica as migrações do Flyway na inicialização)
cd feedback-service && mvn quarkus:dev      # http://localhost:8086

# 4. report-service
cd report-service && mvn quarkus:dev        # http://localhost:8087
```

O `JWT_ISSUER` daqui precisa ser idêntico ao do auth-service; se divergir, toda chamada
autenticada falha na validação com 401. Os e-mails de saída (alertas de nota baixa e relatórios)
são capturados pelo MailHog em `http://localhost:8025`. Para rodar tudo em contêiner:
`docker compose up -d --build`.

### Fluxo de ponta a ponta

```bash
# cadastra um aluno
curl -X POST localhost:8086/cadastro -H 'Content-Type: application/json' \
  -d '{"name":"Alice","login":"alice","password":"Password@1234","role":"STUDENT"}'

# obtém um token no auth-service
TOKEN=$(curl -s -X POST localhost:8085/auth/login -H 'Content-Type: application/json' \
  -d '{"login":"alice","password":"Password@1234"}' | jq -r .token)

# cadastra um curso (exige token de ADMIN)
curl -X POST localhost:8086/courses -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Arquitetura de Software","description":"Padroes e estilos arquiteturais"}'

# qualquer usuário autenticado pode listar os cursos para escolher um courseId
COURSE_ID=$(curl -s localhost:8086/courses -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')

# envia uma avaliação crítica: nota 1 dispara o e-mail de alerta
curl -X POST localhost:8086/feedback -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"description\":\"Too fast\",\"score\":1,\"courseId\":\"$COURSE_ID\"}"

# lista as próprias avaliações e gera o relatório
curl localhost:8086/feedback -H "Authorization: Bearer $TOKEN"
curl -X POST localhost:8087/reports/weekly/run
```

## Configuração

Toda propriedade tem um valor padrão local e um ponto de sobrescrita por variável de ambiente, de
modo que o mesmo build roda pela IDE, pelo docker-compose e na nuvem.

| Variável | Padrão | Usada por |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL local | feedback-service, report-service |
| `AUTH_SERVICE_URL` | `http://localhost:8085` | feedback-service (cadastro) |
| `AUTH_JWKS_URL` | `http://localhost:8085/.well-known/jwks.json` | feedback-service (validação do token) |
| `JWT_ISSUER` | `auth-service` | feedback-service; precisa ser igual ao valor do auth-service |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP do Gmail | os três módulos |
| `ADMIN_EMAIL` | `rate.me.fiap@gmail.com` | destinatário do alerta de nota baixa e do `notifyCritical` |
| `REPORT_EMAIL` | `rate.me.fiap@gmail.com` | destinatário do relatório periódico |
| `ALERT_ADMIN_EMAIL` | herda de `ADMIN_EMAIL` | destinatário do resumo do `scanFeedbackErrors` |
| `CRON_SCHEDULE` | configuração da Function App | agendamento do `generateWeeklyReport`, lido pelo host das Functions |
| `LOG_SCAN_SCHEDULE` | configuração da Function App (`0 */5 * * * *` na imagem) | agendamento do `scanFeedbackErrors` |
| `LOG_SCAN_ENABLED`, `LOG_SCAN_LAG_SECONDS` | `true`, `120` | comportamento do `scanFeedbackErrors` |
| `FEEDBACK_INSIGHTS_APP_ID`, `FEEDBACK_INSIGHTS_API_KEY` | vazio | credenciais da API de consulta do Application Insights |

O arquivo `.env.example` traz um conjunto local pronto para copiar.

## Testes

```bash
mvn -B verify
```

Roda as suítes unitárias dos três módulos e o portão do JaCoCo, que falha o build quando um módulo
cai abaixo de 85% de cobertura de linhas. O relatório em HTML de cada módulo fica em
`target/site/jacoco/index.html`. As suítes são JUnit + Mockito puros: como todo sistema externo
está atrás de uma porta, nenhuma delas precisa de contêiner.

## CI/CD

| Workflow | Disparo | O que faz |
|---|---|---|
| `ci.yml` | push e PR em `main` e `develop` | `mvn -B verify`, portão de cobertura, tabela de cobertura no resumo da execução e relatórios como artefato |
| `cd.yml` | push em `main` | build em matriz das três imagens, publicadas no Docker Hub |
| `report-service-cd.yml` | push em `main` tocando `report-service/` | constrói a imagem, publica `:<commit>` e `:latest` no ACR e reinicia a Function App |
| `notification-function-cd.yml` | push em `main` tocando `notification-function/` | mesmo fluxo para a Function App de notificação |

## Deploy na nuvem

Tudo vive no grupo de recursos `rg-dev-servless-FIAP`.

| Componente | Recurso Azure | Imagem |
|---|---|---|
| auth-service | Container App `auth-service-fiap` | `ratemeacr.azurecr.io/auth-service:observability` |
| feedback-service | Container App `feedback-service-fiap` | `ratemeacr.azurecr.io/feedback-service:courses-917dc2c` |
| report-service | Function App `rate-me-report-service` | `ratemeacr.azurecr.io/report-service:latest` |
| notification-function | Function App `rate-me-notification-function` | `ratemeacr.azurecr.io/notification-function:latest` |
| banco de dados | PostgreSQL Flexible Server `tc-database` | — |

As duas Function Apps compartilham o `rate-me-dedicated-plan` (B1). Contêiner customizado no Azure
Functions exige plano Premium ou Dedicated — o Flex Consumption não aceita imagem própria —, e
reaproveitar um único plano para as duas mantém o custo adicional em zero.

### Monitoramento

Cada serviço tem seu próprio componente de Application Insights, todos gravando no workspace de
Log Analytics compartilhado `workspacergdevservlessfiapb71d`. A instrumentação é o agente Java do
Application Insights 3.7.8, anexado sem tocar em código de negócio: embutido na imagem com
`-javaagent` nos Container Apps, pela configuração `JAVA_OPTS` na Function App de notificação em
contêiner, e por `APPLICATIONINSIGHTS_ENABLE_AGENT` na Function App do relatório. A variável
`APPLICATIONINSIGHTS_ROLE_NAME` é o que separa os serviços no portal. Além disso, os logs da
aplicação carregam `correlationId`, `authId` e `role` no MDC, e o `scanFeedbackErrors` transforma
telemetria de nível ERROR em e-mail para os administradores.

## Equipe

Igor Costa · Leandro Fita · Thiago Soares · Victor Reis · Rodrigo Ferreira
