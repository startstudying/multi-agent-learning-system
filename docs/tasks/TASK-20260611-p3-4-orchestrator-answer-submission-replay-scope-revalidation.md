# TASK-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation

## 1. 任务名称

P3-4 子任务：orchestrator answer submission replay scope revalidation

## 2. 任务类型

后端权限 / Orchestrator + Assessment replay 安全补强 / stale authorization replay 修复。

## 3. 目标

修复 `ANSWER_SUBMISSION` Orchestrator replay precheck 与直接答题提交路径的授权语义漂移：

- 已有 answer/workflow replay 在返回旧 workflow 前必须重新校验当前 `questionId -> course -> ACTIVE enrollment`。
- learner 被移出课程或 enrollment 变为 `DROPPED` 后，同 `requestId` / 同 payload 的 Orchestrator replay 必须拒绝，不能返回旧 workflow。
- 拒绝路径不得新增 answer/grading/mastery/wrong-question/learning-event，也不得泄漏旧 workflow 标识、题目、课程或 raw answer。

## 4. Skill Selection Report

### Task Type

权限安全 bug fix；P3-4 父计划下的 S 级语义子任务。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目要求所有需求/修复走 S/M/L 工作流、文档、验证、Evidence、Memory 闭环。 |
| `test-driven-development` | 本任务必须先写 RED 测试证明 replay scope 漏洞，再最小修复。 |
| `systematic-debugging` | 修复前先定位数据流与根因，避免只补 Orchestrator 表层症状。 |
| `spring-ai-agent-backend` | Orchestrator/Assessment 属于后端 Agent workflow 链路，需保持 Controller -> Service 分层。 |
| `java-security-review` | 本任务是权限重放绕过修复，需审查 RBAC/IDOR/敏感信息泄漏。 |
| `object-scope-authorization` | 校验 `questionId -> KnowledgePoint.courseId -> ACTIVE enrollment` 的对象范围。 |
| `auth-context-boundary` | 保持 Bearer roles-first 与 spoofed `X-User-Id` 非提升语义。 |

### Missing Skills

无。

### GitHub Research Needed

No。本任务是项目内 replay/idempotency 授权一致性修复，不需要外部参考。

### New Project-Specific Skill To Create

暂不创建。若后续发现多个 replay/idempotency 入口反复出现 stale authorization，可抽取项目技能。

## 5. Size Classification

Size：S - Small Slice / Fast Lane。

原因：

- 目标缺陷明确，最小生产修复预计只改 `AssessmentService.replayAnswerIfPresent(...)`。
- 不改变 REST API path、请求/响应 DTO、数据库 schema、依赖、部署、前端或 AgentTrace 合同。
- 新增测试集中在一个 Orchestrator MockMvc 测试类。

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

- 需要修改 workflow envelope、AgentTask schema、认证/角色模型、DB schema 或 API contract。
- 新增测试暴露多个 replay/idempotency 服务入口存在系统性问题。
- 生产修改扩展到 3 个以上模块。

## 6. Subagent Decision

Use Subagents：Yes。

原因：用户明确要求专家 subagent 并行开发；本任务涉及权限、安全、测试和架构边界，适合 L1 并行专家分析。

Parallelism Level：L1 Parallel Analysis。

Selected Subagents：

- Security Expert：分析 replay scope 绕过风险和修复边界。
- Test Expert：设计最小 RED/GREEN MockMvc 测试。
- Architect：判断 S/M/L 分类、Allowed/Disallowed files 和架构漂移风险。

Implementation Mode：Single Codex implementation。

不使用并行实现的原因：

- 写入集中在同一个服务类和同一个测试类。
- 多 worker 同时改同一文件会制造冲突；专家报告已经并行完成。

专家报告：

- `docs/subagents/runs/RUN-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation-security.md`
- `docs/subagents/runs/RUN-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation-test.md`
- `docs/subagents/runs/RUN-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation-architect.md`

## 7. Embedded Context Pack

### 7.1 当前边界

本子任务只修复 Orchestrator `ANSWER_SUBMISSION` replay 返回旧 workflow 前缺少当前答题 scope revalidation 的问题。

### 7.2 已读上下文

- `AGENTS.md`
- `.agents/skills/feature-development-workflow/SKILL.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/subagents/SUBAGENT_REGISTRY.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`
- `docs/planning/backend-architecture-todolist.md`
- `backend/src/main/java/com/learningos/orchestrator/application/OrchestratorWorkflowService.java`
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`

### 7.3 允许修改文件

- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `docs/tasks/TASK-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation.md`
- `docs/subagents/runs/RUN-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation-security.md`
- `docs/subagents/runs/RUN-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation-test.md`
- `docs/subagents/runs/RUN-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation-architect.md`
- `docs/evidence/EVIDENCE-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

### 7.4 禁止修改文件

除非升级为 M 并补齐对应文档，本子任务不得修改：

- `backend/src/main/java/com/learningos/orchestrator/api/**`
- `backend/src/main/java/com/learningos/common/auth/**`
- `backend/src/main/resources/db/migration/**`
- `backend/pom.xml`
- `backend/src/main/resources/application*.yml`
- `frontend/**`
- API / schema / dependency / deployment 配置

### 7.5 计划新增测试

- `OrchestratorWorkflowControllerTest.answerSubmissionWorkflowReplayRevalidatesEnrollmentBeforeReturningExistingWorkflow`

### 7.6 测试命令

RED / GREEN focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest#answerSubmissionWorkflowReplayRevalidatesEnrollmentBeforeReturningExistingWorkflow test
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest,AssessmentControllerTest,AssessmentServiceTest test
```

Full backend：

```powershell
cd D:\多元agent\backend
mvn test
```

### 7.7 Architecture Drift 预检

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 修复落在 Assessment Service；Controller 不写业务权限。 |
| Frontend rules | PASS | 不改前端。 |
| Agent / RAG rules | PASS | 不改 Agent/RAG runtime；Orchestrator 仍通过 Service 编排。 |
| Security | PASS | replay 前重新校验对象范围；不新增 secret。 |
| API / Database | PASS | 不改 API contract 或 schema。 |

## 8. Acceptance Criteria

- RED 测试能证明 dropped enrollment 后 Orchestrator replay 不能返回旧 workflow。
- `AssessmentService.replayAnswerIfPresent(...)` 与 `submitAnswerWithTraceId(...)` 在 `questionId` scope revalidation 上保持一致。
- 拒绝响应无 `data`，不泄漏旧 workflow、agentTask、trace、question、requestId、course 或 raw answer。
- 拒绝 replay 不新增 answer/grading/mastery/wrong-question/learning-event。
- focused、adjacent、full backend 验证完成，或说明无法运行原因。
- Combined Evidence/Acceptance、Changelog、Memory 和 P3-4 TODO 更新完成。
- P3-4 父项保持 open，不误标完成。

## 9. 当前状态

状态：Done。

开始时间：2026-06-11。

完成时间：2026-06-11。

P3-4 父项保持 open；本子任务完成后，P3-4 仍剩更完整业务矩阵抽样复核和正式 production streaming 设计。

## 10. 完成记录

### 10.1 实现结果

- 新增 `OrchestratorWorkflowControllerTest.answerSubmissionWorkflowReplayRevalidatesEnrollmentBeforeReturningExistingWorkflow`。
- `AssessmentService.replayAnswerIfPresent(...)` 在查找历史 answer replay 前复用 `requireSubmitQuestionScope(...)`。
- Orchestrator `ANSWER_SUBMISSION` replay 与直接答题提交路径在 `questionId -> course -> ACTIVE enrollment` 授权语义上保持一致。
- 无 REST API、DTO、DB schema、依赖、部署、前端或认证框架改动。

### 10.2 验证结果

RED：

```text
mvn --% -Dtest=OrchestratorWorkflowControllerTest#answerSubmissionWorkflowReplayRevalidatesEnrollmentBeforeReturningExistingWorkflow test
Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
Status expected:<403> but was:<200>
```

Focused：

```text
mvn --% -Dtest=OrchestratorWorkflowControllerTest#answerSubmissionWorkflowReplayRevalidatesEnrollmentBeforeReturningExistingWorkflow test
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

Adjacent：

```text
mvn --% -Dtest=OrchestratorWorkflowControllerTest,AssessmentControllerTest,AssessmentServiceTest test
Tests run: 93, Failures: 0, Errors: 0, Skipped: 0
```

Full backend：

```text
mvn test
Tests run: 596, Failures: 0, Errors: 0, Skipped: 1
```

### 10.3 Evidence

- `docs/evidence/EVIDENCE-20260611-p3-4-orchestrator-answer-submission-replay-scope-revalidation.md`

### 10.4 后续建议

继续按语义子任务推进：

```text
P3-4 子任务：formal production streaming design
```

或继续补更完整业务矩阵抽样复核。
