# TASK-20260610-p3-4-evaluation-review-forged-id-object-oracle-matrix

## 1. 任务名称

P3-4 子任务：Evaluation/Review forged-id object-oracle matrix

## 2. 任务类型

后端权限 / IDOR / forged-id / object-oracle 测试矩阵补强。

## 3. 目标

在不修改生产代码、REST API 合同、数据库 schema、依赖、部署或前端的前提下，补齐 Evaluation / Review 高价值 forged-id HTTP 回归测试：

- Bearer `TEACHER` 即使伪造 `X-User-Id`，也不能向 foreign `evaluationSetId` 记录 EvaluationRun。
- Bearer `TEACHER` 即使伪造 `X-User-Id`，也不能对 foreign `reviewId` 提交 review decision。
- 拒绝响应不泄露 forged id、task/resource/review/evaluation set 标识或标题。
- 拒绝请求不产生 EvaluationRun / EvaluationRunMetric，且不改变 foreign review/resource/task 状态。

## 4. Skill Selection

| Skill | 选择原因 |
|---|---|
| `feature-development-workflow` | 项目要求所有开发请求走 S/M/L 工作流、文档、验证、Evidence、Memory 闭环。 |
| `subagent-driven-development` | 用户明确要求专家 subagent 并行开发；本轮使用 Evaluation/Review 专家并行只读审查。 |
| `object-scope-authorization` | 本任务验证 evaluationSet/review/task/resource 对象归属、防枚举和响应去敏。 |
| `auth-context-boundary` | 新测试覆盖 Bearer 身份优先、spoofed `X-User-Id` 不提升权限。 |
| `test-driven-development` | 本任务为测试优先安全回归；如 RED 暴露生产缺陷再升级任务。 |
| `Confidence Check` | 实现前确认无重复覆盖、架构合规、根因和边界清晰。 |

缺失技能：无。

GitHub research：不需要。该任务只验证项目内已有权限/一致性规则，不引入新模式或依赖。

新项目技能：暂不创建。

## 5. Size Classification

Size：S - Small Slice / Fast Lane。

原因：

- 只影响 Evaluation / Review 权限测试矩阵。
- 预计只修改 2 个测试类和工作流文档。
- 不改变生产 REST API、DTO、数据库 schema、依赖、部署、前端或 Agent/RAG runtime。
- 可用 focused / adjacent / full backend Maven 测试验证。

可跳过：PRD、REQ、SPEC、PLAN、standalone Context Pack、独立 Acceptance、Retrospective。

升级触发：如果测试 RED 暴露现有生产代码未拒绝 forged-id 或产生副作用，停止并升级为 M 任务，补 REQ/SPEC/PLAN/CONTEXT 后再改生产代码。

## 6. Subagent Decision

Use Subagents：Yes。

原因：用户明确要求专家 subagent 并行开发；本任务涉及权限/安全矩阵，适合 Evaluation/Review 专家并行只读审查。

Parallelism Level：L1 - Parallel Analysis。

Implementation Mode：Single Codex implementation。

说明：

- 已复用既有 Evaluation/Review 专家线程 `019eb08d-6e19-7370-99da-764ad17b9e62` 排队只读审查。
- 为避免同一测试文件冲突，主线程负责唯一代码修改；subagent 只输出测试设计/风险/验收建议。

专家报告文件：

- `docs/subagents/runs/RUN-20260610-p3-4-evaluation-review-forged-id-object-oracle-matrix.md`

## 7. Embedded Context Pack

### 7.1 当前边界

本子任务只补 Evaluation / Review forged-id object-oracle HTTP 测试，不修改生产代码。

### 7.2 已读上下文

- `AGENTS.md`
- `.agents/skills/feature-development-workflow/SKILL.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/subagents/SUBAGENT_REGISTRY.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `backend/src/test/java/com/learningos/evaluation/api/EvaluationRunControllerTest.java`
- `backend/src/test/java/com/learningos/evaluation/api/EvaluationSetControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java`
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationRunService.java`
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationSetService.java`
- `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java`

### 7.3 允许修改文件

- `backend/src/test/java/com/learningos/evaluation/api/EvaluationRunControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java`
- `docs/tasks/TASK-20260610-p3-4-evaluation-review-forged-id-object-oracle-matrix.md`
- `docs/subagents/runs/RUN-20260610-p3-4-evaluation-review-forged-id-object-oracle-matrix.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-evaluation-review-forged-id-object-oracle-matrix.md`
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

### 7.5 计划新增测试

1. `bearerTeacherCannotRecordRunForForeignEvaluationSetDespiteSpoofedHeaderWithoutSideEffects`
2. `bearerTeacherCannotDecideForeignReviewDespiteSpoofedHeaderWithoutMutatingReviewState`

### 7.6 测试命令

Focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EvaluationRunControllerTest,ResourceReviewControllerTest test
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest test
```

Full backend：

```powershell
cd D:\多元agent\backend
mvn test
```

### 7.7 Architecture Drift 预检

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 测试-only；生产权限仍由 Service 层执行。 |
| Frontend rules | PASS | 不改前端。 |
| Agent / RAG rules | PASS | 不改 Agent/RAG runtime。 |
| Security | PASS | 补强 forged-id / object-oracle / spoofed header / 无副作用测试；不新增 secret。 |
| API / Database | PASS | 不改 API contract 或 schema。 |

## 8. Acceptance Criteria

- EvaluationRun forged foreign `evaluationSetId` 记录拒绝路径已覆盖。
- ResourceReview forged foreign `reviewId` decision 拒绝路径已覆盖。
- 拒绝响应不包含 forged id、task/resource/review/evaluation set 标识或标题，且无 `data`。
- 拒绝请求不新增 EvaluationRun / EvaluationRunMetric，不改变 foreign review/resource/task 状态。
- focused、adjacent、full backend 验证完成。
- Combined Evidence/Acceptance、Changelog、Memory 和 P3-4 TODO 更新完成。

## 9. 当前状态

状态：In Progress。

P3-4 父项保持 open；本子任务完成后仍需继续 Assessment submit foreign-questionId、dev/test legacy fallback cleanup、frontend SSE sensitive URL cleanup 等剩余项。
