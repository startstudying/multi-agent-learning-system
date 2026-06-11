# TASK-20260608 P3-4-H RAG Document Course/Chapter Metadata Scope

Status: Done

## Done Criteria

- [x] Spec-First 文档已创建。
- [x] Backend/RAG 与 Security 专家报告已集成。
- [x] `POST /api/knowledge-bases/{kbId}/documents` 对非空 `courseId` 执行 course manage scope。
- [x] teacher 只能上传自己课程的 course metadata。
- [x] student / ordinary user 带 `courseId` 上传返回 `FORBIDDEN`。
- [x] teacher missing/foreign course 返回 `FORBIDDEN` 且无 `data`。
- [x] admin missing course 返回 `NOT_FOUND`。
- [x] `chapterId` 非空时必须提供 `courseId`。
- [x] missing/foreign chapter 返回通用 `VALIDATION_ERROR`。
- [x] 失败请求不创建 `kb_document` / `kb_index_task`。
- [x] 无 course metadata 的上传和 requestId replay 兼容。
- [x] 不新增依赖、不新增 migration、不改 frontend。
- [x] Evidence / Acceptance / Memory / Changelog / TODO / Retrospective / Skill 更新。

## Allowed Files

- `backend/src/main/java/com/learningos/rag/application/DocumentService.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- `docs/product/PRD-20260608-rag-document-course-scope.md`
- `docs/requirements/REQ-20260608-rag-document-course-scope.md`
- `docs/specs/SPEC-20260608-rag-document-course-scope.md`
- `docs/plans/PLAN-20260608-rag-document-course-scope.md`
- `docs/tasks/TASK-20260608-rag-document-course-scope.md`
- `docs/context/CONTEXT-20260608-rag-document-course-scope.md`
- `docs/subagents/runs/RUN-20260608-rag-document-course-scope-backend.md`
- `docs/subagents/runs/RUN-20260608-rag-document-course-scope-security.md`
- `docs/subagents/runs/RUN-20260608-rag-document-course-scope-integration.md`
- `docs/evidence/EVIDENCE-20260608-rag-document-course-scope.md`
- `docs/acceptance/ACCEPT-20260608-rag-document-course-scope.md`
- `docs/retrospectives/RETRO-20260608-rag-document-course-scope.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/SKILL_REGISTRY.md`

## Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- model provider / parser / vector adapter implementation files
- unrelated assessment / analytics / orchestrator files

## Test Commands

```powershell
cd backend
mvn --% -Dtest=DocumentControllerTest test
mvn --% -Dtest=DocumentControllerTest,CourseKnowledgeControllerTest,RagQueryServiceTest,IndexServiceTest test
mvn test
```
