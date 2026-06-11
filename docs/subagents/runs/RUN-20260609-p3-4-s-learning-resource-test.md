# RUN - 20260609 P3-4-S LearningPath / ResourceGeneration create RBAC Test Plan

## 1. 目标

为 P3-4-S 增加最小 RED tests，证明：

- HTTP 主路径必须使用 Bearer role facts，而不是 subject-name inference。
- `USER sub=admin` 不能提升为 admin。
- ResourceGeneration 权限失败发生在 durable side effects 之前。

## 2. 推荐测试类

- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`

两个测试类均应补充本地 HS256 JWT helper，与既有 P3-4 controller tests 保持一致：

- `AUTH_SECRET = "unit-test-secret"`
- `AUTH_ISSUER = "learning-os"`
- `learning-os.auth.jwt-secret=unit-test-secret`
- `learning-os.auth.jwt-issuer=learning-os`

## 3. 必须新增 RED tests

### 3.1 LearningPath

#### `courseBoundLearningPathCreateUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`

前置：

- 创建 existing course。
- 不给 learner 创建 enrollment。

请求：

- `Authorization: Bearer jwt(sub=ops_admin, roles=[ADMIN])`
- `X-User-Id: alice`
- body: `learnerId=alice`, `goalId=<courseId>`

期望：

- `200 OK`
- path 持久化为 `learnerId=alice` / `goalId=<courseId>`

当前 RED 原因：

- 旧逻辑只认 `sub=admin`，会拒绝 `ops_admin` 跨 learner create。

#### `courseBoundLearningPathCreateRejectsBearerUserSubjectAdminRoleConfusionWithoutPersisting`

前置：

- 创建 existing course。
- 不给 `alice` 创建 enrollment。

请求：

- `Authorization: Bearer jwt(sub=admin, roles=[USER])`
- body: `learnerId=alice`, `goalId=<courseId>`

期望：

- `403 FORBIDDEN`
- 无 `data`
- 不新增 learning path / nodes / event

当前 RED 原因：

- 旧逻辑可能把 `sub=admin` 当 admin，错误绕过 owner/enrollment。

### 3.2 ResourceGeneration

#### `courseBoundResourceGenerationCreateRejectsBearerAdminForOtherLearnerWithoutSideEffects`

前置：

- 创建 existing course。
- 不给 `alice` 创建 enrollment。

请求：

- `Authorization: Bearer jwt(sub=ops_admin, roles=[ADMIN])`
- `X-User-Id: alice`
- body: `learnerId=alice`, `goalId=<courseId>`

期望：

- `403 FORBIDDEN`
- 无 task/resource/review/trace/model-call side effects

说明：

- 采用 Architect/Security 决策：ResourceGeneration 不扩展 admin 代创建能力。

#### `courseBoundResourceGenerationCreateRejectsBearerUserSubjectAdminRoleConfusionBeforeSideEffects`

前置：

- 创建 existing course。
- 不给 `admin` 创建 enrollment。

请求：

- `Authorization: Bearer jwt(sub=admin, roles=[USER])`
- body: `learnerId=admin`, `goalId=<courseId>`

期望：

- `403 FORBIDDEN`
- 无 `resource_generation_task`
- 无 `learning_resource`
- 无 `resource_review`
- 无 `agent_task`
- 无 `agent_trace`
- 无 `model_call_log`

当前 RED 原因：

- 旧 enrollment helper 可能将 `sub=admin` 当 admin bypass enrollment。

## 4. 回归测试要求

既有测试必须保持：

- active enrolled learner 可创建 course-bound learning path。
- active enrolled learner 可创建 course-bound resource generation task。
- template goal 非 courseId 仍兼容。
- mismatched learner create 仍拒绝。

## 5. 验证命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest test
mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

