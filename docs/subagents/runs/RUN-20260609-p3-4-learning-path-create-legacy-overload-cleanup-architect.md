# RUN-20260609 P3-4 LearningPath create legacy overload cleanup - Architect

## 结论

建议删除 `LearningWorkflowService.createPathForUser(String, CreateLearningPathRequest)` 和私有 `isAdmin(String)`。

## 关键证据

- `LearningWorkflowService` 两参 legacy overload 只通过 `isAdmin(currentUserId)` 委托四参 roles-first overload。
- `isAdmin(String)` 只判断 `"admin".equals(userId)`，属于 subject-name admin 语义。
- `LearningPathController.create(...)` 已从 `UserContext.roles()` 派生 explicit role facts，并调用四参 roles-first overload。
- 生产代码没有发现旧两参 create overload 的必要调用点。

## 必须保留的行为

- 非 admin 只能为自己创建 LearningPath。
- explicit `ADMIN` 可为其他 learner 创建 LearningPath，并保持 course-bound enrollment bypass。
- Bearer `USER sub=admin` 不获得 admin 创建语义。
- 不给 teacher 新增代创建能力。

## 不触碰范围

- 不改 REST API、DTO、schema、frontend、`CourseAccessService` enrollment 语义。
- 不改 ResourceGeneration、Orchestrator、Agent Trace、Review Gate。
- 不做 formal OAuth2/JWK/Spring Security 迁移。
