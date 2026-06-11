# RUN - P3-4-U Review Gate RBAC 集成评审

## 决策

采用安全专家建议：本切片实施 `Review Gate ResourceReview roles-first RBAC`。

## 取舍

| 候选 | 决策 | 原因 |
|---|---|---|
| Review Gate ResourceReview roles-first RBAC | 采纳 | 审核接口可发布 AI 资源，当前仍有 subject-name inference，风险最高且切片小。 |
| ResourceGeneration / Agent Trace detail roles-first RBAC | 后续 | 架构风险明确，但不应与 Review Gate 同切片混改。 |
| CourseAccessService legacy overload 收口 | 后续 | 测试价值高，但当前 HTTP 主路径 Review Gate 风险更直接。 |

## 实施边界

- 只修改 `GET /api/reviews/resources` 与 `POST /api/reviews/resources/{reviewId}/decision` 的角色事实传递与服务授权。
- Controller 从 `UserContext.roles()` 派生 `ADMIN` / `TEACHER`。
- Service 新增 roles-first overload；legacy overload 保留兼容。
- Teacher 对象范围仍沿用 `ResourceReview -> ResourceGenerationTask.goalId -> Course.teacherId`。
- 不改 API path、DTO、DB schema、frontend、依赖、正式 OAuth2/JWK/Spring Security。

## Architecture Drift Pre-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只提取当前用户与角色事实；Service 执行对象授权和审核状态流转。 |
| Frontend rules | PASS | 不改 frontend。 |
| Agent / RAG rules | PASS | 保留 Review Gate、Agent task/trace 发布语义。 |
| Security | PASS | 权限由后端代码执行，不新增 secrets/dependencies。 |
| API / Database | PASS | 不改 API contract 或 schema。 |
