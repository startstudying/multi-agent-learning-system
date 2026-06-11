# REQ-20260609 P3-4-L class analytics roles-first course scope

## 1. Functional Requirements

| ID | Requirement | Priority |
|---|---|---|
| REQ-P3-4-L-1 | `GET /api/analytics/classes/{courseId}/summary` 必须使用 roles-first admin/teacher 判断。 | P0 |
| REQ-P3-4-L-2 | Bearer `ADMIN` token 必须优先于 spoofed `X-User-Id`，并可访问 existing course class summary。 | P0 |
| REQ-P3-4-L-3 | Bearer `TEACHER` token 仅当 `sub == Course.teacherId` 时可访问该课程 class summary。 | P0 |
| REQ-P3-4-L-4 | Bearer `STUDENT` token 即使携带 spoofed `X-User-Id: admin` 也必须返回 `FORBIDDEN` 且无 `data`。 | P0 |
| REQ-P3-4-L-5 | 非 admin 访问 missing course 与 foreign course class summary 必须统一返回 `FORBIDDEN` 且无 `data`。 | P0 |
| REQ-P3-4-L-6 | admin 访问 missing course class summary 必须保留 `NOT_FOUND`。 | P0 |
| REQ-P3-4-L-7 | class learner set 继续来自 active `course_enrollment`，不回退到 legacy learning path membership。 | P0 |
| REQ-P3-4-L-8 | 不新增依赖、DB migration、API path、frontend 或 RAG/model/vector 变更。 | P0 |

## 2. Error Semantics

| Scenario | HTTP | code | data |
|---|---:|---|---|
| Bearer admin existing course | 200 | `OK` | present |
| Bearer teacher own course | 200 | `OK` | present |
| Bearer teacher foreign course | 403 | `FORBIDDEN` | absent |
| Bearer student spoofed admin header | 403 | `FORBIDDEN` | absent |
| non-admin missing course | 403 | `FORBIDDEN` | absent |
| admin missing course | 404 | `NOT_FOUND` | absent |

## 3. Security Requirements

- Controller 只读取 current user 与请求参数，class summary 对象级授权在 Service 层完成。
- 任何越权响应不得包含 course id 之外的敏感对象详情、teacherId、course title、learner ids、review ids、resource title 或 markdown content。
- JWT 测试 secret 只能使用固定假值，不写入真实凭据。

## 4. Non-functional Requirements

- 测试不依赖真实外部模型、VectorDB、MinIO 或 MySQL。
- Maven focused / adjacent / full backend tests 可运行。
- 不修改 `docs/superpowers/**`。
