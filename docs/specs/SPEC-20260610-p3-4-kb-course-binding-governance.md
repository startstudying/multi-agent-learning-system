# SPEC - P3-4 KB-course binding schema and lifecycle governance

## 1. Schema

新增 migration：

```text
backend/src/main/resources/db/migration/V20__kb_course_binding_governance.sql
```

`kb_knowledge_base` 新增：

| Column | Type | Notes |
|---|---|---|
| `course_id` | `varchar(80)` | nullable，绑定课程 |
| `binding_status` | `varchar(40)` | `UNBOUND` / `BOUND` / `CONFLICTED` |
| `bound_by` | `varchar(120)` | 绑定操作者 |
| `bound_at` | `datetime(6)` | 绑定时间 |

新增：

- `idx_kb_course_binding(course_id, binding_status, deleted_at)`
- `idx_kb_document_kb_course_deleted(kb_id, course_id, deleted_at)`
- `ck_kb_binding_status`
- `ck_kb_binding_course_consistency`
- `fk_kb_course_binding_course`

## 2. Migration backfill

对 active KB：

- 没有 active course document：保持 `UNBOUND`。
- active documents 全部属于同一个有效 course：回填为 `BOUND`。
- active documents 混合多个 course、空/非空混合、或 course 不存在：标记 `CONFLICTED`。

## 3. API contract

### `POST /api/knowledge-bases`

Request 增加可选：

```json
{
  "courseId": "crs_xxx"
}
```

Response 增加：

```json
{
  "courseId": "crs_xxx",
  "bindingStatus": "BOUND",
  "boundAt": "2026-06-10T..."
}
```

### Document response

`DocumentStatusResponse` 增加 `courseId` / `chapterId`，用于验证上传后的治理元数据。

## 4. Service contract

### `KnowledgeBaseService`

`create(userId, admin, teacher, request)`：

1. normalize `courseId`。
2. `courseId == null`：创建 `UNBOUND` KB。
3. `courseId != null`：
   - `CourseAccessService.requireCourseRead(...)`
   - `CourseAccessService.requireCourseManage(...)`
   - 保存 `courseId / BOUND / boundBy / boundAt`。

### `PermissionService`

`canReadKnowledgeBase(...)`：

- `BOUND`：必须通过 `CourseAccessService.requireCourseRead(...)`。
- `UNBOUND`：保留现有 owner / `PUBLIC` / `kb_permission`。
- `CONFLICTED`：仅 admin 可做 KB 级读取；非 admin 不得通过 owner / `PUBLIC` / explicit permission / course-derived access 读取。

`canWriteKnowledgeBase(...)`：

- `BOUND`：必须通过 `CourseAccessService.requireCourseManage(...)`。
- `UNBOUND`：保留 owner / explicit write。
- `CONFLICTED`：仅 admin 可做 KB 级治理写权限；文档上传仍由 `DocumentService` 直接拒绝。

### `DocumentService`

`upload(...)`：

1. load active KB。
2. `ensureCanWrite(...)`。
3. normalize `courseId/chapterId/requestId`。
4. 若 `requestId` 非空，先按 `createdBy + requestId` 尝试 replay / conflict 判断：
   - hash 相同：重放首次响应。
   - hash 不同：返回 `CONFLICT`。
   - 对 `BOUND` KB，当前请求省略 `courseId` 时使用既有 document course 计算 replay hash，支持同一请求二次重放。
5. 若 KB 是 `UNBOUND`，通过 `PESSIMISTIC_WRITE` 刷新并锁定 KB 行，再二次检查 replay，避免等待锁期间 requestId 状态漂移。
6. resolve effective course:
   - `BOUND`：使用 KB `courseId`；普通新请求显式 `courseId` 不一致则 `VALIDATION_ERROR`。
   - `UNBOUND` 且请求 `courseId == null`：保持不绑定；如只有 `chapterId` 则 `VALIDATION_ERROR`。
   - `UNBOUND` 且请求 `courseId != null`：若 KB 还没有 active document，则当前用户必须 `requireCourseRead + requireCourseManage`，随后自动绑定 KB 为 `BOUND`。
   - `UNBOUND` 且已有 active document：拒绝 course metadata 上传，返回 `VALIDATION_ERROR`。
   - `CONFLICTED`：拒绝文档上传写入，即使 admin 已有 KB 级治理权限也不通过上传路径修复冲突。
7. 校验 chapter 属于 effective course。
8. 执行 storage / save `kb_document` / create `kb_index_task`。

## 5. Error semantics

| Scenario | Error |
|---|---|
| teacher foreign course-bound create | `FORBIDDEN` |
| student course-bound create | `FORBIDDEN` |
| same `createdBy + requestId` with different payload | `CONFLICT` |
| bound KB upload mismatched `courseId` without requestId replay hit | `VALIDATION_ERROR` |
| unbound existing-document KB upload `courseId` / `chapterId` | `VALIDATION_ERROR` |
| unbound empty KB upload `chapterId` without `courseId` | `VALIDATION_ERROR` |
| conflicted KB document upload | `FORBIDDEN` |
| forbidden RAG query | `FORBIDDEN` and no success artifacts |

## 6. Architecture drift check

| Check | Expected |
|---|---|
| Backend layering | Controller passes identity facts; Service owns authorization |
| Agent / RAG | Query permission filtering before retrieval/log/citation |
| Security | No Prompt-based permission; no secrets; no new dependency |
| API / DB | DTO/schema changes documented here |
