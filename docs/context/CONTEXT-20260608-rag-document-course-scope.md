# CONTEXT-20260608 P3-4-H RAG Document Course/Chapter Metadata Scope

## 1. Current Task Boundary

本切片只收口 RAG document upload 中 `courseId/chapterId` 元数据写入权限。目标是防止有 KB 写权限的用户把文档标记到无权课程或错误章节。

## 2. Related Memory And Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/specs/SPEC-20260608-rag-document-course-scope.md`
- `docs/specs/SPEC-20260608-course-enrollment-scope.md`
- `docs/specs/SPEC-20260608-rbac-course-class-answer-matrix.md`
- `docs/specs/SPEC-20260606-rag-document-upload-idempotency.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

## 3. Selected Skills

- `feature-development-workflow`
- `object-scope-authorization`
- `rag-project-review`
- `security-review`
- `spring-boot-architecture`
- `test-driven-development`
- `verification-before-completion`
- `Confidence Check`

## 4. Subagent Plan

| Subagent | Scope | Output |
|---|---|---|
| Backend/RAG Expert | 当前 upload/idempotency/index 链路和最小实现 | `RUN-20260608-rag-document-course-scope-backend.md` |
| Security & Quality | 权限矩阵、IDOR、错误语义和测试矩阵 | `RUN-20260608-rag-document-course-scope-security.md` |
| Integration Reviewer | Main Codex 合并输出 | `RUN-20260608-rag-document-course-scope-integration.md` |

## 5. Allowed Files

- `backend/src/main/java/com/learningos/rag/application/DocumentService.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- 本切片同名 workflow 文档、evidence、acceptance、memory、changelog、skill。

## 6. Files Not Allowed

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- unrelated Agent/model/parser/vector/assessment files

## 7. Test Commands

```powershell
cd backend
mvn --% -Dtest=DocumentControllerTest test
mvn --% -Dtest=DocumentControllerTest,CourseKnowledgeControllerTest,RagQueryServiceTest,IndexServiceTest test
mvn test
```

## 8. Architecture Drift Checklist

| Check | Expected |
|---|---|
| Controller only delegates | PASS |
| Service contains authorization | PASS |
| No new dependency | PASS |
| No schema drift | PASS |
| No frontend LLM/API key change | PASS |
| RAG permission filtering not weakened | PASS |
