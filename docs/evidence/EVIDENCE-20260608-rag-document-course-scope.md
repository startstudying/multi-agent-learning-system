# EVIDENCE - P3-4-H RAG Document Course/Chapter Metadata Scope

## 1. 追溯

- PRD：`docs/product/PRD-20260608-rag-document-course-scope.md`
- REQ：`docs/requirements/REQ-20260608-rag-document-course-scope.md`
- SPEC：`docs/specs/SPEC-20260608-rag-document-course-scope.md`
- PLAN：`docs/plans/PLAN-20260608-rag-document-course-scope.md`
- TASK：`docs/tasks/TASK-20260608-rag-document-course-scope.md`
- Context Pack：`docs/context/CONTEXT-20260608-rag-document-course-scope.md`
- 日期：2026-06-08

## 2. 实现内容

本切片收口 `POST /api/knowledge-bases/{kbId}/documents` 的 course/chapter 元数据写入权限：

- `DocumentService.upload(...)` 在 requestId/hash/replay/storage/save/index 之前执行 metadata scope 校验。
- `courseId` 非空时复用 `CourseAccessService.requireCourseRead(...)` + `requireCourseManage(...)`。
- teacher 只能上传 own course 的课程资料元数据。
- student 即使拥有 KB 写权限且已 enrolled，也不能上传课程元数据。
- admin missing course 返回 `NOT_FOUND`。
- `chapterId` 非空但缺少 `courseId` 返回 `VALIDATION_ERROR`。
- missing / foreign chapter 统一返回固定 `VALIDATION_ERROR`，不区分枚举信号。
- requestId hash 保持包含归一化后的 `courseId` / `chapterId`，不同元数据仍冲突。

未新增依赖、未新增 migration、未改 frontend、未改 DTO 响应字段。

## 3. 变更文件

| 文件 | 操作 | 摘要 |
|---|---|---|
| `backend/src/main/java/com/learningos/rag/application/DocumentService.java` | 修改 | 注入 `CourseAccessService` / `ChapterRepository`，新增 course/chapter scope 校验并前移到存储前。 |
| `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java` | 修改 | 补齐 teacher/student/admin、chapter missing/foreign、requestId 冲突等权限矩阵测试。 |
| `docs/subagents/runs/RUN-20260608-rag-document-course-scope-backend.md` | 新增 | Backend/RAG 专家分析。 |
| `docs/subagents/runs/RUN-20260608-rag-document-course-scope-security.md` | 新增 | Security & Quality 分析。 |
| `docs/subagents/runs/RUN-20260608-rag-document-course-scope-integration.md` | 新增 | 集成决策。 |

## 4. TDD RED 证据

先仅增加测试后运行：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentControllerTest test
```

首次结果：

- BUILD FAILURE
- Tests run: 17
- Failures: 6
- Errors: 0
- Skipped: 0

失败点全部来自当前实现仍返回 `200 OK`，而新增测试期望 `403/404/400/409`。

## 5. GREEN / 回归验证

### 5.1 聚焦测试

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentControllerTest test
```

结果：

- BUILD SUCCESS
- Tests run: 18
- Failures: 0
- Errors: 0
- Skipped: 0

### 5.2 相邻回归

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentControllerTest,CourseKnowledgeControllerTest,RagQueryServiceTest,IndexServiceTest test
```

结果：

- BUILD SUCCESS
- Tests run: 58
- Failures: 0
- Errors: 0
- Skipped: 0

### 5.3 全量后端测试

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- BUILD SUCCESS
- Tests run: 337
- Failures: 0
- Errors: 0
- Skipped: 1

### 5.4 最终收口复验

文档状态收口后再次运行同一组验证：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentControllerTest test
mvn --% -Dtest=DocumentControllerTest,CourseKnowledgeControllerTest,RagQueryServiceTest,IndexServiceTest test
mvn test
```

结果：

- `DocumentControllerTest`：BUILD SUCCESS；Tests run: 18，Failures: 0，Errors: 0，Skipped: 0。
- 相邻回归：BUILD SUCCESS；Tests run: 58，Failures: 0，Errors: 0，Skipped: 0。
- 全量后端：BUILD SUCCESS；Tests run: 337，Failures: 0，Errors: 0，Skipped: 1。
- 复验时间：2026-06-08 19:28-19:32 Asia/Shanghai。

## 6. 代码审查闭环

只读代码审查指出一个验收覆盖缺口：缺少“missing chapter 返回通用 `VALIDATION_ERROR`”的显式测试。

已补充 `rejectsMissingChapterWithGenericValidationError()`，并再次通过聚焦、相邻和全量验证。

## 7. 安全与架构检查

- 权限在 Service 层执行，不依赖 Prompt。
- 校验在 `storageService.store(...)` 之前执行，失败请求不会留下对象存储副作用。
- 非 admin 对 missing / foreign chapter 不暴露对象 id。
- 未修改 frontend、模型 provider、parser、vector、schema 或迁移。

## 8. 结论

本切片已完成并验证通过。
