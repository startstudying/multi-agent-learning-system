# ACCEPT - P3-4-H RAG Document Course/Chapter Metadata Scope

## 1. 追溯

- PRD：`docs/product/PRD-20260608-rag-document-course-scope.md`
- REQ：`docs/requirements/REQ-20260608-rag-document-course-scope.md`
- SPEC：`docs/specs/SPEC-20260608-rag-document-course-scope.md`
- PLAN：`docs/plans/PLAN-20260608-rag-document-course-scope.md`
- TASK：`docs/tasks/TASK-20260608-rag-document-course-scope.md`
- Evidence：`docs/evidence/EVIDENCE-20260608-rag-document-course-scope.md`

## 2. 验收清单

### 功能验收

- [x] `POST /api/knowledge-bases/{kbId}/documents` 对非空 `courseId` 执行 course manage scope。
- [x] teacher 只能上传自己课程的课程资料元数据。
- [x] student / ordinary user 即使拥有 KB 写权限，也不能伪造课程元数据。
- [x] teacher missing / foreign course 返回 `FORBIDDEN` 且无 `data`。
- [x] admin missing course 返回 `NOT_FOUND`。
- [x] `chapterId` 非空时必须同时提供 `courseId`。
- [x] missing / foreign chapter 返回固定 `VALIDATION_ERROR`。
- [x] 失败请求不创建 `kb_document` / `kb_index_task`。
- [x] 无 course metadata 的通用 KB 上传仍可用。
- [x] requestId replay 与 course/chapter payload 冲突语义保持不变。

### 非功能验收

- [x] 未新增依赖。
- [x] 未新增 DB migration。
- [x] 未修改 frontend。
- [x] 未新增响应字段。
- [x] 权限校验位于 Service 层。
- [x] 失败请求在对象存储之前被拦截。

### 架构验收

- [x] Backend layering 未破坏。
- [x] RAG parser / vector / model provider 未改动。
- [x] 权限在后端落实，不依赖 Prompt。
- [x] 未提交密钥或敏感信息。
- [x] 无数据库 schema 漂移。

### 文档验收

- [x] Evidence 已创建。
- [x] Acceptance 已创建。
- [x] PLAN / TASK 已更新。
- [x] Memory 已更新。
- [x] Changelog 已更新。
- [x] Retrospective 已创建。
- [x] Project-specific skill 已更新。

## 3. 测试摘要

| 测试项 | 结果 | 备注 |
|---|---|---|
| RED 验证 | PASS | 新增测试首次运行失败 6 项，当前实现仍返回 200。 |
| 聚焦测试 | PASS | `mvn --% -Dtest=DocumentControllerTest test`；18 tests，0 failures/errors。 |
| 相邻回归 | PASS | `mvn --% -Dtest=DocumentControllerTest,CourseKnowledgeControllerTest,RagQueryServiceTest,IndexServiceTest test`；58 tests，0 failures/errors。 |
| 后端全量测试 | PASS | `mvn test`；337 tests，0 failures，0 errors，1 skipped。 |

## 4. 遗留问题

| 问题 | 严重程度 | 后续 TASK |
|---|---|---|
| 正式 JWT/RBAC 仍未落地，当前仍是 `X-User-Id` 过渡身份。 | High | 后续 P3-4 真实 RBAC/JWT 切片 |
| broader class/course 权限矩阵仍未完全收口。 | Medium | 后续 P3-4 broader class/course matrix |

## 5. 验收结论

- [x] 通过
- [ ] 有条件通过
- [ ] 不通过

## 6. 签字

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-08 | Accepted |
