# Deployment And Local Operations

This guide describes the current local and container deployment path for the AI Learning OS backend. The repository currently uses installed Maven; no Maven wrapper is present.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop or Docker Compose v2
- Node.js 20+ for frontend verification

## Environment Files

Backend runtime configuration is read from `backend/.env.example` and Spring environment variables:

- Database: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- Redis: `REDIS_HOST`, `REDIS_PORT`
- MinIO: `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET`
- AI model placeholders: `AI_MODEL_PROVIDER`, `AI_CHAT_MODEL`, `AI_EMBEDDING_MODEL`
- RAG limits: `RAG_DEFAULT_TOP_K`, `RAG_MAX_CONTEXT_TOKENS`, `RAG_RERANKER_TIMEOUT_MS`

For local work, copy `backend/.env.example` to `backend/.env` when you need non-default values.

## Local Dependencies

```powershell
cd backend
docker compose up -d
```

Services:

- MySQL 8 on `3306`
- Redis 7 on `6379`
- MinIO API on `9000`
- MinIO console on `9001`

Validate the Compose file before starting shared environments:

```powershell
cd backend
docker compose config
```

## Backend

```powershell
cd backend
mvn test
mvn spring-boot:run
```

Health check:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

The Phase 1 health response reports application, database, Redis, MinIO, and model configuration status without requiring Redis, MinIO, or an AI model to be live before startup.

## Frontend

```powershell
cd frontend
npm install
npm test -- --run
npm run build
npm run dev
```

## Docker Image

Build the backend image:

```powershell
cd backend
docker build -t learning-os-backend .
```

Run it against the local Compose dependencies:

```powershell
docker run --rm -p 8080:8080 --env-file .env learning-os-backend
```

When the container runs outside the Compose network, use host-reachable endpoints such as `DB_URL=jdbc:mysql://host.docker.internal:3306/learning_os?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` and `MINIO_ENDPOINT=http://host.docker.internal:9000`.

## Release Checklist

Before tagging or deploying a backend build:

1. Run `mvn test` from `backend`.
2. Run `docker compose config` from `backend`.
3. Build the Docker image with `docker build -t learning-os-backend .`.
4. Start the backend and verify `GET /api/health`.
5. Review `docs/architecture/observability.md` for trace and health surfaces.
6. Review `docs/data/seed-data.md` before enabling any profile-scoped demo data.
