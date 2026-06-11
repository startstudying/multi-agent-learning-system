# CONTEXT - P3-4 KB-course binding schema and lifecycle governance

## Related memory/docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/specs/SPEC-20260609-p3-4-p-rag-kb-rbac.md`
- `docs/specs/SPEC-20260608-rag-document-course-scope.md`
- `docs/specs/SPEC-20260610-p3-4-rag-query-runtime-roles-first-rbac.md`

## Subagent reports

- `docs/subagents/runs/RUN-20260610-p3-4-kb-course-binding-governance-architect.md`
- `docs/subagents/runs/RUN-20260610-p3-4-kb-course-binding-governance-security.md`
- `docs/subagents/runs/RUN-20260610-p3-4-kb-course-binding-governance-test.md`
- `docs/subagents/runs/RUN-20260610-p3-4-kb-course-binding-governance-integration-review.md`
- `docs/subagents/runs/RUN-20260610-p3-4-kb-course-binding-governance-security-final.md`
- `docs/subagents/runs/RUN-20260610-p3-4-kb-course-binding-governance-test-final.md`
- `docs/subagents/runs/RUN-20260610-p3-4-kb-course-binding-governance-verification-final.md`
- `docs/subagents/runs/RUN-20260610-p3-4-kb-course-binding-governance-documentation-consistency-final.md`

## Allowed files

Production:

- `backend/src/main/resources/db/migration/V20__kb_course_binding_governance.sql`
- `backend/src/main/java/com/learningos/rag/domain/KnowledgeBase.java`
- `backend/src/main/java/com/learningos/rag/domain/enums/KnowledgeBaseBindingStatus.java`
- `backend/src/main/java/com/learningos/rag/api/dto/KnowledgeBaseDtos.java`
- `backend/src/main/java/com/learningos/rag/api/dto/DocumentDtos.java`
- `backend/src/main/java/com/learningos/rag/application/KnowledgeBaseService.java`
- `backend/src/main/java/com/learningos/rag/application/PermissionService.java`
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java`
- `backend/src/main/java/com/learningos/rag/repository/KbDocumentRepository.java`

Tests:

- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java`
- `backend/src/test/java/com/learningos/rag/api/KnowledgeBaseControllerTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- `backend/src/test/java/com/learningos/rag/application/PermissionServiceTest.java`
- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`

Docs:

- `docs/evidence/EVIDENCE-20260610-p3-4-kb-course-binding-governance.md`
- `docs/acceptance/ACCEPT-20260610-p3-4-kb-course-binding-governance.md`
- `docs/retrospectives/RETRO-20260610-p3-4-kb-course-binding-governance.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## Disallowed files

- `.env*`
- secrets / credentials
- frontend files
- parser/OCR/vector/reranker/model provider implementation
- unrelated P3-4 answer/class matrix files

## Boundary

Implement one task only: KB-course binding governance. Do not mark P3-4 parent complete unless all remaining parent TODOs are closed.

## Final semantics

- `BOUND` KB read/write 必须走 `CourseAccessService`，`PUBLIC`、owner、explicit permission 不绕过课程访问。
- 空 `UNBOUND` KB 首次合法课程文档上传自动绑定为 `BOUND`；已有 active document 的 `UNBOUND` KB 不允许再上传 course metadata。
- `DocumentService` 对 `UNBOUND` 自动绑定前使用 KB 行级 `PESSIMISTIC_WRITE`，并在锁后再次检查 requestId replay。
- 同一 `createdBy + requestId` 不同 payload 返回 `409 CONFLICT`，优先于 KB-course mismatch。
- `CONFLICTED` KB 非 admin 不可读写。
