# Test Commands

## Backend

```bash
# Full test suite
cd backend && mvn test

# Single test class
cd backend && mvn test -Dtest=ClassNameTest

# Compile only
cd backend && mvn compile -q

# MySQL Flyway smoke, opt-in and local Docker-backed
powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1

# MySQL Flyway smoke when local 3306 is occupied
$env:MYSQL_PORT='3307'
powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1 -JdbcUrl 'jdbc:mysql://127.0.0.1:3307/learning_os_migration_smoke?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
```

## Frontend

```bash
# Type check + build
cd frontend && pnpm build

# Lint (if configured)
cd frontend && pnpm lint

# Unit tests (if configured)
cd frontend && pnpm test
```

## Integration

```bash
# Start backend (dev profile)
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Start frontend dev server
cd frontend && pnpm dev
```

## Pre-Commit Checklist

1. `cd backend && mvn test` — all tests pass
2. `cd frontend && pnpm build` — build succeeds
3. Architecture drift check — no violations
4. No secrets in diff

## When Tests Cannot Run

Document in Evidence:

- Which command failed and why
- Environment limitation (missing DB, missing API key in CI, etc.)
- Manual verification steps performed instead
