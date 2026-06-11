# TASK - P3-4 KB-course binding schema and lifecycle governance

## 目标

完成 P3-4 子任务：RAG KB 与 Course 的 schema/lifecycle governance。

## Done Criteria

- [x] V20 migration 增加 KB course binding 字段、索引、约束和回填逻辑。
- [x] `KnowledgeBase` entity / DTO 支持 `courseId` 与 `bindingStatus`。
- [x] `KnowledgeBaseService` 创建 course-bound KB 时校验课程 manage 权限。
- [x] `PermissionService` 对 `BOUND` KB 通过 `CourseAccessService` 判定读写。
- [x] `DocumentService` 强制 KB-course 与 document-course 一致。
- [x] 空 `UNBOUND` KB 首次合法课程文档上传自动绑定，并在绑定前锁定 KB 行。
- [x] 同一 `createdBy + requestId` 不同 payload 返回 `409 CONFLICT`，优先于 KB-course mismatch。
- [x] RAG query 越权请求不写 `kb_query_log` / `source_citation` 成功记录。
- [x] Focused / adjacent / full tests 完成；MySQL smoke 因本机环境限制未通过，已记录。
- [x] Evidence / Acceptance / Changelog / Memory 更新。

## Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=SchemaConvergenceMigrationTest#v20MigrationAddsKbCourseBindingGovernanceColumns test
mvn --% -Dtest=PermissionServiceTest,RagQueryServiceTest test
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest test
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,RagQueryServiceTest,PermissionServiceTest,SchemaConvergenceMigrationTest test
mvn test
```

MySQL smoke（环境可用时）：

```powershell
cd D:\多元agent
powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1
```

## 当前边界

只实现 `kb-course-binding-governance`，不处理剩余 P3-4 父项的 class/course 全矩阵、answer record 扩展或 P3-2 OCR/layout。
