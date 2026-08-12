# msj-backend

Spring Boot 4 REST API for the Machine Science journal. Java 17, JPA/Hibernate,
Flyway, JWT security, PostgreSQL (Neon in dev).

## Run

Build tool is **Maven** (via the `./mvnw` wrapper — no global Maven needed).

```bash
cp .env.example .env        # DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET, MSJ_ADMIN_*
./run-dev.sh                # = source .env && ./mvnw spring-boot:run
# or:
./mvnw -DskipTests package && java -jar target/msj-backend.jar
```

- API base: `/api/v1`
- Swagger UI: `/swagger-ui.html` · OpenAPI JSON: `/v3/api-docs`
- Health: `/actuator/health`

`./mvnw -DskipTests package` builds the runnable jar at `target/msj-backend.jar`
(tests are trivial by default; wire a Testcontainers Postgres in CI to run the
real context-load test).

### Docker

```bash
docker build -t msj-backend .          # multi-stage: Maven build → JRE runtime
# or from the platform root: docker compose up -d --build backend
```

## Layout

```
src/main/java/az/edu/aztu/msj/
├── config/     AppProperties, OpenApiConfig, AdminBootstrap
├── common/     ApiException, GlobalExceptionHandler, ApiError, PageResponse
├── security/   JwtService, JwtAuthenticationFilter, SecurityConfig, AppUserDetails(Service)
├── auth/       AuthController/Service, RefreshToken, DTOs
├── user/       User, UserRepository
├── board/      BoardMember (+ public controller)
├── issue/      Issue (+ public controller with table-of-contents)
├── article/    Article, ArticleAuthor, ArticleFile*, ArticleMetric, ArticleStatusHistory, service, public controller
├── metric/     ArticleEvent, daily rollup, MetricService (dedup + counters), public recording controller
├── content/    JournalSettings, ContentPage, Announcement (JSONB) + public controller
├── contact/    ContactMessage + controller
└── admin/      AdminController/Service (dashboard, submissions, status workflow, metrics overview)
```

Schema is owned by Flyway (`src/main/resources/db/migration/`); Hibernate runs in
`validate` mode and never mutates the schema. Dev/demo data:
`src/main/resources/db/seed/dev_seed.sql` (apply manually to a dev DB).

## Key endpoints

| Method | Path                                | Auth        |
|--------|-------------------------------------|-------------|
| POST   | `/api/v1/auth/login` `/register` `/refresh` `/logout` | public / bearer |
| GET    | `/api/v1/auth/me`                   | bearer      |
| GET    | `/api/v1/board`                     | public      |
| GET    | `/api/v1/issues` · `/issues/{slug}` | public      |
| GET    | `/api/v1/articles` · `/articles/{id}` | public    |
| GET    | `/api/v1/settings` · `/pages` · `/announcements` | public |
| POST   | `/api/v1/metrics/events`            | public (deduped) |
| POST   | `/api/v1/public/contact`            | public      |
| GET    | `/api/v1/admin/dashboard` · `/admin/metrics/overview` | ADMIN/EDITOR* |
| GET    | `/api/v1/admin/articles`            | ADMIN/EDITOR* |
| PATCH  | `/api/v1/admin/articles/{id}/status`| ADMIN/EDITOR* |

## Metrics design

`POST /metrics/events` hashes `ip|user-agent|day` into a session key. A partial
unique index (`ux_article_events_dedup`) makes the raw insert a no-op on repeat,
so a counter only moves on the **first** event per visitor/day. Verified:
two identical view posts → `view_count + 1`.

## Notes for production

- Switch `AuthService.register` to create `PENDING` users + email verification.
- Move file storage to S3 (config stub in `msj.storage`); add `/articles/{id}/pdf`.
- Add scheduled Crossref/Scopus citation-sync jobs writing to `citations`.
- Serve behind HTTPS; set a strong `JWT_SECRET`; restrict `CORS_ORIGINS`.
