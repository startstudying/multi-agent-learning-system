# CONTEXT - AI 问答能力六阶段演进路线

## 1. Task Boundary

本上下文包用于承载用户提出的六阶段 AI 问答路线：

1. 普通问答。
2. `Thinking` / `Fast` / `Expert` 三档模式。
3. `reasoning summary` 思考摘要。
4. SSE 流式输出。
5. 联网搜索、文件检索、代码分析工具调用。
6. 规划、检索、审查、生成多 Agent。
新增校准：目标效果以 `gpt-5.5` 为主模型，通过 `reasoning.effort`、干净上下文、工具检索、多轮自检、结构化输出和评测迭代复刻 GPT Web 端的高质量机制，不复刻会员权益或不可公开产品策略。

本轮只完成路线和规格文档，不进入实现。

## 2. Related Existing Capabilities

- RAG query API and citations。
- Production `POST /api/rag/query/stream`。
- Agent Trace governance and `agent_tool_call`。
- Model provider registry and `AiModelGateway`。
- Orchestrator workflow context for several AI paths。
- Frontend student cockpit with RAG stream integration。

## 3. Selected Skills

| Skill | Role |
|---|---|
| feature-development-workflow | 强制执行项目工作流。 |
| brainstorming | 将六阶段想法收敛为路线和可拆方案。 |
| ai-learning-architecture | 多 Agent、状态机、工具边界。 |
| spring-ai-agent-backend | Spring Boot / Spring AI / SSE / Tool calling。 |
| vue-edu-admin-frontend | Vue 问答工作台体验。 |
| educational-rag-pipeline | 文件检索、RAG citation、权限过滤。 |
| agent-trace-governance | trace、tool call、model/token/cost 记录。 |
| security-review | 工具、联网、代码分析、安全摘要。 |

## 4. Subagent Plan

Use Subagents: documented analysis only.

Reason:

- L 级跨模块 Agent/RAG 任务必须启用专家门。
- 当前没有启动并行实现的明确授权，避免在规划阶段创建代码分叉。

Report:

- `docs/subagents/runs/RUN-20260611-ai-qa-evolution-roadmap.md`

## 5. Allowed Files

只允许修改本任务文档、证据、验收、changelog 和 project memory。

## 6. Disallowed Files

- `backend/**`
- `frontend/**`
- `docs/superpowers/**`
- `pom.xml`
- `package.json`
- Flyway migration

## 7. Test / Verification Commands

```powershell
Test-Path docs/product/PRD-20260611-ai-qa-evolution-roadmap.md
Test-Path docs/requirements/REQ-20260611-ai-qa-evolution-roadmap.md
Test-Path docs/specs/SPEC-20260611-ai-qa-evolution-roadmap.md
Test-Path docs/plans/PLAN-20260611-ai-qa-evolution-roadmap.md
Test-Path docs/tasks/TASK-20260611-ai-qa-evolution-roadmap.md
Test-Path docs/context/CONTEXT-20260611-ai-qa-evolution-roadmap.md
Test-Path docs/subagents/runs/RUN-20260611-ai-qa-evolution-roadmap.md
```

## 8. Current Task Boundary

完成路线定义，不实现代码。后续第一实现切片建议为：

```text
V1-V3 统一问答合同：普通问答 + answerMode + gpt-5.5 reasoning effort mapping + safe reasoningSummary
```
