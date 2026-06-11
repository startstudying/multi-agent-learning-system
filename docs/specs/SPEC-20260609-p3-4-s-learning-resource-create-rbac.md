# SPEC - P3-4-S LearningPath / ResourceGeneration course-bound create roles-first RBAC

## 1. Traceability

- PRD: `docs/product/PRD-20260609-p3-4-s-learning-resource-create-rbac.md`
- REQ: `docs/requirements/REQ-20260609-p3-4-s-learning-resource-create-rbac.md`
- Subagent reports:
  - `docs/subagents/runs/RUN-20260609-p3-4-s-learning-resource-backend.md`
  - `docs/subagents/runs/RUN-20260609-p3-4-s-learning-resource-security.md`
  - `docs/subagents/runs/RUN-20260609-p3-4-s-learning-resource-test.md`
  - `docs/subagents/runs/RUN-20260609-p3-4-s-learning-resource-integration-review.md`

## 1.1 Scope Boundary

本切片只验收两个直连 HTTP create API：

- `POST /api/learning-paths`
- `POST /api/resources/generation-tasks`

Orchestrator `RESOURCE_GENERATION` workflow create 仍是后续切片。该路径当前仍由 Orchestrator 调用 legacy workflow create 入口，不作为本切片完成条件。

## 2. Current State

### 2.1 LearningPath

`LearningPathController.create(...)` 当前只调用：

```java
learningWorkflowService.createPathForUser(currentUserService.currentUserId(), request)
```

`LearningWorkflowService.createPathForUser(...)` 当前用本地 legacy `isAdmin(currentUserId)` 判定跨 learner admin 权限，并调用旧签名 enrollment helper。

### 2.2 ResourceGeneration

`ResourceGenerationController.create(...)` 当前只调用：

```java
resourceGenerationService.createTask(currentUserService.currentUserId(), request)
```

`ResourceGenerationService` 当前 owner-only 是正确边界；问题在 course-bound enrollment helper 调用旧签名，导致 `USER sub=admin` 可能绕过 active enrollment。

Orchestrator `RESOURCE_GENERATION` workflow create 不在本切片内迁移；Integration Review 已将其记录为后续风险。

## 3. API Contract

本切片不修改 API path、request DTO、response DTO。

| API | Contract Change | Authorization Change |
|---|---|---|
| `POST /api/learning-paths` | 无 | create 使用 roles-first admin fact 与 roles-first enrollment helper |
| `POST /api/resources/generation-tasks` | 无 | create 使用 roles-first service overload；保持 owner-only；course-bound active enrollment 不允许 subject-name admin bypass |

## 4. Authorization Semantics

### 4.1 LearningPath create

| Scenario | Expected |
|---|---|
| Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id` + learner `alice` + existing course without enrollment | `OK` |
| Bearer `USER sub=admin` + learner `alice` + existing course without enrollment | `FORBIDDEN` |
| Bearer non-admin owner + active enrollment | `OK` |
| Bearer non-admin owner + no active enrollment | `FORBIDDEN` |
| template goal not existing course | 保持兼容，owner 可创建 |

### 4.2 ResourceGeneration create

| Scenario | Expected |
|---|---|
| Bearer `ADMIN sub=ops_admin` + learner `alice` | `FORBIDDEN`，不新增 admin 代创建能力 |
| Bearer `TEACHER sub=instructor_1` + learner `alice` | `FORBIDDEN`，不新增 teacher 代创建能力 |
| Bearer `USER sub=admin` + learner `admin` + existing course without enrollment | `FORBIDDEN`，无 durable side effects |
| Bearer owner + active enrollment | `OK` |
| template goal not existing course | 保持兼容，owner 可创建 |

## 5. Service Contract

### 5.1 CourseAccessService

新增 roles-first overload：

```java
public void requireLearnerEnrolledForExistingCourse(
        String currentUserId,
        boolean currentUserAdmin,
        String learnerId,
        String courseId
)
```

语义：

- `courseId` 不是 existing course 时直接返回，保持 template goal 兼容。
- `currentUserAdmin == true` 时可 bypass enrollment，仅供明确允许 admin 代操作的调用方使用。
- 其他情况要求 `course_enrollment.status == ACTIVE`。

旧签名保留并委托 legacy inference，用于未迁移兼容路径。

### 5.2 LearningWorkflowService

新增 roles-first overload：

```java
public LearningPathResponse createPathForUser(
        String currentUserId,
        boolean currentUserAdmin,
        boolean currentUserTeacher,
        CreateLearningPathRequest request
)
```

语义：

- `currentUserAdmin` 可跨 learner。
- 非 admin 必须 `currentUserId.equals(request.learnerId())`。
- course-bound enrollment 调用 roles-first helper，并传 `currentUserAdmin`。
- `currentUserTeacher` 暂不授予代创建能力，仅保留签名一致性。

### 5.3 ResourceGenerationService

新增 roles-first overload：

```java
public ResourceGenerationResponse createTask(
        String currentUserId,
        boolean currentUserAdmin,
        boolean currentUserTeacher,
        ResourceGenerationRequest request
)
```

```java
public ResourceGenerationResponse createTaskInWorkflow(
        String currentUserId,
        boolean currentUserAdmin,
        boolean currentUserTeacher,
        ResourceGenerationRequest request,
        AgentExecutionContext executionContext
)
```

语义：

- 保持 `ensureLearnerOwner(currentUserId, request.learnerId())`，不因 `ADMIN` / `TEACHER` role 放宽。
- course-bound enrollment 调用 roles-first helper，但传 `currentUserAdmin=false`，避免 ResourceGeneration 使用 admin enrollment bypass。
- 权限失败必须在 requestId replay、safety check、agent run、generation task save、model call 前发生。
- 本切片仅验收 direct HTTP create 调用该 roles-first 入口；Orchestrator workflow create 的调用方迁移另开后续切片。

## 6. Persistence / Dependency / Frontend

- DB migration：无。
- 新依赖：无。
- Frontend：无。
- API DTO：无。
- Agent/RAG runtime：无。

## 7. Architecture Drift Pre-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 提取身份事实，Service 执行授权 |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 不改 Agent/RAG runtime；ResourceGeneration 仅收口调用前授权 |
| Security | PASS | 无 secrets；无 dependency |
| API / Database | PASS | 无 API/DB contract change |
