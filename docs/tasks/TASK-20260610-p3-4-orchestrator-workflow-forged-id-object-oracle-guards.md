# TASK-20260610-p3-4-orchestrator-workflow-forged-id-object-oracle-guards

## 1. 任务名称

P3-4 子任务：Orchestrator workflow forged-id object-oracle guards

## 2. 任务类型

权限 / 安全测试矩阵补强。

## 3. 目标

在不修改生产代码、API 合同、数据库 schema、依赖或前端的前提下，补齐 Orchestrator workflow status / retry 的 forged `workflowId` 对象枚举与副作用测试：

- 非 owner Bearer 用户即使伪造 `X-User-Id`，也不能查询他人的 workflow status。
- 非 owner Bearer 用户即使伪造 `X-User-Id`，也不能 retry 他人失败的 `RESOURCE_GENERATION` workflow。
- 拒绝路径返回安全 `NOT_FOUND`，不泄露 workflowId、agentTaskId、traceId、requestId 等元数据。
- forbidden retry 不新增 AgentTask、AgentTrace、ResourceGenerationTask、LearningResource、ResourceReview、ModelCallLog、TokenUsageLog 或 SourceCitation 副作用。

## 4. Skill Selection

| Skill | 选择原因 |
|---|---|
| `feature-development-workflow` | 项目要求所有需求进入受控 S/M/L 工作流。 |
| `multi-agent-coder` | 用户明确要求专家 subagent 并行开发；本切片由 resource-agent 专家并行实现。 |
| `security-review` | 本任务验证 IDOR、object oracle、Bearer/header spoofing 和拒绝无副作用。 |
| `object-scope-authorization` | 匹配 workflow/task 对象归属、防枚举和响应去敏规则。 |
| `auth-context-boundary` | 匹配 Bearer 优先和 spoofed `X-User-Id` 无效规则。 |
| `verification-before-completion` | 完成前必须提供 fresh verification 证据。 |

缺失技能：无。

GitHub research：不需要。本任务是项目内现有 Orchestrator 权限矩阵补强。

新项目技能：暂不创建。

## 5. Size Classification

Size：S - Small Slice / Fast Lane。

原因：

- 只影响 Orchestrator workflow status / retry 测试矩阵。
- 只修改 1 个测试类和流程文档。
- 不改变 REST API path、请求/响应 DTO、数据库 schema、依赖、部署或前后端合同。
- 新增测试通过，未暴露生产代码缺陷。

可跳过：PRD、REQ、SPEC、PLAN、standalone Context Pack。

必须文档：本 mini TASK，内嵌 Context Pack；完成后创建 combined Evidence/Acceptance。

## 6. Subagent Decision

Use Subagents：Yes。

原因：用户明确要求专家 subagent 并行开发；本切片由 resource-agent 专家在不重叠写入范围内实现。

Parallelism Level：L3 bounded parallel implementation。

实现边界：

- 专家只修改 `OrchestratorWorkflowControllerTest.java` 和自身 subagent run 报告。
- 主 Codex 负责验收 diff、补 TASK / Evidence、运行 focused / adjacent / full verification，并统一更新 memory / changelog / TODO。

专家报告：

- `docs/subagents/runs/RUN-20260610-p3-4-broader-forged-id-business-object-penetration-matrix-resource-agent.md`

## 7. Embedded Context Pack

### 7.1 当前边界

本切片只补 Orchestrator workflow forged-id / object-oracle 测试。未修改生产代码。

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
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/harness/TEST_COMMANDS.md`

### 7.3 允许修改文件

- `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`
- `docs/tasks/TASK-20260610-p3-4-orchestrator-workflow-forged-id-object-oracle-guards.md`
- `docs/subagents/runs/RUN-20260610-p3-4-broader-forged-id-business-object-penetration-matrix-resource-agent.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-orchestrator-workflow-forged-id-object-oracle-guards.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

### 7.4 禁止修改文件

除非升级为 M 并更新 Context Pack，本切片不得修改：

- `backend/src/main/**`
- `backend/pom.xml`
- `frontend/**`
- `backend/src/main/resources/db/migration/**`
- API / schema / dependency / deployment 配置

### 7.5 新增测试

1. `workflowStatusRejectsBearerNonOwnerForgedWorkflowIdWithoutLeakingMetadata`
2. `workflowRetryRejectsBearerNonOwnerForgedWorkflowIdWithoutSideEffects`

### 7.6 测试命令

Focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest test
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=OrchestratorWorkflowControllerTest,ResourceGenerationControllerTest,AgentTraceControllerTest test
```

Full backend：

```powershell
cd D:\多元agent\backend
mvn test
```

### 7.7 Architecture Drift 预检

- Backend layering：PASS，本切片只补测试。
- Frontend rules：N/A，不改前端。
- Agent / RAG rules：PASS，不改变 Agent/RAG runtime。
- Security：PASS，不新增 secret / 依赖；权限仍由后端代码执行。
- API / Database：PASS，不改 API contract 或 schema。

## 8. Acceptance Criteria

- 新增测试覆盖 forged `workflowId` status / retry 的 non-owner Bearer + spoofed header 场景。
- forbidden status 不泄漏 workflow/task/trace/request 元数据。
- forbidden retry 不产生新增业务或 trace 副作用。
- focused、adjacent、full backend 验证完成。
- Combined Evidence/Acceptance、Changelog、Memory 和 P3-4 TODO 更新完成。

## 9. 完成记录

状态：Done。

证据：

- `docs/evidence/EVIDENCE-20260610-p3-4-orchestrator-workflow-forged-id-object-oracle-guards.md`
- Focused：`mvn --% -Dtest=OrchestratorWorkflowControllerTest test`，`32 run, 0 failures, 0 errors, 0 skipped`。
- Adjacent：`mvn --% -Dtest=OrchestratorWorkflowControllerTest,ResourceGenerationControllerTest,AgentTraceControllerTest test`，`73 run, 0 failures, 0 errors, 0 skipped`。
- Full backend：`mvn test`，`572 run, 0 failures, 0 errors, 1 skipped`。

验收：PASS。本切片仅补测试，未修改生产代码、API、DTO、DB schema、依赖、部署或前端；P3-4 父项仍保持 open。
