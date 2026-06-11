# ACCEPT-20260610 P3-4 子任务：KB-course binding governance

## Acceptance Summary

Status: ACCEPTED.

P3-4 `KB-course binding governance` 子任务已完成。RAG KB 现在有课程绑定字段和生命周期状态，`BOUND` KB 的读写必须经过 `CourseAccessService`，空 `UNBOUND` KB 首次合法课程文档上传会自动绑定，`CONFLICTED` KB 默认安全封锁，requestId payload conflict 保持 `409 CONFLICT` 优先级。

## Acceptance Criteria

| Criteria | Status | Evidence |
|---|---|---|
| V20 migration 增加 KB course binding 字段、索引、约束和回填逻辑 | PASS | `V20__kb_course_binding_governance.sql`；`SchemaConvergenceMigrationTest` passed。 |
| `KnowledgeBase` entity / DTO 支持 `courseId` 与 `bindingStatus` | PASS | `KnowledgeBase.java`、`KnowledgeBaseBindingStatus.java`、`KnowledgeBaseDtos.java`。 |
| `KnowledgeBaseService` 创建 course-bound KB 时校验课程 manage 权限 | PASS | `KnowledgeBaseService.create(...)` 调用 `requireCourseRead` + `requireCourseManage`；`KnowledgeBaseControllerTest` passed。 |
| `PermissionService` 对 `BOUND` KB 通过 `CourseAccessService` 判定读写 | PASS | `canReadCourseBoundKnowledgeBase(...)` / `canWriteCourseBoundKnowledgeBase(...)`；新增 admin 不绕过回归测试。 |
| `PUBLIC` / owner / explicit permission 不能绕过 BOUND course access | PASS | `PermissionServiceTest`、`RagQueryServiceTest`、`DocumentControllerTest` 覆盖 dropped student / owner public denial / role-confusion denial。 |
| 空 `UNBOUND` KB 首次合法 course document upload 自动绑定 | PASS | `DocumentService.resolveEffectiveCourseScope(...)`；`DocumentControllerTest` passed。 |
| `UNBOUND` 自动绑定有并发保护 | PASS | `DocumentService.lockUnboundKnowledgeBaseForUpload(...)` 使用 `PESSIMISTIC_WRITE`，锁后二次 replay。 |
| 同一 `createdBy + requestId` 不同 payload 返回 `409 CONFLICT` | PASS | `DocumentControllerTest.rejectsSameRequestIdWhenCourseOrChapterMetadataChanges` passed。 |
| RAG query 越权请求不写 `kb_query_log` / `source_citation` 成功记录 | PASS | `RagQueryServiceTest` passed。 |
| 专家 subagent 并行开发/审查 | PASS | Architect / Integration / Security / Test / Final Verification / Documentation Consistency experts 均参与，报告见 `docs/subagents/runs/`。 |
| Evidence / Acceptance / Changelog / Memory 更新 | PASS | 本文件、Evidence、Retro、Changelog、Project/Backend/RAG memory 已更新。 |

补充说明：Final Verification 与 Documentation Consistency 收尾复核报告已记录在 `docs/subagents/runs/`。

## Verification

- Focused permission: `mvn --% -Dtest=PermissionServiceTest test` -> `9 run, 0 failures, 0 errors`.
- Focused document: `mvn --% -Dtest=DocumentControllerTest test` -> `25 run, 0 failures, 0 errors`.
- Adjacent: `mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,RagQueryServiceTest,PermissionServiceTest,SchemaConvergenceMigrationTest test` -> `76 run, 0 failures, 0 errors`.
- Full backend: `mvn test` -> `520 run, 0 failures, 0 errors, 1 skipped`.
- MySQL smoke: attempted, blocked by local environment (`Docker daemon is not available`; MySQL `root` access denied).

## Accepted Limitations / Follow-up

- P3-4 parent is not complete.
- Remaining P3-4 work includes broader class/course matrix, answer-record expansion, SSE production auth strategy, and dev/test legacy fallback cleanup.
- `CONFLICTED` KB currently provides safe blockade and admin KB-level governance access, not a full conflict repair workbench.
- MySQL smoke needs a reachable MySQL 8 environment with valid credentials.
