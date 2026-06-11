# REQ - P3-4-P RAG KB management roles-first RBAC

## 1. 追踪

- PRD: `docs/product/PRD-20260609-p3-4-p-rag-kb-rbac.md`
- 关联规格：
  - `docs/specs/SPEC-20260608-real-auth-rbac-context.md`
  - `docs/specs/SPEC-20260608-rag-document-course-scope.md`
  - `docs/specs/SPEC-20260609-p3-4-m-course-access-roles-first-overload.md`
  - `docs/specs/SPEC-20260609-p3-4-permission-matrix.md`

## 2. Skill Selection Report

## Task Type

RAG / retrieval 管理入口权限加固；具体是 RAG KB management 和 document/index task 主路径 roles-first RBAC。

## Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目强制的 PRD/REQ/SPEC/PLAN/TASK/CONTEXT -> implementation -> evidence 流程 |
| `auth-context-boundary` | Bearer JWT、`UserContext.roles()`、spoofed header 与 legacy fallback 边界 |
| `object-scope-authorization` | KB/document/index task 对象级授权、missing-vs-foreign anti-enumeration |
| `rag-hybrid-retrieval` | 确认本切片不改变 retrieval runtime，只收口 KB/document 管理权限 |
| `spring-boot-architecture` | Controller -> Service -> Repository 分层和 role facts 传递 |
| `security-review` | RBAC/IDOR/role-confusion 风险评估 |
| `test-generator` | Bearer spoof、role-confusion、missing oracle 回归测试 |
| `verification-before-completion` | 完成声明前必须有 fresh verification evidence |

## Missing Skills

无。现有 project-specific skills 已覆盖 auth context、object-scope authorization 和 RAG retrieval 边界。

## GitHub Research Needed

No。任务是既有项目 RBAC 迁移，不需要引入外部库或参考实现。

## New Project-Specific Skill To Create

暂不创建。若后续多个 RAG management 入口继续迁移，可抽取 `rag-management-rbac` 技能。

## 3. Functional Requirements

| ID | Requirement | Priority | Acceptance |
|---|---|---|---|
| REQ-P3-4-P-01 | `GET /api/knowledge-bases` 必须使用 Bearer roles-first admin fact；`ADMIN` role 可列出全部 active KB | P0 | Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id=student` 可看到 foreign private KB |
| REQ-P3-4-P-02 | `POST /api/knowledge-bases` 必须继续允许当前用户创建个人 KB | P0 | `USER/STUDENT/TEACHER/ADMIN` 按当前 subject 创建 owner 为当前 userId 的 KB |
| REQ-P3-4-P-03 | KB read/write authorization 必须支持 role-aware overload，并保留旧签名兼容 RAG query 等路径 | P0 | 新管理入口使用 role-aware overload；旧测试继续通过 |
| REQ-P3-4-P-04 | Bearer `ADMIN` 必须可上传 document 到任意 active KB，即使请求包含 spoofed `X-User-Id` | P0 | Controller test 通过 |
| REQ-P3-4-P-05 | Bearer `TEACHER` 必须可在 own-course manage 范围内上传 course metadata，不依赖 `teacher_` subject 前缀 | P0 | `TEACHER sub=instructor_1` 可上传 `courseId` 对应 `teacherId=instructor_1` |
| REQ-P3-4-P-06 | Bearer `USER sub=teacher_1` 不能通过 subject 命名获得 teacher course metadata 管理权限 | P0 | 上传 course metadata 返回 `FORBIDDEN`，且不写 document/index task |
| REQ-P3-4-P-07 | `GET /api/documents/{documentId}`、`POST /api/documents/{documentId}/reindex`、`GET /api/index-tasks/{taskId}` 的 missing 行为必须基于 explicit admin fact | P0 | Bearer admin missing 返回 `NOT_FOUND`；Bearer `USER sub=admin` missing 返回 `FORBIDDEN` |
| REQ-P3-4-P-08 | 本切片不得新增 DB migration、依赖、API path、DTO 字段或 frontend 改动 | P0 | 文件变更审查通过 |

## 4. Non-Functional Requirements

- 权限判断必须在 backend service 层执行，不能依赖 prompt、frontend 或 header 文案。
- 不得记录 secrets、raw Bearer token、API key 或私密日志到文档。
- 保持现有 Controller response contract，不改变 DTO shape。
- 测试必须覆盖 spoofed header 和 subject-name role confusion。

## 5. Out of Scope

- Formal OAuth2/JWK/Spring Security migration。
- KB-course binding schema。
- RAG query retrieval permission runtime。
- Vector DB、parser、index worker、object storage provider。
- Frontend 页面或 API wrapper。
- 整体 P3-4 完成声明。
