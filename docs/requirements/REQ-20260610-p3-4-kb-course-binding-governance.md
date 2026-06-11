# REQ - P3-4 KB-course binding schema and lifecycle governance

## 功能需求

### R1 KB 绑定字段

`kb_knowledge_base` 必须保存：

- `course_id`
- `binding_status`
- `bound_by`
- `bound_at`

`binding_status` 至少支持：

- `UNBOUND`
- `BOUND`
- `CONFLICTED`

### R2 创建 course-bound KB

`POST /api/knowledge-bases` 支持可选 `courseId`。

- `courseId` 为空：创建 `UNBOUND` KB。
- `courseId` 非空：当前用户必须能 manage 该课程。
- Bearer `TEACHER` 使用 token subject 和 roles。
- Bearer `USER sub=teacher_1/admin` 不获得 teacher/admin 语义。

### R3 course-bound KB 读权限

对 `BOUND` KB：

- admin 可读。
- course teacher 可读。
- active enrolled student 可读。
- owner / `PUBLIC` / `kb_permission` 不能单独绕过课程读权限。

对 `UNBOUND` KB：保留 owner / `PUBLIC` / `kb_permission` 原语义。

对 `CONFLICTED` KB：非 admin 不可读写；admin 可做 KB 级安全治理读写，但当前任务不提供冲突修复工作台。

### R4 course-bound KB 写权限

对 `BOUND` KB：

- admin 可写。
- course teacher 可写。
- owner / `PUBLIC` / explicit write 不能单独绕过课程 manage 权限。

对 `UNBOUND` KB：保留 owner / explicit write 原语义。空 `UNBOUND` KB 的第一次合法课程文档上传可以自动绑定为 `BOUND`；已有 active document 的 `UNBOUND` KB 不允许再通过文档上传新增 course metadata。

对 `CONFLICTED` KB：文档上传路径必须拒绝，即使 admin 也不能通过上传新文档来隐式修复冲突。

### R5 文档上传一致性

- `BOUND` KB 上传文档时，文档 `courseId` 必须等于 KB `courseId`。
- `BOUND` KB 上传时省略 `courseId`，服务层使用 KB `courseId`。
- 空 `UNBOUND` KB 首次上传带 `courseId` 的文档时，当前用户必须能 read + manage 该课程，服务层自动写入 KB `courseId / BOUND / boundBy / boundAt`。
- `UNBOUND` KB 上传只有 `chapterId` 但没有 `courseId` 时返回 `VALIDATION_ERROR`。
- 已有 active document 的 `UNBOUND` KB 不允许再上传带 `courseId` / `chapterId` 的文档。
- `chapterId` 必须属于最终课程。
- 拒绝路径不得写 object storage、`kb_document` 或 `kb_index_task`。

### R6 requestId 幂等优先级

文档上传在通过 KB 写权限校验后，若同一 `createdBy + requestId` 已存在：

- payload hash 相同：重放首次响应。
- payload hash 不同：返回 `409 CONFLICT`。
- 即使不同 payload 同时会触发 KB-course mismatch，也必须优先返回 requestId payload conflict，避免把幂等冲突误报为普通 `VALIDATION_ERROR`。

### R7 RAG query 前置授权

`PermissionService.requireReadableKbIds(...)` 必须统一执行 course-bound 授权。

- mixed allowed + forbidden `kbIds` 整体拒绝。
- forbidden query 不得写 `kb_query_log` / `source_citation` 成功记录。
- requestId replay 前必须重新执行当前权限检查。

## 非功能需求

- 不新增依赖。
- 不把权限逻辑放入 Controller。
- 不在 forbidden response 中泄露 foreign `kbId/courseId/documentId/indexTaskId`。
- MySQL migration 必须可从空库执行。
