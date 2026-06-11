# EVIDENCE - P3-4-P RAG KB management roles-first RBAC

## 1. 范围

本证据记录 P3-4-P 对 RAG Knowledge Base management 主路径的 roles-first RBAC 收口。

覆盖路径：

- `POST /api/knowledge-bases`
- `GET /api/knowledge-bases`
- `POST /api/knowledge-bases/{kbId}/documents`
- `GET /api/knowledge-bases/{kbId}/documents`
- `GET /api/documents/{documentId}`
- `POST /api/documents/{documentId}/reindex`
- `GET /api/index-tasks/{taskId}`

非目标：

- 不迁移 formal OAuth2 / JWK / Spring Security。
- 不新增 KB-course binding schema。
- 不修改 API path、request DTO、response DTO、DB schema、migration、dependency 或 frontend。
- 不修改 `/api/rag/query` retrieval runtime。
- 不修改 parser、vector、index worker、object storage、model provider 或 Agent runtime。
- 不宣称整个 P3-4 或 backend TODO 完成。

## 2. Subagent / Integration 证据

| 报告 | 结论 |
|---|---|
| `docs/subagents/runs/RUN-20260609-p3-4-p-rag-kb-rbac-backend.md` | 后端专家建议保持 API/DB 合同稳定，通过 role-aware overload 迁移 KB management 主路径。 |
| `docs/subagents/runs/RUN-20260609-p3-4-p-rag-kb-rbac-security.md` | 安全专家要求覆盖 Bearer spoof、subject-name role-confusion、missing/foreign oracle 和 course metadata spoofing。 |
| `docs/subagents/runs/RUN-20260609-p3-4-p-rag-kb-rbac-test.md` | 测试专家建议先写 RED 矩阵，再运行 RAG/auth adjacent 与 full backend 回归。 |
| `docs/subagents/runs/RUN-20260609-p3-4-p-rag-kb-rbac-integration-review.md` | 集成评审确认采用 L1 并行分析 + Main Codex 单线程实现；Controller 只从 `UserContext.roles()` 派生 role facts，Service 执行授权。 |

## 3. 代码证据

| 文件 | 证据 |
|---|---|
| `backend/src/main/java/com/learningos/rag/api/KnowledgeBaseController.java` | Controller 调用 `CurrentUserService.currentUser()`，从 `UserContext.roles()` 派生 `ADMIN` / `TEACHER` facts，并传给 `KnowledgeBaseService.create/listAccessible(...)`。 |
| `backend/src/main/java/com/learningos/rag/api/DocumentController.java` | Upload/list/detail/reindex/index-task detail 路径均读取 `UserContext` 并显式传入 admin/teacher facts。 |
| `backend/src/main/java/com/learningos/rag/application/KnowledgeBaseService.java` | 新增 role-aware `create(...)` / `listAccessible(...)` overload；旧签名保留并默认非 admin/teacher，避免兼容路径被误提权。 |
| `backend/src/main/java/com/learningos/rag/application/DocumentService.java` | 新增 role-aware upload/list/detail/reindex/index-task overload；course metadata 校验调用 role-aware `CourseAccessService.requireCourseRead/requireCourseManage(...)`；`scopedMissing(...)` 使用显式 `currentUserAdmin`。 |
| `backend/src/main/java/com/learningos/rag/application/PermissionService.java` | 新增 role-aware KB list/filter/require/read/write overload；`currentUserAdmin == true` 可 read/write/list 全部 active KB；旧签名保留默认非 admin/teacher。 |
| `backend/src/test/java/com/learningos/rag/api/KnowledgeBaseControllerTest.java` | 覆盖 Bearer admin spoofed header list all active private KB、Bearer admin upload foreign private KB、Bearer teacher no-prefix own-course metadata、Bearer `USER sub=teacher_1` denial。 |
| `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java` | 覆盖 admin missing document/reindex/index task 返回 `NOT_FOUND`，Bearer `USER sub=admin` missing document/reindex/index task 返回 safe `FORBIDDEN`。 |
| `backend/src/test/java/com/learningos/rag/application/PermissionServiceTest.java` | 覆盖 role-aware admin 可 read/write/list active KB，legacy literal `admin` 非 role 不提权。 |

## 4. RED 证据

新增 P3-4-P RAG KB management RBAC 测试后，首次 focused 运行出现预期 RED：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest test
```

观察结果：

```text
Tests run: 26, Failures: 6, Errors: 0
BUILD FAILURE
```

关键失败命中：

- Bearer `ADMIN sub=ops_admin` 仍因旧路径依赖 legacy `currentUserId == "admin"` 而无法 list / upload foreign private KB。
- Bearer `TEACHER sub=instructor_1` 因无 `teacher_` 前缀而无法使用 own-course metadata。
- Bearer `USER sub=teacher_1` 被旧 subject-name inference 错误放行 course metadata。
- Bearer `USER sub=admin` missing document/reindex/index task 得到 admin-like `NOT_FOUND`，形成 role-confusion oracle。

## 5. GREEN / Regression 证据

### 5.1 Controller focused verification

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest test
```

结果：

```text
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 5.2 Permission service focused verification

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=PermissionServiceTest test
```

结果：

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 5.3 RAG adjacent regression

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,ChatControllerTest,RagEvaluationControllerTest test
```

结果：

```text
Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 5.4 Auth / Course adjacent regression

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest,CourseKnowledgeControllerTest test
```

结果：

```text
Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 5.5 Full backend verification

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 426, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-09T16:17:17+08:00
```

说明：第一次 `mvn test` 在 124 秒工具超时前未返回结果，随后以 300 秒超时重新运行并得到上述 `BUILD SUCCESS`。输出中包含 Mockito dynamic agent / JVM sharing warnings，不影响 Maven 测试结果。

## 6. 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| Backend layering | PASS | Controller 只提取当前用户上下文与 role facts；KB/document/course 授权仍在 application service。 |
| Frontend rules | PASS | 未修改 `frontend/**`，未引入前端 LLM 调用或密钥暴露。 |
| Agent / RAG runtime | PASS | 未修改 `/api/rag/query`、retrieval、parser、vector、index worker、model gateway、Agent runtime 或 citation 链路。 |
| Security | PASS | Bearer roles 优先于 spoofed header；management 主路径不再从 subject 字符串推断 admin/teacher；非 admin missing/foreign collapse 到 safe `FORBIDDEN`。 |
| API / Database | PASS | 未新增或修改 API path、request DTO、response DTO、DB schema、migration、dependency。 |

## 7. 限制

- 本切片只完成 RAG KB management 主路径 roles-first RBAC。
- 学生/普通用户 personal KB 创建能力保留，不代表全量课程绑定 KB 模型已完成。
- `/api/rag/query` retrieval runtime 继续使用旧兼容签名，未在本切片迁移。
- broader class/course、formal OAuth2/JWK/Spring Security、更多 legacy `CourseAccessService` caller 仍是后续 P3-4 工作。
- 当前工作区不是 git repository，本证据以文件内容、测试名称与 Maven 输出为准，不依赖 git diff。
