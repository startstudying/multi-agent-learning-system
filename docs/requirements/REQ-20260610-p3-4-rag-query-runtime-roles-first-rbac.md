# REQ-20260610 P3-4 子任务：RAG query runtime roles-first RBAC

## 背景

P3-4-P 已完成 RAG Knowledge Base management 主路径 roles-first RBAC，但验收文档明确未覆盖 `/api/rag/query` retrieval runtime。当前 `ChatController`、`TutorController` 和 Orchestrator RAG_QA 仍主要向 `RagQueryService` 传入 `currentUserId`，查询服务再走 legacy `PermissionService.requireReadableKbIds(userId, kbIds)`，无法表达 Bearer token 中的显式 `ADMIN` / `TEACHER` role facts。

## 目标

把 RAG query runtime 的 KB 读取授权迁移到 roles-first 路径：

- HTTP `/api/rag/query` POST / GET 从 `UserContext.roles()` 派生 role facts。
- Chat SSE 与 Tutor ask / stream 也传入 role facts。
- Orchestrator `RAG_QA` workflow 在 replay precheck 和实际 query 执行时传入 role facts。
- `RagQueryService` 对普通 query、requestId replay、traceId query 均支持 roles-first overload。
- legacy overload 保留，但默认 `currentUserAdmin=false` / `currentUserTeacher=false`，不得从 userId 字符串推断角色。

## 非目标

- 不新增 KB-course binding schema。
- 不改变 `KnowledgeBase`、`KbDocument`、`KbQueryLog` DB schema。
- 不改变 REST path、request DTO、response DTO 或前端。
- 不处理 SSE 生产认证传递策略。
- 不引入新依赖。

## 功能需求

| ID | Requirement |
|---|---|
| FR-01 | Bearer `ADMIN` 访问 `/api/rag/query` 时，即使 `X-User-Id` spoofed，也应以 JWT subject 和 explicit admin role 作为授权事实。 |
| FR-02 | Bearer `USER sub=admin` 不得通过 subject-name role confusion 读取 foreign private KB。 |
| FR-03 | `RagQueryService` role-aware query 对 foreign private KB 的 admin read 应通过，并正常写 query log / citation。 |
| FR-04 | legacy `RagQueryService.query("admin", ...)` 不得获得 admin-like KB read。 |
| FR-05 | requestId replay path 必须先按当前 role facts 做 permission filtering，禁止失权后通过 replay 读取旧响应。 |
| FR-06 | Orchestrator `RAG_QA` replay precheck 和实际 query 必须使用同一组 role facts。 |

## 约束

- Controller 只读取 `CurrentUserService.currentUser()` 并派生 role facts，不实现 KB 权限规则。
- KB read authorization 仍由 `PermissionService` 执行。
- 越权 query 不能写 `kb_query_log` 或 `source_citation`。
- `requestHash` 语义保持稳定，不新增敏感字段。
