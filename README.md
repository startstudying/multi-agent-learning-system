# AI Learning OS

Personalized learning platform built as a modular monolith: Course RAG, learner profile extraction, learning paths, multi-agent resource generation, critic review, assessment feedback, mastery updates, and traceable replanning.

## Current Scope

- `backend/`: Java 21 + Spring Boot 3.5 backend with MySQL 8 Flyway migrations.
- `frontend/`: Vue 3 + TypeScript + Vite workbench with route-based student, teacher, and admin pages.
- `docs/`: architecture, RAG design, API contracts, deployment, and seed data notes.

## Local Backend

Prerequisites:

- Java 21 (install from [Adoptium](https://adoptium.net/) or [Oracle JDK](https://jdk.java.net/21/))
- Docker Desktop or Docker Compose v2
- (Optional) Maven 3.9+ — if not installed, use the included `mvnw` wrapper instead of `mvn`

```powershell
cd backend
docker compose up -d
./mvnw spring-boot:run
```

Health check:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

## Local Frontend

```powershell
cd frontend
npm install
npm run dev
```

## Verification

Backend:

```powershell
cd backend
./mvnw test
docker compose config
```

Frontend:

```powershell
cd frontend
npm test -- --run
npm run build
```

## Implemented Workflow

1. Create/list knowledge bases.
2. Upload documents and create pending index tasks.
3. Query RAG and return citations plus trace ids.
4. Stream chat status/token/done events through SSE.
5. Extract learner profile drafts.
6. Generate traceable learning paths.
7. Create resource-generation tasks with pending critic review.
8. Inspect Agent trace timelines.
9. Approve or request revisions from the teacher review queue.
10. Submit assessment answers, update mastery, and trigger replanning records.
11. Inspect admin health and analytics overview.
12. Use the Vue workbench to demonstrate the full learning loop.

## Runtime Defaults

- Primary DB: MySQL 8 on `3306`.
- Optional services: Redis 7, MinIO, and later VectorDB adapters.
- Frontend API base: `VITE_API_BASE_URL`, defaulting to `http://localhost:8080`.
- Dev user header: `X-User-Id`, defaulting to `stu_001` in the frontend client.

## Troubleshooting

### Chinese text displays as garbled (mojibake)

All docs are UTF-8 encoded. If Chinese characters appear garbled:
- In PowerShell: `chcp 65001` before reading files
- In VS Code / IntelliJ: ensure the file is opened with UTF-8 encoding
- In Git Bash / WSL: UTF-8 should be handled automatically

### Java / Maven not found

Set `JAVA_HOME` to your JDK 21 installation and use the included `./mvnw` wrapper:

```powershell
setx JAVA_HOME "C:\Program Files\Java\jdk-21"
# Restart terminal, then:
./mvnw spring-boot:run
```

### RAG query returns 403 "No accessible knowledge bases"

The demo seed data migration (`V24__demo_seed_data.sql`) creates sample users, roles, and a `kb_java_backend` knowledge base. Ensure Flyway has run all migrations and database services are up via `docker compose up -d`.

## More Docs

- `docs/INDEX.md` — documentation map
- `docs/api/reference.md`
- `docs/api/contract.md`
- `docs/operations/deployment.md`
- `docs/architecture/overview.md`
- `docs/architecture/rag-architecture.md`
- `docs/architecture/observability.md`
- `docs/data/model.md`
- `docs/data/seed-data.md`
- `docs/planning/system-design-and-development-plan.md`
