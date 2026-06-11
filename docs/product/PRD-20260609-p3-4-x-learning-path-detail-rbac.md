# PRD - P3-4-X LearningPath Detail Roles-First RBAC

## 1. 背景

P3-4 系列正在收口后端权限与安全。`POST /api/learning-paths` 已在 P3-4-S 改为 roles-first，但 `GET /api/learning-paths/{pathId}` 仍只传 `currentUserId` 到 service，并由 `LearningWorkflowService` 本地 `"admin"` 字符串判断 admin。

这导致 Bearer `USER sub=admin` 可能被当作管理员读取他人学习路径；Bearer `ADMIN sub=ops_admin` 反而不具备 admin detail 语义。

## 2. 用户价值

- 防止学习路径详情 IDOR / 角色混淆。
- 保持 Bearer role facts 是后端授权事实来源。
- 保持 non-admin missing/foreign anti-enumeration 语义。

## 3. 目标

- `GET /api/learning-paths/{pathId}` 使用 `UserContext.roles()` 派生 explicit admin fact。
- Bearer `ADMIN` 可读 foreign existing path，并对 missing path 得到 `NOT_FOUND`。
- Bearer `USER sub=admin` 不获得 admin 语义。
- owner 读取 own path 行为不变。

## 4. 非目标

- 不改变 `POST /api/learning-paths` 语义。
- 不修改 REST API path、request DTO、response DTO。
- 不修改数据库 schema。
- 不引入依赖。
- 不处理 formal OAuth2/JWK/Spring Security。
- 不把 P3-4 总项标为完成。

## 5. 成功指标

- RED 测试能复现当前 roles-first 缺口。
- GREEN 后 focused、adjacent、full backend tests 通过。
- Evidence / Acceptance / Changelog / Memory / TODO 完整更新。

