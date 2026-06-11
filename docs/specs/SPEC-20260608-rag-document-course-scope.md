# SPEC-20260608 P3-4-H RAG Document Course/Chapter Metadata Scope

## 1. API

```text
POST /api/knowledge-bases/{kbId}/documents
Content-Type: multipart/form-data
```

请求参数：

| Field | Required | Notes |
|---|---:|---|
| `file` | yes | 现有行为不变 |
| `courseId` | no | 非空时必须通过 course manage scope |
| `chapterId` | no | 非空时必须属于 request `courseId` |
| `requestId` | no | 现有幂等行为不变 |

## 2. Service 行为

`DocumentService.upload(...)` 顺序：

```text
load active KB
-> ensureCanWrite(userId, kbId)
-> normalize courseId / chapterId
-> validateCourseChapterScope(userId, courseId, chapterId)
-> normalize requestId
-> request hash / replay / store file / save KbDocument / create index task
```

必须在 `storageService.store(...)` 之前完成 course/chapter 校验，避免失败请求写对象存储或数据库。

## 3. Course / Chapter 校验

新增或复用：

```java
CourseAccessService.requireCourseManage(currentUserId, course)
ChapterRepository.findById(chapterId)
```

规则：

1. `courseId` blank：
   - `chapterId` blank：跳过 course/chapter scope。
   - `chapterId` 非 blank：`VALIDATION_ERROR`，message 使用 `courseId is required when chapterId is provided`。
2. `courseId` 非 blank：
   - admin missing course：`NOT_FOUND`。
   - teacher/student missing/foreign course：`FORBIDDEN`。
   - allowed course：继续 chapter 校验。
3. `chapterId` 非 blank：
   - chapter 不存在或 `chapter.courseId != courseId`：`VALIDATION_ERROR`。
   - message 固定为 `chapterId must belong to request course`。

## 4. 不变行为

- `KbDocument.courseId` / `chapterId` 字段继续保存请求规范化值。
- 无 course metadata 的通用 KB 上传继续可用。
- `requestHash` 继续包含 `courseId` / `chapterId`。
- `GET /api/documents/{documentId}`、`POST /api/documents/{documentId}/reindex`、`GET /api/index-tasks/{taskId}` 不改合同。
- 不新增响应字段。

## 5. 错误语义

| Scenario | HTTP | ErrorCode | data |
|---|---:|---|---|
| no KB write | 403 | `FORBIDDEN` | absent |
| teacher foreign/missing course | 403 | `FORBIDDEN` | absent |
| student with courseId | 403 | `FORBIDDEN` | absent |
| admin missing course | 404 | `NOT_FOUND` | absent |
| chapterId without courseId | 400 | `VALIDATION_ERROR` | absent |
| missing/foreign chapter | 400 | `VALIDATION_ERROR` | absent |
| requestId conflict | 409 | `CONFLICT` | absent |

## 6. 测试要求

新增/更新 `DocumentControllerTest`：

- teacher own-course chapter upload persists metadata。
- teacher foreign course returns `FORBIDDEN` and creates no document/index task。
- teacher missing course returns `FORBIDDEN` and response does not echo requested course。
- student with courseId returns `FORBIDDEN`。
- admin missing course returns `NOT_FOUND`。
- chapterId without courseId returns `VALIDATION_ERROR`。
- chapter from another course returns generic `VALIDATION_ERROR`。
- no course metadata upload still works。
- requestId replay with same course/chapter still replays；different course/chapter conflicts。

## 7. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 委托；Service 执行 KB/course/chapter 校验。 |
| Frontend rules | PASS | 不改 frontend。 |
| Agent / RAG rules | PASS | 不改 Agent/model/retrieval；RAG metadata 写入更可信。 |
| Security | PASS | 权限在后端代码，不依赖 Prompt。 |
| API / Database | PASS | 不新增 endpoint，不新增 schema。 |
