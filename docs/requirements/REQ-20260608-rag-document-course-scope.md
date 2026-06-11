# REQ-20260608 P3-4-H RAG Document Course/Chapter Metadata Scope

## 1. 功能需求

| ID | Requirement |
|---|---|
| FR-01 | `POST /api/knowledge-bases/{kbId}/documents` 在 `courseId` 非空时，必须先校验 KB 写权限，再校验课程管理权限。 |
| FR-02 | teacher / `teacher_*` 只能上传带自己课程 `courseId` 的文档。 |
| FR-03 | student / ordinary user 带任意 `courseId` 上传时返回 `FORBIDDEN`。 |
| FR-04 | admin 带任意 existing course 上传成功；admin 带 missing course 返回 `NOT_FOUND`。 |
| FR-05 | 非 admin 对 missing course 与 foreign course 均返回安全 `FORBIDDEN`。 |
| FR-06 | `chapterId` 非空时必须同时提供 `courseId`。 |
| FR-07 | `chapterId` 非空时，该 chapter 必须存在且属于 request `courseId`。 |
| FR-08 | chapter 缺失或 foreign chapter 均返回通用 `VALIDATION_ERROR`，不暴露 chapter 是否存在。 |
| FR-09 | 请求无 `courseId` / `chapterId` 时，保留现有通用 KB 上传行为。 |
| FR-10 | requestId 幂等 hash 必须包含规范化后的 `courseId` / `chapterId`，维持不同 course/chapter payload 冲突语义。 |

## 2. 非功能需求

| ID | Requirement |
|---|---|
| NFR-01 | 不新增依赖。 |
| NFR-02 | 不新增 DB migration。 |
| NFR-03 | 不修改 frontend。 |
| NFR-04 | Controller 只读取参数并委托 Service，权限校验在 Service 层。 |
| NFR-05 | 失败请求不得写入 `kb_document` 或 `kb_index_task`。 |
| NFR-06 | 错误响应不得包含 foreign course/chapter id、课程标题、教师 id 或 KB 内部对象信息。 |

## 3. 权限矩阵

| Actor | No course metadata | Own course | Foreign course | Missing course | Foreign/missing chapter |
|---|---:|---:|---:|---:|---:|
| student / ordinary | Existing KB write rule | `FORBIDDEN` | `FORBIDDEN` | `FORBIDDEN` | `VALIDATION_ERROR` only after course allowed; student stops at `FORBIDDEN` |
| teacher / `teacher_*` | Existing KB write rule | 200 | `FORBIDDEN` | `FORBIDDEN` | `VALIDATION_ERROR` |
| admin | Existing KB write rule | 200 | 200 | `NOT_FOUND` | `VALIDATION_ERROR` |

## 4. 边界

- 本切片只保护上传时写入的 course/chapter metadata。
- 既有文档 detail/list/reindex 仍按 KB read/write 权限控制，不新增 course read gate。
- 若未来新增 `KnowledgeBase.courseId`，需要进一步校验 KB 与 course 的绑定关系；本切片先记录为后续 schema 任务。
