# SPEC - 权限与安全加固

## 1. 概述

本规格定义 P3-4 的最小安全收口：Profile、Learning Path、Analytics overview、Health exposure 和 RAG `kbIds` 的权限边界加固。目标是阻断最明显的越权路径，并为后续完整 RBAC 预留接口边界。

## 2. 追踪

- PRD：`docs/product/PRD-20260606-permission-security-hardening.md`
- REQ：`docs/requirements/REQ-20260606-permission-security-hardening.md`
- TODO：`docs/planning/backend-architecture-todolist.md` P3-4

## 3. 领域模型

- `CurrentUserService`：提供当前用户身份。
- `LearningWorkflowService`：提供 profile 和 path 数据读取。
- `AnalyticsService`：提供 overview / student / teacher summary。
- `HealthService`：提供环境和依赖状态。
- `PermissionService`：提供 KB 访问过滤。

## 4. API 契约

### 4.1 Profile

```http
POST /api/profile/dialogue/extract
```

规则：`request.learnerId` 必须等于当前用户。

### 4.2 Learning Path

```http
POST /api/learning-paths
GET /api/learning-paths/{pathId}
```

规则：请求与路径 owner 必须一致。

### 4.3 Analytics

```http
GET /api/analytics/overview
```

规则：仅 `admin` 可访问。

### 4.4 Health

```http
GET /api/health
```

规则：公开基础健康信息，但不泄露密码、私钥、完整 provider error 或敏感部署指纹。

### 4.5 RAG

```http
POST /api/rag/query
GET /api/rag/query
POST /api/orchestrator/workflows
```

规则：`kbIds` 必须全部可访问；混合越权请求按 strict 方式拒绝。

## 5. 响应约束

- 任何越权请求不得返回敏感资源数据。
- 健康输出不得返回 secret / password / token / provider error 原文。
- 管理视图不得被普通学生读取。

## 6. 后端流程

```text
Controller
-> currentUserId / owner 校验
-> Service 权限判断
-> 允许则继续查询
-> 拒绝则返回 FORBIDDEN
```

## 7. 数据库变更

无新增数据库迁移。

## 8. 状态流转

```text
ALLOW
-> FORBIDDEN
-> NOT_FOUND
```

## 9. 错误处理

- Profile / Learning Path owner mismatch: `FORBIDDEN`
- Analytics overview non-admin: `FORBIDDEN`
- RAG mixed forbidden `kbIds`: `FORBIDDEN`
- Health sensitive detail requests: 返回收敛后的信息，不抛敏感数据

## 10. 权限规则

- 学生只能访问自己的画像、路径和相关记录。
- Teacher 仅能访问已授权课程/班级相关数据。
- Admin 可访问治理类聚合数据。
- Strict RAG 权限模式下，任何越权 KB 都不能混入结果集。

## 11. Trace / 日志

- 使用现有 `X-Trace-Id`。
- 越权拒绝必须有审计日志或测试证据。

## 12. 测试策略

- `ProfileControllerTest` / `LearningWorkflowControllerTest` 补 owner 越权测试。
- `AnalyticsControllerTest` 补 overview admin-only 测试。
- `HealthControllerTest` 补敏感字段收敛测试。
- `ChatControllerTest` / `RagQueryServiceTest` 补混合 `kbIds` strict 拒绝测试。

## 13. 验收清单

- [x] Profile owner 校验通过。
- [x] Learning Path owner 校验通过。
- [x] analytics overview 仅 admin 可访问。
- [x] Health 输出收敛。
- [x] RAG 混合越权 `kbIds` 被严格拒绝。
- [x] 安全测试通过。

## 14. 实施备注

- 本 SPEC 已完成最小安全收口切片。
- 完整生产认证、课程/班级 RBAC、资源与答题记录全量权限矩阵不在本切片内，继续保留为 P3-4 后续任务。
