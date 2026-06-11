# SPEC - P3-4-P RAG KB management roles-first RBAC

## 1. 追踪

- PRD: `docs/product/PRD-20260609-p3-4-p-rag-kb-rbac.md`
- REQ: `docs/requirements/REQ-20260609-p3-4-p-rag-kb-rbac.md`
- Subagent reports:
  - `docs/subagents/runs/RUN-20260609-p3-4-p-rag-kb-rbac-backend.md`
  - `docs/subagents/runs/RUN-20260609-p3-4-p-rag-kb-rbac-security.md`
  - `docs/subagents/runs/RUN-20260609-p3-4-p-rag-kb-rbac-test.md`
  - `docs/subagents/runs/RUN-20260609-p3-4-p-rag-kb-rbac-integration-review.md`

## 2. 现状

当前 RAG KB management 主路径的 Controller 多数只向 Service 传递 `currentUserId`。Service 内部再通过 owner/public/permission 或 legacy subject 推断完成授权。这与 Bearer JWT roles-first 模式不一致，尤其会影响：

- `ADMIN sub=ops_admin` 不是 `admin` 时的全局管理能力。
- `USER sub=teacher_1` 被 legacy `teacher_*` 推断为 teacher 的风险。
- `USER sub=admin` 在 missing document/index task 上得到 admin `NOT_FOUND` 的风险。

## 3. API Contract

本切片不新增、不删除、不修改 API path、request DTO 或 response DTO。

| API | Contract Change | Authorization Change |
|---|---|---|
| `POST /api/knowledge-bases` | 无 | 使用 `CurrentUserService.currentUser()` 的 subject 创建 owner；不依赖 spoofed header |
| `GET /api/knowledge-bases` | 无 | `ADMIN` role 可列出全部 active KB；其他用户沿用 owner/public/USER permission |
| `POST /api/knowledge-bases/{kbId}/documents` | 无 | KB write 和 course metadata scope 使用 explicit role facts |
| `GET /api/knowledge-bases/{kbId}/documents` | 无 | KB read 使用 explicit role facts |
| `GET /api/documents/{documentId}` | 无 | document 所属 KB read 使用 explicit role facts；missing response 使用 explicit admin fact |
| `POST /api/documents/{documentId}/reindex` | 无 | document 所属 KB write 使用 explicit role facts；missing response 使用 explicit admin fact |
| `GET /api/index-tasks/{taskId}` | 无 | task -> document -> KB read 使用 explicit role facts；missing response 使用 explicit admin fact |

## 4. Role Facts

Controller 必须按以下规则计算：

```java
UserContext currentUser = currentUserService.currentUser();
String currentUserId = currentUser.userId();
boolean currentUserAdmin = currentUser.roles().contains("ADMIN");
boolean currentUserTeacher = currentUser.roles().contains("TEACHER");
```

禁止在本切片新增入口中使用 `CurrentUserService.isAdmin()` 或 `CurrentUserService.isTeacherUser()` 作为 Bearer roles-first 判断来源，因为这些 helper 在 dev/test 下保留 legacy userId inference。

## 5. Service Contract

### 5.1 `KnowledgeBaseService`

新增或迁移到 role-aware 主签名：

```java
public KnowledgeBase create(String userId, boolean currentUserAdmin, boolean currentUserTeacher, CreateKnowledgeBaseRequest request)
public List<KnowledgeBase> listAccessible(String userId, boolean currentUserAdmin, boolean currentUserTeacher)
```

兼容要求：

- 如旧签名仍被测试或旧路径使用，可保留旧签名并委托到默认非 admin/teacher 的 role-aware overload。
- `create(...)` 不应因 `currentUserAdmin/currentUserTeacher` 改变 owner 语义；owner 仍为 `userId`。

### 5.2 `PermissionService`

新增 role-aware overload：

```java
public List<KnowledgeBase> listAccessibleKnowledgeBases(String userId, boolean currentUserAdmin, boolean currentUserTeacher)
public List<String> filterAllowedKbIds(String userId, boolean currentUserAdmin, boolean currentUserTeacher, Collection<String> requestedKbIds)
public List<String> requireReadableKbIds(String userId, boolean currentUserAdmin, boolean currentUserTeacher, Collection<String> requestedKbIds)
public boolean canReadKnowledgeBase(String userId, boolean currentUserAdmin, boolean currentUserTeacher, String kbId)
public boolean canWriteKnowledgeBase(String userId, boolean currentUserAdmin, boolean currentUserTeacher, String kbId)
```

语义：

- `currentUserAdmin == true`：可 list/read/write 全部 `KnowledgeBase.Status.ACTIVE` KB。
- 非 admin：保持现有 owner、`Visibility.PUBLIC`、`kb_permission subjectType=USER` 规则。
- `currentUserTeacher` 暂不直接放大 KB 权限；教师仍依赖 owner/permission，课程 metadata 另由 `CourseAccessService` 校验。
- 旧签名必须保留，供 `/api/rag/query` 等兼容路径继续使用。

### 5.3 `DocumentService`

新增或迁移到 role-aware 主签名：

```java
public DocumentUploadResponse upload(String userId, boolean currentUserAdmin, boolean currentUserTeacher, String kbId, MultipartFile file, String courseId, String chapterId, String requestId)
public KbDocument getDocument(String userId, boolean currentUserAdmin, boolean currentUserTeacher, String documentId)
public List<KbDocument> listDocuments(String userId, boolean currentUserAdmin, boolean currentUserTeacher, String kbId)
public DocumentUploadResponse reindex(String userId, boolean currentUserAdmin, boolean currentUserTeacher, String documentId)
public IndexTaskDetailResponse getIndexTask(String userId, boolean currentUserAdmin, boolean currentUserTeacher, String taskId)
```

兼容要求：

- 如旧签名仍被旧测试或非 HTTP 路径使用，可保留旧签名并委托到默认非 admin/teacher 的 role-aware overload。
- `validateCourseChapterScope(...)` 必须使用 `CourseAccessService` role-aware overload：
  - `requireCourseRead(currentUserId, currentUserAdmin, currentUserTeacher, courseId)`
  - `requireCourseManage(currentUserId, currentUserAdmin, currentUserTeacher, course)`
- `scopedMissing(...)` 必须基于 `currentUserAdmin`，不得基于 `"admin".equals(userId)`。

## 6. Authorization Matrix

| Scenario | Expected |
|---|---|
| Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id=student` list KB | 返回全部 active KB |
| Bearer `ADMIN sub=ops_admin` upload to foreign private KB | 允许 |
| Bearer `TEACHER sub=instructor_1` upload metadata for `Course.teacherId=instructor_1` | 允许 |
| Bearer `USER sub=teacher_1` upload metadata for `Course.teacherId=teacher_1` | `FORBIDDEN` |
| Bearer `ADMIN sub=ops_admin` missing document/index task | `NOT_FOUND` |
| Bearer `USER sub=admin` missing document/index task | `FORBIDDEN` |
| Non-admin foreign private KB document list/read/reindex/index task | `FORBIDDEN` |

## 7. Persistence / Dependency / Frontend

- DB migration：无。
- 新依赖：无。
- Frontend：无变更。
- Parser/vector/index worker/storage/model provider：无变更。

## 8. Architecture Drift Pre-check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 提取 identity facts；Service 执行授权 |
| Frontend rules | PASS | 不改 frontend |
| Agent / RAG rules | PASS | 不改 retrieval/generation；只收口 management 权限 |
| Security | PASS | 无 secrets；无 dependency |
| API / Database | PASS | 无 API/DB contract change |
