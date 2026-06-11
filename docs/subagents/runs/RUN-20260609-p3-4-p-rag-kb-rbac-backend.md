# RUN - 20260609 P3-4-P RAG KB roles-first RBAC Backend Expert

## 1. 结论

RAG KB management 当前仍主要是 `currentUserId` / USER permission 模型：

- `KnowledgeBaseController` 未读取 `UserContext.roles()`。
- `DocumentController` 未读取 `UserContext.roles()`。
- `DocumentService` 调用 `CourseAccessService` legacy 签名，课程绑定校验仍可能从 `admin` / `teacher_*` subject 推断角色。
- `DocumentService.scopedMissing(...)` 用 `userId == "admin"` 判断 admin missing 语义。

建议 P3-4-P 做最小 roles-first 调用链收口，不重构 KB schema / parser / index worker / frontend。

## 2. 涉及文件

Controller:

- `backend/src/main/java/com/learningos/rag/api/KnowledgeBaseController.java`
- `backend/src/main/java/com/learningos/rag/api/DocumentController.java`

Service:

- `backend/src/main/java/com/learningos/rag/application/KnowledgeBaseService.java`
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java`
- `backend/src/main/java/com/learningos/rag/application/PermissionService.java`

Tests:

- `backend/src/test/java/com/learningos/rag/api/KnowledgeBaseControllerTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- `backend/src/test/java/com/learningos/rag/application/PermissionServiceTest.java`

## 3. 当前授权链路

- `POST /api/knowledge-bases`：只传 `currentUserId()`；service 创建个人 KB 并授予 OWNER。
- `GET /api/knowledge-bases`：只传 `currentUserId()`；`PermissionService` 按 owner/public/USER permission 过滤。
- `POST /api/knowledge-bases/{kbId}/documents`：只传 `currentUserId()`；`ensureCanWrite` 后调用 legacy `CourseAccessService.requireCourseRead/Manage(userId, ...)`。
- `GET /api/documents/{documentId}` / `GET /api/index-tasks/{taskId}`：missing 语义通过 `userId == "admin"` 决定 `NOT_FOUND` vs `FORBIDDEN`。

## 4. 推荐最小实施范围

1. Controller 改为读取 `UserContext currentUser = currentUserService.currentUser()`，从 roles 派生 admin/teacher facts。
2. `KnowledgeBaseService.listAccessible(...)` 支持 admin role 全局 list。
3. `PermissionService` 增加 role-aware read/write/list overload；旧签名保留给 RAG query 等兼容路径。
4. `DocumentService` 增加/迁移 roles-first 签名，文档 upload/list/detail/reindex/index-task 使用 role-aware KB read/write。
5. `DocumentService.validateCourseChapterScope(...)` 使用 `CourseAccessService` role-aware overload。
6. `scopedMissing(...)` 使用 explicit `currentUserAdmin`。

不建议本切片重构 `kb_permission` 为 role/group permission，也不建议改 API response DTO。

## 5. 不应修改范围

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- parser/vector/index worker/storage/provider
- `/api/rag/query` runtime、Tutor、Evaluation、PromptVersion

## 6. 建议测试

Focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,PermissionServiceTest test
```

Adjacent:

```powershell
mvn --% -Dtest=CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,ChatControllerTest,RagEvaluationControllerTest test
```

Full:

```powershell
mvn test
```
