# RUN - P3-4-T Orchestrator RESOURCE_GENERATION Test Plan

## 测试目标

用 TDD 覆盖 Orchestrator `RESOURCE_GENERATION` create 的 roles-first RBAC 缺口，重点是 Bearer `USER sub=admin` 不能通过 subject-name role confusion 绕过 course enrollment。

## RED 测试建议

### 1. subject-name admin role confusion

`resourceGenerationWorkflowRejectsBearerUserSubjectAdminRoleConfusionBeforeResourceSideEffects`

- Bearer JWT：`sub=admin`，roles=`USER`。
- `workflowType=RESOURCE_GENERATION`，`learnerId=admin`。
- `goalId` 指向已存在 course，且 `admin` 没有 ACTIVE enrollment。
- 期望 HTTP `403` / `FORBIDDEN`。
- 断言没有 `resource_generation_task`、`learning_resource`、`resource_review`、`source_citation`、`model_call_log`、`token_usage_log`。

### 2. admin 不能为其他 learner 代创建

`resourceGenerationWorkflowRejectsBearerAdminForOtherLearnerBeforeWorkflowSideEffects`

- Bearer JWT：`sub=ops_admin`，roles=`ADMIN`。
- spoofed `X-User-Id=alice`。
- `learnerId=alice`。
- 期望 owner-only 拒绝，并且 workflow envelope 都不创建。

### 3. student owner + active enrollment 成功

`resourceGenerationWorkflowAllowsBearerStudentOwnerWithActiveEnrollmentAndIgnoresSpoofedHeader`

- Bearer JWT：`sub=alice`，roles=`STUDENT`。
- spoofed `X-User-Id=admin`。
- `learnerId=alice` 且 active enrollment。
- 期望成功，workflow/task/resource owner 均为 `alice`。

## 测试基础设施

`OrchestratorWorkflowControllerTest` 需要加入：

- `learning-os.auth.jwt-secret=unit-test-secret`
- `learning-os.auth.issuer=learning-os`
- HS256 `jwt(...)` helper
- `CourseRepository` / `CourseEnrollmentRepository` fixture helper
- ResourceGeneration side-effect 断言 helper

