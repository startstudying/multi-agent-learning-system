# TASK-20260609 P3-4 子任务：Analytics teacherClassSummary legacy subject-name cleanup

## 目标

收口 `AnalyticsService.teacherClassSummary` 的服务层 legacy subject-name role 判断，避免 class analytics 在服务层通过 `currentUserId = "admin"`、`teacher` 或 `teacher_*` 推断 admin/teacher 权限。

## Task Type

Refactor / security cleanup。

## Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 继续推进 backend TODO 父计划。 |
| `multi-agent-coder` | 用户要求专家 subagent 并行开发。 |
| `test-driven-development` | 删除 legacy overload/fallback 需要 RED/GREEN。 |
| `security-review` | 本任务处理 subject-name role confusion。 |
| `auth-context-boundary` | 角色判断必须来自 trusted role facts。 |
| `object-scope-authorization` | class/course analytics 属于对象级课程范围授权。 |
| `verification-before-completion` | 完成前必须有新鲜测试证据。 |

Missing Skills: 无。

GitHub Research Needed: No。

New Project-Specific Skill To Create: 暂不需要。

## Size Classification

- Size: S - Small Slice / Fast Lane
- Reason: 单个 Service 的 legacy authorization surface cleanup；不改 REST API、DTO、schema、依赖、前端合同或正式认证架构。
- Required Documents: 本 mini TASK，内嵌 Context Pack；完成后使用合并 Evidence/Acceptance。
- Can Skip: 独立 PRD/REQ/SPEC/PLAN/CONTEXT、独立 Retro。
- Upgrade Trigger: 如果需要改 REST contract、class/enrollment 数据模型、Spring Security/OAuth2/JWK 或 broader class/course matrix，则升级为 M/L。

## Subagent Plan

用户明确要求专家 subagent 并行开发，本 S 切片启用 L1 并行分析：

| Expert | Scope | Expected Output |
|---|---|---|
| Architect | 判断是否删除两参 overload/helper/fallback | 架构影响和边界 |
| Security Reviewer | role-confusion 风险审查 | 风险评级和必须保护行为 |
| Test Engineer | TDD 最小测试设计 | RED/GREEN/adjacent 命令 |

主 Codex 负责实现、集成和验证。

## Context Pack

### Related Memory and Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/skills/project-specific/object-scope-authorization.md`

### Allowed Files

- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `backend/src/test/java/com/learningos/analytics/application/AnalyticsServiceTest.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`（仅验证需要时修改）
- `docs/tasks/TASK-20260609-p3-4-analytics-teacher-class-legacy-cleanup.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-analytics-teacher-class-legacy-cleanup.md`
- `docs/subagents/runs/RUN-20260609-p3-4-analytics-teacher-class-legacy-cleanup-*.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

### Disallowed Files

- `frontend/**`
- `backend/pom.xml`
- database migration files
- `AnalyticsController.java`
- `CourseAccessService.java`
- ResourceGeneration / Orchestrator / Agent Trace / Review Gate modules
- formal OAuth2/JWK/Spring Security configuration

## Planned Test Commands

RED:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsServiceTest#teacherClassSummaryLegacyOverloadIsRemoved+analyticsServiceLegacyTeacherHelperIsRemoved+rolesFirstTeacherClassSummaryDoesNotGrantAdminWhenOnlySubjectNameIsAdmin+rolesFirstTeacherClassSummaryDoesNotGrantTeacherWhenOnlySubjectOwnsCourse test
```

GREEN focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsServiceTest#teacherClassSummaryLegacyOverloadIsRemoved+analyticsServiceLegacyTeacherHelperIsRemoved+rolesFirstTeacherClassSummaryDoesNotGrantAdminWhenOnlySubjectNameIsAdmin+rolesFirstTeacherClassSummaryDoesNotGrantTeacherWhenOnlySubjectOwnsCourse test
```

Adjacent:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsServiceTest,AnalyticsControllerTest test
```

Full backend:

```powershell
cd D:\多元agent\backend
mvn test
```

## Acceptance Criteria

- [x] `AnalyticsService` no longer exposes public `teacherClassSummary(String, String)`.
- [x] `AnalyticsService` no longer contains `isLegacyTeacherUser(String)`.
- [x] `requireTeacherClassAccess(...)` no longer grants admin/teacher access from subject name when explicit role facts are false.
- [x] Roles-first `teacherClassSummary(String, String, boolean, boolean)` remains intact.
- [x] HTTP class summary behavior remains covered by existing controller tests.
- [x] No REST API contract, DTO, DB, dependency, frontend, `AnalyticsController`, `CourseAccessService`, or formal OAuth2/JWK/Spring Security change.
- [x] Parent P3-4 remains not fully complete unless broader class/course and formal OAuth2/JWK/Spring Security items are completed separately.

## Completion Evidence

- Evidence / Acceptance: `docs/evidence/EVIDENCE-20260609-p3-4-analytics-teacher-class-legacy-cleanup.md`
- Subagent reports:
  - `docs/subagents/runs/RUN-20260609-p3-4-analytics-teacher-class-legacy-cleanup-architect.md`
  - `docs/subagents/runs/RUN-20260609-p3-4-analytics-teacher-class-legacy-cleanup-security.md`
  - `docs/subagents/runs/RUN-20260609-p3-4-analytics-teacher-class-legacy-cleanup-test.md`
  - `docs/subagents/runs/RUN-20260609-p3-4-analytics-teacher-class-legacy-cleanup-integration-review.md`
- Verification: RED `4 run, 4 failures` plus membership RED `1 run, 1 failure`; focused `5/5`; compile guard success; adjacent `73/73`; full backend `482 run, 0 failures, 0 errors, 1 skipped`.
