# REQ-20260609 P3-4-K 权限渗透测试矩阵补齐

## 1. Functional Requirements

| ID | Requirement | Priority |
|---|---|---|
| REQ-P3-4-K-1 | 新增跨模块权限矩阵测试，覆盖 auth、course、assessment、RAG document、resource/review/trace、analytics 的最小安全不变量。 | P0 |
| REQ-P3-4-K-2 | Bearer token 存在时必须优先于 spoofed `X-User-Id`，invalid Bearer 不得 fallback。 | P0 |
| REQ-P3-4-K-3 | prod / production / staging 缺少 Bearer 时不得信任 `X-User-Id`。 | P0 |
| REQ-P3-4-K-4 | student 不得创建课程、写入课程知识图谱、上传带 course metadata 的 RAG 文档或提交 resource review decision。 | P0 |
| REQ-P3-4-K-5 | teacher 只能访问 own-course scoped surface；foreign course detail、graph、student summary、review decision 必须拒绝。 | P0 |
| REQ-P3-4-K-6 | student 只能访问自己的 learner scoped surface；foreign answer、wrong-question、student summary、resource task/trace 必须拒绝。 | P0 |
| REQ-P3-4-K-7 | 非 admin 访问 foreign 与 missing object 必须返回同类安全 `FORBIDDEN` 且无 `data`，响应不得回显对象 ID。 | P0 |
| REQ-P3-4-K-8 | admin 可访问 existing global/foreign object；admin missing object 保持 `NOT_FOUND`。 | P0 |
| REQ-P3-4-K-9 | token-budget governance、overview、ops alerts 等运营聚合接口必须 admin-only。 | P0 |
| REQ-P3-4-K-10 | 不新增依赖、不改 schema、不改前端；生产代码变更必须由 RED 测试触发并保持最小。 | P0 |

## 2. Matrix Scope

| Domain | Minimum Matrix |
|---|---|
| Auth Context | dev/test fallback、valid Bearer wins、invalid Bearer no fallback、staging/prod no header-only auth |
| Course / Graph | student write deny、student active-only list/detail/graph、teacher own-course only、admin global |
| Assessment | student owner-only、teacher own-course active-enrolled learner、admin global、foreign/missing anti-enumeration |
| RAG Document | KB write 与 course metadata scope 分离；course/chapter metadata fail before side effects |
| Resource / Review / Trace | learner owner-only task/trace；review admin global / teacher own-course / student denied |
| Analytics | overview、ops alerts、token-budget governance admin-only；student summary course scope |

## 3. Error Semantics

| Scenario | HTTP | code | data |
|---|---:|---|---|
| invalid Bearer + spoofed header | 401 | `UNAUTHORIZED` | absent |
| staging/prod header-only auth | 401 | `UNAUTHORIZED` | absent |
| student writes teacher-managed object | 403 | `FORBIDDEN` | absent |
| teacher accesses foreign course object | 403 | `FORBIDDEN` | absent |
| student accesses foreign learner object | 403 | `FORBIDDEN` | absent |
| non-admin missing object | 403 | `FORBIDDEN` | absent |
| admin missing object | 404 | `NOT_FOUND` | absent |

## 4. Security Requirements

- 越权响应不得包含 foreign object id、parent id、resource id、traceId、title、markdown content、raw model output 或 token。
- Controller 只读取 current user 和请求参数；对象级授权在 Service 层完成。
- 测试数据不得包含真实 secret；JWT 测试 secret 只能是固定假值。
- 若新增测试发现生产缺陷，先记录 RED，再把具体生产文件加入 Context Pack 后做最小修复。

## 5. Non-functional Requirements

- 测试应可通过 Maven focused/adjacent/full 命令运行。
- 测试不能依赖真实外部模型、VectorDB、MinIO 或 MySQL。
- 不修改 `docs/superpowers/`。

## 6. Acceptance Status

| Area | Status | Notes |
|---|---|---|
| Auth Context | PASS | staging header-only deny、Bearer role 优先于 spoofed `X-User-Id` 已验证。 |
| Analytics | PASS | `overview` / `token-budget governance` 由 role-derived admin gate 保护。 |
| Course / Graph | PASS | active enrolled student 仍不能写 graph；dropped enrollment course 不泄露给 student list。 |
| RAG Document | PASS | student 拥有 public KB 且 enrolled 也不能伪造 course metadata。 |
| Assessment / Resource / Review / Trace | PASS | 既有 owner/course/admin/anti-enumeration 矩阵在 adjacent regression 中通过。 |
| Non-functional | PASS | 无依赖、schema、frontend 改动；focused/adjacent/full backend verification 通过。 |

限制：broader class/course 与 formal OAuth2/JWK/Spring Security 仍为后续任务。
