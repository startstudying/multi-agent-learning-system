# EVIDENCE-20260610 P3-4 子任务：KB-course binding governance

## Scope

本证据记录 P3-4 子任务 `kb-course binding governance` 的实现、专家审查、测试和环境限制。

目标：为 RAG Knowledge Base 增加课程绑定事实，并确保 `BOUND` KB 的读写授权统一经过 `CourseAccessService`，防止 `PUBLIC`、owner 或 explicit KB permission 绕过课程权限。

## Implementation Evidence

| Area | Evidence |
|---|---|
| Schema | `backend/src/main/resources/db/migration/V20__kb_course_binding_governance.sql` 增加 `course_id`、`binding_status`、`bound_by`、`bound_at`，并添加 `idx_kb_course_binding`、`idx_kb_document_kb_course_deleted`、`ck_kb_binding_status`、`ck_kb_binding_course_consistency`、`fk_kb_course_binding_course`。 |
| Migration backfill | V20 将无 active course document 的 active KB 保持 `UNBOUND`；单一有效 course document 的 KB 回填为 `BOUND`；混合/无效 course scope 的 KB 标记为 `CONFLICTED`。 |
| Domain / DTO | `KnowledgeBase`、`KnowledgeBaseBindingStatus`、`KnowledgeBaseDtos`、`DocumentDtos` 支持 `courseId`、`bindingStatus`、`boundBy`、`boundAt` 及 document course/chapter metadata 输出。 |
| KB create | `KnowledgeBaseService.create(...)` 在创建 course-bound KB 时调用 `CourseAccessService.requireCourseRead(...)` 和 `requireCourseManage(...)`，并保存 `BOUND/boundBy/boundAt`。 |
| KB permission | `PermissionService` 对 `BOUND` read/write 统一进入 `CourseAccessService`；`PUBLIC`、owner、explicit permission 不再绕过 course access。 |
| Admin path hardening | `PermissionServiceTest.adminCourseBoundKnowledgeBaseStillRequiresCourseAccessPath` 固定 admin 也不能对绑定到不存在 course 的 `BOUND` KB 早返回读写，避免绕过 `CourseAccessService`。 |
| `CONFLICTED` | 非 admin 不能通过 owner/public/course-derived access 读取；admin 只有 KB 级治理读写语义；`DocumentService` 文档上传路径对 `CONFLICTED` 直接拒绝。 |
| UNBOUND lifecycle | `DocumentService` 允许空 `UNBOUND` KB 第一次合法 course document upload 自动绑定为 `BOUND`；已有 active document 的 `UNBOUND` KB 拒绝新增 course metadata。 |
| Concurrency guard | `DocumentService.lockUnboundKnowledgeBaseForUpload(...)` 对 `UNBOUND` 自动绑定使用 `EntityManager.refresh(..., LockModeType.PESSIMISTIC_WRITE)`，并在锁后二次检查 requestId replay。 |
| requestId priority | `DocumentService` 在通过 KB 写权限后先处理 `createdBy + requestId` replay/conflict；同 requestId 不同 payload 返回 `409 CONFLICT`，不被 KB-course mismatch 抢成 `400 VALIDATION_ERROR`。 |
| RAG query | `RagQueryService` 继续在 retrieval / query log / citation 前调用 `PermissionService.requireReadableKbIds(...)`；forbidden query 不写成功业务 artifact。 |

## Expert Review Evidence

| Report | Verdict / Finding |
|---|---|
| `docs/subagents/runs/RUN-20260610-p3-4-kb-course-binding-governance-architect.md` | 采用 KB 表内字段方案，不新增 `kb_course_binding` 表。 |
| `docs/subagents/runs/RUN-20260610-p3-4-kb-course-binding-governance-integration-review.md` | 采用专家并行分析/设计，主 Codex 单线实现与集成。 |
| `docs/subagents/runs/RUN-20260610-p3-4-kb-course-binding-governance-test-final.md` | 指出 requestId payload conflict 应保持 `409 CONFLICT`，不能被 course mismatch 改为 `400`。 |
| `docs/subagents/runs/RUN-20260610-p3-4-kb-course-binding-governance-security-final.md` | 确认未发现 `PUBLIC` / owner / explicit permission 绕过 course access 的直接路径；指出 `UNBOUND` 自动绑定需要 KB 行锁。 |
| Final Verification Expert | 指出 admin 早返回可能绕过 `CourseAccessService`；已通过 `PermissionService` 调整和新增回归测试修复。 |
| Documentation Consistency Expert | 指出旧文档需修正 `UNBOUND`、requestId 优先级、`CONFLICTED` 治理语义；已更新 REQ/SPEC/PLAN/CONTEXT。 |

| `docs/subagents/runs/RUN-20260610-p3-4-kb-course-binding-governance-verification-final.md` | 收尾阶段补充最终验证证据报告；接受 Maven 验证证据，并保留 MySQL smoke 为环境阻塞状态。 |
| `docs/subagents/runs/RUN-20260610-p3-4-kb-course-binding-governance-documentation-consistency-final.md` | 收尾阶段补充文档一致性最终报告；确认当前 memory / planning / changelog 不再把本子任务标为 open。 |

## Verification Commands

### Focused - PermissionService after admin CourseAccess hardening

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=PermissionServiceTest test
```

Result:

```text
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-10T12:52:14+08:00
```

### Focused - Document upload lifecycle / requestId

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DocumentControllerTest test
```

Result:

```text
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-10T12:46:34+08:00
```

### Adjacent - P3-4 KB-course binding matrix

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,RagQueryServiceTest,PermissionServiceTest,SchemaConvergenceMigrationTest test
```

Result:

```text
Tests run: 76, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-10T12:53:15+08:00
```

### Full backend

```powershell
cd D:\多元agent\backend
mvn test
```

Result:

```text
Tests run: 520, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-10T12:55:30+08:00
```

## MySQL Smoke Attempt

Command:

```powershell
cd D:\多元agent
powershell -ExecutionPolicy Bypass -File scripts/mysql-migration-smoke.ps1 -Attempts 1 -DelaySeconds 1
```

Result:

```text
Docker daemon is not available.
Access denied for user 'root'@'localhost' (using password: YES)
MySQL migration smoke test failed after 1 attempts.
```

Verdict: 未通过，原因是本机环境限制。Docker daemon 不可用，且 `127.0.0.1:3306` MySQL root 凭据拒绝访问。未将 MySQL smoke 记为通过。

## Architecture Drift Check After

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只传身份/role facts；Service 层执行 KB/course/document 授权和生命周期治理。 |
| Frontend rules | PASS | 未修改 frontend，未引入前端 LLM 调用或密钥。 |
| Agent / RAG | PASS | RAG query 仍在 retrieval/log/citation 前执行 permission filtering；未改 Agent loop。 |
| Security | PASS | 权限在 backend code 中执行；`BOUND` KB read/write 统一经 `CourseAccessService`；无新增 secrets。 |
| API / Database | PASS | API/DTO/schema 变更已在 PRD/REQ/SPEC/PLAN/TASK/CONTEXT 中记录。 |

## Acceptance Verdict

Verdict: PASS for `P3-4 子任务：KB-course binding governance`.

P3-4 parent remains open.
