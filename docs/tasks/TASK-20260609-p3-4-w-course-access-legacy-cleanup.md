# TASK - P3-4-W CourseAccessService legacy overload cleanup

## 1. Traceability

- PLAN: `docs/plans/PLAN-20260609-p3-4-w-course-access-legacy-cleanup.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-w-course-access-legacy-cleanup.md`

## 2. Goal

删除 `CourseAccessService` 中 subject-name role inference legacy API 面，确保课程授权公共入口只接受 explicit role facts。

## 3. Files Allowed To Modify

- `backend/src/test/java/com/learningos/knowledge/application/CourseAccessServiceTest.java`
- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`
- `docs/subagents/runs/RUN-20260609-p3-4-w-course-access-legacy-cleanup-*.md`
- `docs/product/PRD-20260609-p3-4-w-course-access-legacy-cleanup.md`
- `docs/requirements/REQ-20260609-p3-4-w-course-access-legacy-cleanup.md`
- `docs/specs/SPEC-20260609-p3-4-w-course-access-legacy-cleanup.md`
- `docs/plans/PLAN-20260609-p3-4-w-course-access-legacy-cleanup.md`
- `docs/tasks/TASK-20260609-p3-4-w-course-access-legacy-cleanup.md`
- `docs/context/CONTEXT-20260609-p3-4-w-course-access-legacy-cleanup.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-w-course-access-legacy-cleanup.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-w-course-access-legacy-cleanup.md`
- `docs/retrospectives/RETRO-20260609-p3-4-w-course-access-legacy-cleanup.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 4. Files Not Allowed To Modify

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- `CurrentUserService` / `DevAuthFilter`
- formal OAuth2/JWK/Spring Security config
- Agent/RAG/model provider runtime
- other service legacy cleanup outside `CourseAccessService`

## 5. Implementation Steps

1. 新增 `CourseAccessServiceTest`，用 reflection 锁定 legacy overload/helper 不存在，并补 roles-first 行为回归。
2. 运行 focused RED，确认测试因旧 overload/helper 存在而失败。
3. 删除 `CourseAccessService` 4 个 legacy public overload。
4. 删除 `scopedCourseMissing(String)`、`isAdmin(String)`、`isTeacherUser(String)`。
5. 运行 `rg` 和 compile guard 证明源码调用点均为 roles-first。
6. 运行 focused、adjacent、full Maven tests。
7. 写 Evidence / Acceptance / Retro，更新 Changelog / Memory / TODO。

## 6. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseAccessServiceTest test
mvn --% -DskipTests compile
mvn --% -Dtest=CourseAccessServiceTest,CourseKnowledgeControllerTest,AnalyticsControllerTest,AssessmentControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest,DocumentControllerTest test
mvn test
```

## 7. Done Criteria

- [x] PRD/REQ/SPEC/PLAN/TASK/CONTEXT 存在。
- [x] RED 失败已观察并记录。
- [x] `CourseAccessService` legacy public overload 已删除。
- [x] `CourseAccessService` subject-name helper 已删除。
- [x] `rg` 证明所有 `courseAccessService.*` 调用都是 roles-first。
- [x] compile guard 通过。
- [x] focused/adjacent/full tests 已运行或限制已说明。
- [x] Evidence / Acceptance / Retro 已创建。
- [x] Changelog / Memory / TODO 已更新。

## 8. Status

Done。
