# TASK-20260611-p3-4-teacher-permission-residual-sampling-matrix

## 1. 任务名称

P3-4 子任务：teacher permission residual sampling matrix

## 2. 任务类型

后端权限 / 教师端授权数据 / 残余渗透测试抽样矩阵补强。

## 3. 目标

在不修改生产代码、REST API 合同、数据库 schema、依赖、部署或前端的前提下，补齐 P3-4 剩余教师端数据权限抽样：

- Bearer `TEACHER` 的 KB list 只能看到 token subject 可读的课程绑定 KB，不能因 `PUBLIC` 或 spoofed `X-User-Id` 泄漏 foreign course-bound KB。
- Bearer `TEACHER` 不能 reindex foreign course-bound document；拒绝响应不泄露 document/index task 元数据，且不产生新 index task。
- 教师 class summary 的 pending reviews 只聚合本课程 review，不泄漏 foreign review/resource/task/title。

## 4. Skill Selection Report

### Task Type

权限 / 安全测试矩阵补强；P3-4 父计划下的 S 级语义子任务。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目要求所有 feature/fix/enhancement 走 S/M/L 工作流、文档、验证、Evidence、Memory 闭环。 |
| `security-review` | 本任务覆盖 RBAC、IDOR、header spoofing、防枚举、拒绝无副作用。 |
| `object-scope-authorization` | 验证 KB/document/class summary/review 的对象归属、课程范围和响应去敏。 |
| `auth-context-boundary` | 验证 Bearer 优先、`X-User-Id` spoofing 不提升权限、角色事实来自 `UserContext.roles()`。 |
| `test-driven-development` | 本任务以测试钉子补强为主；若 RED 暴露缺陷，再升级并修生产代码。 |
| `verification-before-completion` | 完成前必须记录实际运行的 focused / adjacent / full 验证证据。 |

### Missing Skills

无。

### GitHub Research Needed

No。本任务是项目内现有权限矩阵抽样，不新增依赖或外部模式。

### New Project-Specific Skill To Create

暂不创建。

## 5. Size Classification

Size：S - Small Slice / Fast Lane。

原因：

- 只补 3 个 controller integration test 类的高价值抽样断言。
- 不改变 API path、请求/响应 DTO、数据库 schema、依赖、部署、前端或 Agent/RAG runtime。
- 预计无生产代码修改；若新增测试失败且暴露生产权限缺陷，停止并升级为 M。

Required Documents：

- 本 mini TASK，内嵌 Context Pack。
- 完成后创建 combined Evidence/Acceptance。

Can Skip：

- PRD
- REQ
- SPEC
- PLAN
- standalone Context Pack
- standalone Acceptance

Upgrade Trigger：

- 任一新增测试 RED 且需要修改 `backend/src/main/**`。
- 发现需要改变 REST API/DTO/schema/dependency/frontend-backend contract。
- 任务扩展到 3 个以上生产模块或正式 production streaming 设计。

## 6. Subagent Decision

Use Subagents：Yes。

原因：用户明确要求专家 subagent 并行开发；本任务涉及权限和测试矩阵，适合 L1 并行专家分析。

Parallelism Level：L1 Parallel Analysis。

Selected Subagents：

- Security Expert：复用 `019eb226-dccf-7161-9530-1b07d8846ae4`，只读审查残余高价值权限场景。
- Test Expert：复用 `019eb227-1d93-7093-bbdb-caea544879ba`，只读设计最小 MockMvc regression set；报告已返回并落盘。
- Backend Integration / Architect：复用 `019eb227-5db9-72f2-b7b4-f0293e658bb5`，只读判断 S 级边界和 Context Pack。

Implementation Mode：Single Codex implementation。

不使用并行实现的原因：

- 写入集中在 3 个测试类和文档。
- 多 worker 同时改测试类会增加冲突和重复风险。

专家报告：

- `docs/subagents/runs/RUN-20260611-p3-4-teacher-permission-residual-sampling-matrix-security.md`
- `docs/subagents/runs/RUN-20260611-p3-4-teacher-permission-residual-sampling-matrix-architect.md`
- `docs/subagents/runs/RUN-20260611-p3-4-teacher-permission-residual-sampling-matrix-test.md`

## 7. Embedded Context Pack

### 7.1 当前边界

本子任务只做教师端权限残余抽样测试补强。除非新增测试暴露真实缺陷，否则不修改生产代码。

### 7.2 已读上下文

- `AGENTS.md`
- `.agents/skills/feature-development-workflow/SKILL.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/harness/TEST_COMMANDS.md`
- `docs/planning/backend-architecture-todolist.md`
- `backend/src/test/java/com/learningos/rag/api/KnowledgeBaseControllerTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `backend/src/main/java/com/learningos/rag/application/PermissionService.java`
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`

### 7.3 允许修改文件

- `backend/src/test/java/com/learningos/rag/api/KnowledgeBaseControllerTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `docs/tasks/TASK-20260611-p3-4-teacher-permission-residual-sampling-matrix.md`
- `docs/subagents/runs/RUN-20260611-p3-4-teacher-permission-residual-sampling-matrix-security.md`
- `docs/subagents/runs/RUN-20260611-p3-4-teacher-permission-residual-sampling-matrix-architect.md`
- `docs/subagents/runs/RUN-20260611-p3-4-teacher-permission-residual-sampling-matrix-test.md`
- `docs/evidence/EVIDENCE-20260611-p3-4-teacher-permission-residual-sampling-matrix.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

### 7.4 禁止修改文件

除非升级为 M 并补齐对应文档，本子任务不得修改：

- `backend/src/main/**`
- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- API / schema / dependency / deployment 配置

### 7.5 实际新增测试

1. `KnowledgeBaseControllerTest.bearerTeacherListRedactsForeignCourseBoundKnowledgeBasesDespitePublicVisibilityAndSpoofedHeader`
2. `DocumentControllerTest.bearerTeacherCannotReindexForeignCourseBoundDocumentDespiteSpoofedHeaderWithoutSideEffects`
3. `AnalyticsControllerTest.teacherClassSummaryPendingReviewsRedactsForeignCourseReviews`

### 7.6 测试命令

Focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,AnalyticsControllerTest test
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,AnalyticsControllerTest,ResourceReviewControllerTest,ResourceGenerationControllerTest,AssessmentControllerTest test
```

Full backend：

```powershell
cd D:\多元agent\backend
mvn test
```

### 7.7 Architecture Drift 预检

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 预计测试-only；生产权限仍应由 Service 层执行。 |
| Frontend rules | PASS | 不改前端。 |
| Agent / RAG rules | PASS | 不改变 Agent/RAG runtime。 |
| Security | PASS | 补强 RBAC / IDOR / spoofed header / 防枚举 / 无副作用测试；不新增 secret。 |
| API / Database | PASS | 不改 API contract 或 schema。 |

## 8. Acceptance Criteria

- 新增测试覆盖教师端 KB list、Document reindex、class summary pending reviews 的残余抽样矩阵。
- Bearer token subject/roles 优先于 spoofed `X-User-Id`。
- 拒绝响应无 `data`，且不泄漏 foreign document/index/review/resource/task/course 标识或标题。
- Document reindex 拒绝路径不新增 index task。
- focused、adjacent、full backend 验证完成，或说明无法运行原因。
- Combined Evidence/Acceptance、Changelog、Memory 和 P3-4 TODO 更新完成。
- P3-4 父项保持 open，不误标完成。

## 9. 当前状态

状态：Done。

完成时间：2026-06-11。

P3-4 父项保持 open；本子任务完成后，P3-4 仍剩正式 production streaming 设计、更完整业务矩阵抽样复核，以及 Orchestrator `ANSWER_SUBMISSION` replay/scope revalidation 后续子任务。

## 10. 完成记录

### 10.1 实现结果

- 新增 `KnowledgeBaseControllerTest.bearerTeacherListRedactsForeignCourseBoundKnowledgeBasesDespitePublicVisibilityAndSpoofedHeader`。
- 新增 `DocumentControllerTest.bearerTeacherCannotReindexForeignCourseBoundDocumentDespiteSpoofedHeaderWithoutSideEffects`。
- 新增 `AnalyticsControllerTest.teacherClassSummaryPendingReviewsRedactsForeignCourseReviews`。
- 无生产代码、REST API、DTO、DB schema、依赖、部署或前端改动。

### 10.2 验证结果

Focused：

```text
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,AnalyticsControllerTest test
Tests run: 71, Failures: 0, Errors: 0, Skipped: 0
```

Adjacent：

```text
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,AnalyticsControllerTest,ResourceReviewControllerTest,ResourceGenerationControllerTest,AssessmentControllerTest test
Tests run: 172, Failures: 0, Errors: 0, Skipped: 0
```

Full backend：

```text
mvn test
Tests run: 595, Failures: 0, Errors: 0, Skipped: 1
```

### 10.3 Evidence

- `docs/evidence/EVIDENCE-20260611-p3-4-teacher-permission-residual-sampling-matrix.md`

### 10.4 后续建议

下一项高价值 P3-4 子任务建议为：

```text
P3-4 子任务：orchestrator answer submission replay scope revalidation
```
