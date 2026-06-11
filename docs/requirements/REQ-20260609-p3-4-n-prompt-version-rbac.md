# REQ-20260609 P3-4-N PromptVersion 管理 API RBAC 与 promptText 暴露收口

## 1. 功能需求

### FR-1 Admin-only 写入

`POST /api/agent/prompt-versions` 必须仅允许当前可信身份 roles 包含 `ADMIN` 的用户调用。

- Bearer `ADMIN sub=ops_admin` 可创建/更新。
- Bearer `ADMIN` 请求中的 `X-User-Id` 不得覆盖 token 身份。
- Bearer `STUDENT/USER sub=admin` 不得被当作 admin。
- Bearer `TEACHER` 不得写入 prompt version。

### FR-2 Admin/Teacher 读取

`GET /api/agent/prompt-versions` 与 `GET /api/agent/prompt-versions/{code}/{version}` 必须仅允许 roles 包含 `ADMIN` 或 `TEACHER` 的用户调用。

- `ADMIN` 可读取完整响应。
- `TEACHER` 可读取 metadata 响应。
- `STUDENT` / `USER` 返回 `FORBIDDEN`。
- Bearer `USER sub=teacher_1` 不得被当作 teacher。

### FR-3 promptText 脱敏

`promptText` 仅在 admin 响应中返回。

- Teacher list/detail 响应必须不包含 `promptText` 字段。
- Student/User 禁止访问，不返回 data。
- 内部 Service 用于模型调用的 active prompt 查询能力不因 HTTP 脱敏而失效。

### FR-4 API path 与请求合同不变

本切片不改变 API path、HTTP method、`PromptVersionUpsertRequest` 字段或数据库结构。

### FR-5 dev/test 兼容

dev/test 无 Bearer token 时可继续通过 `X-User-Id=admin` / `X-User-Id=teacher_1` 由 `DevAuthFilter` 派生 roles。

## 2. 非功能需求

- 权限检查必须在后端代码中完成，不能依赖 Prompt。
- 不新增依赖。
- 不写入或记录真实 secret、API key、prompt 原文到 evidence/memory。
- 错误响应使用统一 `ApiResponse` envelope。
- `FORBIDDEN` 响应不包含 `data`。

## 3. 用户流程

### 管理员写入

1. 请求携带 Bearer JWT，roles 包含 `ADMIN`。
2. Controller 从 `CurrentUserService.currentUser()` 读取 role facts。
3. Service 验证 admin。
4. 执行原有 upsert 逻辑。
5. 返回包含 `promptText` 的完整 `PromptVersionResponse`。

### 教师读取

1. 请求携带 Bearer JWT，roles 包含 `TEACHER`。
2. Service 验证 read role。
3. 查询 prompt version。
4. 返回不含 `promptText` 的 metadata 响应。

### 普通用户访问

1. 请求携带 Bearer JWT，roles 为 `USER` 或 `STUDENT`。
2. Service 在查询或写入前拒绝。
3. 返回 `FORBIDDEN`，`data` 不存在。

## 4. 边界情况

- Bearer token 存在时必须优先于 `X-User-Id`。
- Bearer `sub=admin` 但 roles 不含 `ADMIN` 时拒绝写入和读取。
- Bearer `sub=teacher_1` 但 roles 不含 `TEACHER` 时拒绝读取。
- Admin 查询 missing prompt version 返回 `NOT_FOUND`。
- Teacher 查询 missing prompt version 可返回 `NOT_FOUND`，因为 teacher 已被授权读取 metadata 管理面。

## 5. 依赖

- 依赖已存在的 `DevAuthFilter` / `CurrentUserService` / `UserContext.roles()`。
- 依赖现有 `PromptVersionRepository` 与 `PromptVersionResponse`。
- 不新增 Maven dependency。

## 6. Traceability

- 来源 PRD：`docs/product/PRD-20260609-p3-4-n-prompt-version-rbac.md`
- 来源 TODO：`docs/planning/backend-architecture-todolist.md` P3-4 权限与安全加固。
- 历史未完成项：`docs/acceptance/ACCEPT-20260606-model-call-prompt-metadata.md` 明确记录 PromptVersion API RBAC 与 `promptText` 暴露策略待补。

