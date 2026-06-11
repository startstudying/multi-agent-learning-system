# RUN - 20260609 P3-4-S LearningPath / ResourceGeneration create RBAC Backend Architect

## 1. 范围

只读架构分析，目标切片为：

- `POST /api/learning-paths`
- `POST /api/resources/generation-tasks`

本切片不包含 ResourceGeneration detail/trace/cancel/review、前端、DB schema、依赖、formal OAuth2/JWK/Spring Security。

## 2. 当前调用链

### LearningPath create

```text
LearningPathController.create
-> CurrentUserService.currentUserId()
-> LearningWorkflowService.createPathForUser(currentUserId, request)
-> isAdmin(currentUserId) owner/admin gate
-> CourseAccessService.requireLearnerEnrolledForExistingCourse(currentUserId, learnerId, goalId)
-> generatePath
-> persist learning_path / learning_path_node / learning_event
```

当前风险：

- `LearningPathController` 没有传 `UserContext.roles()` 派生的 role facts。
- `LearningWorkflowService.isAdmin(...)` 只识别 `userId == "admin"`。
- course-bound enrollment helper 也会用 legacy `sub=admin` bypass。

### ResourceGeneration create

```text
ResourceGenerationController.create
-> CurrentUserService.currentUserId()
-> ResourceGenerationService.createTask(userId, request)
-> ensureLearnerOwner(userId, learnerId)
-> requireCourseEnrollmentIfCourseGoal(userId, request)
-> CourseAccessService.requireLearnerEnrolledForExistingCourse(userId, learnerId, goalId)
-> agentRunRecorder.startRun
-> createTaskWithContext
-> resource_generation_task / learning_resource / resource_review / trace / model_call_log
```

当前风险：

- HTTP 主路径没有传 roles-first facts。
- ResourceGeneration 本身是 owner-only，但 `USER sub=admin` 且 `learnerId=admin` 会通过 owner check，再被 enrollment helper 的 legacy admin bypass 放行。
- 权限失败必须继续发生在 `agentRunRecorder.startRun` 与 `createTaskWithContext` 之前。

## 3. 建议最小实现

### Controller

- `LearningPathController.create(...)` 读取 `CurrentUserService.currentUser()`。
- `ResourceGenerationController.create(...)` 读取 `CurrentUserService.currentUser()`。
- 仅从 `UserContext.roles()` 派生：
  - `currentUserAdmin`
  - `currentUserTeacher`
- 不在 HTTP 主路径调用 `CurrentUserService.isAdmin()` / `isTeacherUser()`，避免 dev/test legacy subject inference 干扰 Bearer 语义。

### Service

新增 roles-first overload，保留旧签名兼容：

```java
LearningPathResponse createPathForUser(
        String currentUserId,
        boolean currentUserAdmin,
        boolean currentUserTeacher,
        CreateLearningPathRequest request
)
```

```java
ResourceGenerationResponse createTask(
        String currentUserId,
        boolean currentUserAdmin,
        boolean currentUserTeacher,
        ResourceGenerationRequest request
)
```

```java
ResourceGenerationResponse createTaskInWorkflow(
        String currentUserId,
        boolean currentUserAdmin,
        boolean currentUserTeacher,
        ResourceGenerationRequest request,
        AgentExecutionContext executionContext
)
```

### CourseAccessService

新增 roles-first enrollment helper：

```java
void requireLearnerEnrolledForExistingCourse(
        String currentUserId,
        boolean currentUserAdmin,
        String learnerId,
        String courseId
)
```

旧签名保留，用于未迁移兼容路径。

## 4. 冲突决议建议

- LearningPath 保留 P3-4-D 语义：显式 `ADMIN` role 可为任意 learner 创建 learning path，并可绕过 course-bound enrollment。
- ResourceGeneration 不扩展 admin/teacher 代创建能力：继续要求 `currentUserId == request.learnerId()`。
- ResourceGeneration course-bound create 不使用 admin enrollment bypass；active enrollment 是生成资源前置条件。

## 5. 文件建议

允许修改：

- `backend/src/main/java/com/learningos/learning/api/LearningPathController.java`
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- `backend/src/main/java/com/learningos/agent/api/ResourceGenerationController.java`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`
- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`

禁止修改：

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- formal OAuth2/JWK/Spring Security
- Agent/RAG/model provider/review gate runtime

