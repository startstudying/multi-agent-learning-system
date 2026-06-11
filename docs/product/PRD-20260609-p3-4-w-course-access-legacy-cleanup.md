# PRD - P3-4-W CourseAccessService legacy overload cleanup

## 背景

P3-4-M 到 P3-4-V 已将 Course API、Analytics、Assessment、RAG Document、LearningPath、ResourceGeneration、Orchestrator、Review Gate、Agent Trace 等主要 HTTP 路径逐步迁移到 roles-first RBAC。

当前 `CourseAccessService` 仍保留 4 个 legacy public overload，它们通过 `currentUserId == "admin"` 或 `currentUserId` 以 `teacher_` 开头推断角色。这些旧入口当前没有被主源码直接调用，但仍是未来误用风险：一旦新代码调用旧签名，Bearer `USER sub=admin` 或 `USER sub=teacher_1` 可能重新获得 admin/teacher 语义。

## 目标

删除 `CourseAccessService` 中不再被源码调用的 legacy overload 和私有 subject-name role inference helper，让课程授权公共 API 面只保留显式 role facts 入口。

## 用户价值

- 从编译期阻止新业务代码误用 subject-name RBAC。
- 收敛 P3-4 中 `CourseAccessService legacy cleanup` 剩余项。
- 保持现有 HTTP API 行为、DTO、数据库 schema 和 dev/test Bearer 兼容行为不变。

## 非目标

- 不清理 `KnowledgeCatalogService`、`AssessmentService`、`LearningWorkflowService` 等其他服务自身的 legacy overload。
- 不修改 `CurrentUserService` / `DevAuthFilter` 的 dev/test header fallback 行为。
- 不引入 formal OAuth2/JWK/Spring Security。
- 不修改 frontend、API path、request/response DTO、DB migration、依赖。
- 不声明 P3-4 或总后端 TODO 全部完成。
