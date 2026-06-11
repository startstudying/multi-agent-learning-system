# TASK-20260609 P3-4 子任务：LearningPath create legacy overload cleanup

## 目标

收口 `LearningWorkflowService` 中 LearningPath 创建路径的 legacy subject-name admin 判断，避免服务层公共入口继续通过 `currentUserId = "admin"` 推断管理员权限。

## Task Type

Refactor / security cleanup。

## Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 原始 backend TODO 计划推进，必须走项目工作流。 |
| `multi-agent-coder` | 用户明确要求专家 subagent 并行开发。 |
| `test-driven-development` | 删除 legacy overload 需要先写 RED 测试防回归。 |
| `security-review` | 本任务处理 subject-name role confusion 风险。 |
| `auth-context-boundary` | 角色判断必须来自可信 roles fact，不得来自 subject 字符串。 |
| `verification-before-completion` | 完成前必须有新鲜测试证据。 |

Missing Skills: 无。

GitHub Research Needed: No。该任务为项目内部 RBAC 边界清理，不需要外部参考。

New Project-Specific Skill To Create: 暂不需要；已有 `auth-context-boundary` 覆盖。

## Size Classification

- Size: S - Small Slice / Fast Lane
- Reason: 单个 Service 的 legacy overload/helper 清理；不改 REST API、DTO、schema、依赖、前端合同或 Agent/RAG runtime。
- Required Documents: 本 mini TASK，内嵌 Context Pack；完成后使用合并 Evidence/Acceptance。
- Can Skip: 独立 PRD/REQ/SPEC/PLAN/CONTEXT、独立 Retro。
- Upgrade Trigger: 如果需要改 Controller API、数据库、正式 Spring Security/OAuth2/JWK，或扩展 broader class/course 矩阵，则升级为 M/L。

## Subagent Plan

用户明确要求专家 subagent 并行开发，本 S 切片启用 L1 并行分析：

| Expert | Scope | Expected Output |
|---|---|---|
| Architect | 删除 legacy overload/helper 的架构影响 | 是否删除、保留行为、不触碰范围 |
| Security Reviewer | subject-name role confusion 风险 | 风险评级、必须保护的 RBAC 行为 |
| Test Engineer | TDD 最小测试设计 | RED/GREEN/focused/adjacent 命令 |

主 Codex 负责最终实现、集成和验证。

## Context Pack

### Related Memory and Docs

- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/tasks/TASK-20260609-p3-4-learning-path-create-legacy-overload-cleanup.md`

### Allowed Files

- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- `backend/src/test/java/com/learningos/learning/application/LearningWorkflowServiceTest.java`
- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`（仅验证需要时修改）
- `docs/tasks/TASK-20260609-p3-4-learning-path-create-legacy-overload-cleanup.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-learning-path-create-legacy-overload-cleanup.md`
- `docs/subagents/runs/RUN-20260609-p3-4-learning-path-create-legacy-overload-cleanup-*.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

### Disallowed Files

- `frontend/**`
- `backend/pom.xml`
- database migration files
- `LearningPathController.java`
- `CourseAccessService.java`
- ResourceGeneration / Orchestrator / Agent Trace / Review Gate modules
- formal OAuth2/JWK/Spring Security configuration

## Planned Test Commands

RED:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowServiceTest#createPathForUserLegacyOverloadIsRemoved+learningWorkflowServiceSubjectNameAdminHelperIsRemoved test
```

GREEN focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowServiceTest#createPathForUserLegacyOverloadIsRemoved+learningWorkflowServiceSubjectNameAdminHelperIsRemoved+createPathForUserRolesFirstOverloadAllowsExplicitAdminCrossLearnerCreation test
```

Adjacent:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowServiceTest,LearningWorkflowControllerTest test
```

Full backend:

```powershell
cd D:\多元agent\backend
mvn test
```

## Acceptance Criteria

- [x] `LearningWorkflowService` no longer exposes public `createPathForUser(String, CreateLearningPathRequest)`.
- [x] `LearningWorkflowService` no longer contains private `isAdmin(String)` subject-name helper.
- [x] Roles-first `createPathForUser(String, boolean, boolean, CreateLearningPathRequest)` remains intact.
- [x] Explicit admin can still create a learning path for another learner through roles-first service call.
- [x] No REST API contract, DTO, DB, dependency, frontend, ResourceGeneration, Orchestrator, Agent Trace, Review Gate, or formal OAuth2/JWK/Spring Security change.
- [x] Focused and adjacent tests pass, or limitation is documented.
- [x] Parent P3-4 remains not fully complete unless broader class/course and formal OAuth2/JWK/Spring Security items are completed separately.

## Completion Evidence

- Evidence / Acceptance: `docs/evidence/EVIDENCE-20260609-p3-4-learning-path-create-legacy-overload-cleanup.md`
- RED: `2 run, 2 failures`
- Focused: `3/3`
- Compile guard: success
- Adjacent: `25/25`
- Full backend: `477 run, 0 failures, 0 errors, 1 skipped`
