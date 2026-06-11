# REQ - P3-4-X LearningPath Detail Roles-First RBAC

## 1. Functional Requirements

| ID | Requirement |
|---|---|
| REQ-1 | `GET /api/learning-paths/{pathId}` 必须从 `UserContext.roles()` 派生 explicit admin fact。 |
| REQ-2 | Bearer `ADMIN sub=ops_admin` 即使携带 spoofed `X-User-Id`，也能读取 existing foreign learning path detail。 |
| REQ-3 | Bearer `ADMIN sub=ops_admin` 读取 missing path 返回 `NOT_FOUND`。 |
| REQ-4 | Bearer `USER sub=admin` 读取 foreign path 返回 `FORBIDDEN` 且无 `data`。 |
| REQ-5 | Bearer `USER sub=admin` 读取 missing path 返回 `FORBIDDEN` 且无 `data`。 |
| REQ-6 | Bearer owner 即使携带 spoofed `X-User-Id`，也能读取 own path。 |
| REQ-7 | Bearer non-owner foreign/missing path 继续安全返回 `FORBIDDEN`，响应不包含目标 id。 |

## 2. Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-1 | 不新增 dependency。 |
| NFR-2 | 不新增 DB migration。 |
| NFR-3 | 不改变 REST API contract。 |
| NFR-4 | 保持 Controller -> Application Service 分层。 |
| NFR-5 | 权限判断必须在后端 service 层完成，不依赖 Prompt 或 frontend。 |

## 3. Security Requirements

| ID | Requirement |
|---|---|
| SEC-1 | HTTP GET 主路径不得通过 `currentUserId == "admin"` 推断管理员。 |
| SEC-2 | Bearer token 优先于 `X-User-Id`，spoofed header 不影响身份和权限。 |
| SEC-3 | non-admin missing/foreign response 不得形成对象存在性 oracle。 |

