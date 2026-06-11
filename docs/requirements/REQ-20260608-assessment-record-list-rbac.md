# REQ-20260608 P3-4-F Assessment Record List RBAC / Pagination

## 1. Functional Requirements

### REQ-1 Answer list endpoint

新增：

```http
GET /api/assessment/answers
```

查询参数：

| Param | Required | Semantics |
|---|---:|---|
| `learnerId` | No | student 不允许查询他人；teacher/admin 可用于过滤。 |
| `courseId` | No for student/admin, Yes for teacher | teacher 必须提供，用于课程 scope。student/admin 可选过滤。 |
| `page` | No | 默认 `0`，必须 `>= 0`。 |
| `size` | No | 默认 `20`，允许 `1..50`。 |

### REQ-2 Wrong-question list endpoint

新增：

```http
GET /api/assessment/wrong-questions
```

查询参数同 answer list。

### REQ-3 Student scope

- student 未传 `learnerId` 时默认查询自己。
- student 传 `learnerId != currentUserId` 返回 `FORBIDDEN` 且无 `data`。
- student 传 `courseId` 时必须对该 course 有 active enrollment，否则返回 `FORBIDDEN` 且无 `data`。

### REQ-4 Teacher scope

- teacher / `teacher_*` list 必须提供 `courseId`。
- teacher 只能查询自己课程。
- teacher 列表只返回该 course active enrollment learner 的记录。
- teacher 若传 `learnerId`，该 learner 必须在 course active enrollment 中；否则返回空列表，不泄露外部 learner 是否存在。
- teacher 对 missing/foreign course 返回 `FORBIDDEN` 且无 `data`。

### REQ-5 Admin scope

- admin 可全局分页查询。
- admin 可按 `learnerId` / `courseId` 过滤。
- admin 传 missing `courseId` 返回 `NOT_FOUND`。

### REQ-6 Safe response

Answer list item 不返回原始 `answer`，不返回：

- `requestId`
- `requestHash`
- `responseJson`
- `payloadJson`
- raw provider errors
- internal event payload

Wrong-question list item 不返回 internal payload / raw response。

列表 summary 也不返回 `gradingResultId`、`causeAnalysis`、`replanRecordId`，这些内部关联或长文本字段仅保留在受详情权限保护的接口中。

### REQ-7 Pagination

- `page < 0` 或 `size < 1 || size > 50` 返回 `VALIDATION_ERROR`。
- 响应包含 `items`、`page`、`size`、`totalElements`、`totalPages`。

## 2. Non-functional Requirements

- 不新增依赖。
- 不新增 DB migration。
- 不修改 frontend。
- Controller 只处理 HTTP 参数和当前用户，权限在 Service 层。
- 需要 focused / adjacent / full backend Maven 验证。

## 3. Out of Scope

- JWT/RBAC claim。
- answer/wrong-question 导出。
- grading evaluation course scope。
- assessment 记录 `courseId` 归一化。
