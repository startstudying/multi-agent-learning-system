# Reference Priority

Use these references in this order when continuing implementation:

1. Current repository code and Flyway migrations.
2. User requirements: Vue 3 TypeScript frontend, Java 21 Spring Boot backend, Spring AI/Spring AI Alibaba integration path, MySQL 8 as the primary database, optional Redis/MinIO/VectorDB.
3. `docs/architecture/overview.md` for module boundaries and phase order.
4. `docs/architecture/rag-architecture.md` for Course RAG indexing/query rules.
5. `docs/api/contract.md` and `docs/api/reference.md` for frontend/backend contracts.
6. Paper and OSS references gathered during research, used for design justification and evaluation ideas rather than copied implementation.

The user-provided RAG layering diagram remains important for controller/service/repository boundaries and offline/online pipeline separation. Its PostgreSQL/PGVector example is superseded by this repo's MySQL-first runtime decision; vector storage is optional and adapter-based.

