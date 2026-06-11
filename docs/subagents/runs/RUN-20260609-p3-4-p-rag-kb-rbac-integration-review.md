# RUN - 20260609 P3-4-P RAG KB roles-first RBAC Integration Review

## 1. 集成结论

采纳 Backend / Security / Test 三方共同结论：P3-4-P 做 RAG KB management 主路径 roles-first RBAC，不做 schema、frontend、parser/index worker、formal OAuth2/JWK 扩展。

## 2. 最终范围

纳入：

- `POST /api/knowledge-bases`
- `GET /api/knowledge-bases`
- `POST /api/knowledge-bases/{kbId}/documents`
- `GET /api/knowledge-bases/{kbId}/documents`
- `GET /api/documents/{documentId}`
- `POST /api/documents/{documentId}/reindex`
- `GET /api/index-tasks/{taskId}`

核心变更：

- Controller 从 `UserContext.roles()` 派生 admin/teacher facts。
- `PermissionService` 增加 role-aware overload，admin role 可全局 read/write/list KB。
- `DocumentService` 使用 role-aware KB permission 和 role-aware `CourseAccessService` overload。
- `DocumentService.scopedMissing(...)` 使用 explicit admin fact。

## 3. 明确不改

- 不移除学生/普通用户个人 KB 创建能力。
- 不新增 KB-course 绑定 schema。
- 不新增 API path / DTO 字段。
- 不改 frontend。
- 不改 parser/vector/index worker/storage/model provider。
- 不改 `/api/rag/query` retrieval runtime。

## 4. 冲突解决

Security/Test 专家提出“student 是否应禁止创建 KB”。集成决策：本切片保留 personal KB 创建，因为：

- 现有学生端使用 `createKnowledgeBase(...)`。
- 旧 API docs 定义 `POST /api/knowledge-bases` 是创建 KB，不是 admin-only 管理面。
- P3-4-P 的安全缺口主要在 roles-first admin/teacher facts、document course metadata、missing oracle，而不是个人 KB 创建。

后续如要把课程 KB 与个人 KB 分离，需要独立 PRD/schema/API 设计。

## 5. 实施模式

- Subagents：L1 并行分析已完成。
- Implementation：单 Codex，单任务，避免并发改同一 RAG 文件。
- Review：主 Codex 集成评审，测试与 Evidence 证明。
