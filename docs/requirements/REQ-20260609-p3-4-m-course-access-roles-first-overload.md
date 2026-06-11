# REQ-20260609 P3-4-M Course API / CourseAccessService roles-first overload

## 1. Functional Requirements

| ID | Requirement | Priority |
|---|---|---|
| REQ-P3-4-M-1 | `CourseAccessService` 必须提供 role-aware overload：course read、course manage、course list。 | P0 |
| REQ-P3-4-M-2 | `CourseController` 必须向 `KnowledgeCatalogService` 传入 `currentUserId`、`isAdmin()`、`isTeacherUser()`。 | P0 |
| REQ-P3-4-M-3 | `KnowledgePointController` 的 knowledge point / dependency 写路径必须传入相同 roles-first facts。 | P0 |
| REQ-P3-4-M-4 | Bearer `ADMIN` token 即使 `sub != "admin"` 且携带 spoofed `X-User-Id`，也必须可读取/list/manage existing courses。 | P0 |
| REQ-P3-4-M-5 | Bearer `TEACHER` token 即使 `sub` 没有 `teacher_` 前缀，只要 `sub == Course.teacherId`，也必须可读/写 own course graph。 | P0 |
| REQ-P3-4-M-6 | Bearer student/user 即使 spoof `X-User-Id: admin` 也不能读取 foreign course 或写 course graph。 | P0 |
| REQ-P3-4-M-7 | Admin missing course 必须返回 `NOT_FOUND`；non-admin missing 与 foreign course 必须统一返回 `FORBIDDEN` 且无 `data`。 | P0 |
| REQ-P3-4-M-8 | 旧 `CourseAccessService` / `KnowledgeCatalogService` 签名必须保留兼容，不扩大到全仓库迁移。 | P0 |
| REQ-P3-4-M-9 | 不新增依赖、DB migration、API path、frontend、RAG/model/vector 变更。 | P0 |

## 2. Error Semantics

| Scenario | HTTP | code | data |
|---|---:|---|---|
| Bearer admin existing course detail/list/graph/manage | 200 | `OK` | present |
| Bearer admin missing course detail/graph | 404 | `NOT_FOUND` | absent |
| Bearer teacher own course detail/graph/manage | 200 | `OK` | present |
| Bearer teacher foreign/missing course | 403 | `FORBIDDEN` | absent |
| Bearer student foreign/missing course | 403 | `FORBIDDEN` | absent |
| Bearer student spoofed admin header | 403 | `FORBIDDEN` | absent |

## 3. Security Requirements

- Controller 只提取 current user 与 role facts，不写对象归属规则。
- Service 层必须完成 course read/manage/list scope。
- 越权响应不得包含 course title、teacherId、chapterId、knowledgePointId、dependencyId 等对象详情。
- JWT 测试 secret 只能使用固定假值 `unit-test-secret`。

## 4. Non-functional Requirements

- 测试不依赖外部模型、VectorDB、MinIO 或真实 MySQL。
- 不修改 `docs/superpowers/**`。
- Maven focused / adjacent / full backend tests 可运行，若不能运行必须在 Evidence 中说明限制。

## 5. 验收状态

已完成实现与验证；本需求对应的 Evidence / Acceptance / Retro / Memory 已补齐。
