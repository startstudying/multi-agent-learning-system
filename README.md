# AI Learning OS

Personalized learning platform built as a modular monolith: Course RAG, learner profile extraction, learning paths, multi-agent resource generation, critic review, assessment feedback, mastery updates, and traceable replanning.

## Current Scope

- `backend/`: Java 21 + Spring Boot 3.5 backend with MySQL 8 Flyway migrations.
- `frontend/`: Vue 3 + TypeScript + Vite workbench with route-based student, teacher, and admin pages.
- `docs/`: architecture, RAG design, API contracts, deployment, and seed data notes.

## Local Backend

Prerequisites:

- Java 21
- Maven 3.9+
- Docker Desktop or Docker Compose v2

```powershell
cd backend
docker compose up -d
mvn spring-boot:run
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
mvn test
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
