# CONTEXT - 基于 jc-ai 参考的系统优化路线

## 1. Task Boundary

本上下文包用于承载“根据 jc-ai 做一次优化计划”的规划任务。

当前任务只做：

- 外部参考分析。
- 优化方向筛选。
- 路线文档产出。
- 记忆和 changelog 更新。

当前任务不做：

- 后端实现。
- 前端实现。
- DB migration。
- 依赖引入。
- 协议端口暴露。

## 2. Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`
- `docs/memory/DECISION_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`
- `docs/planning/backend-architecture-todolist.md`

## 3. External Reference

- GitCode URL: `https://gitcode.com/jingdianjichi/jc-ai.git`
- Local zip: `D:/迅雷下载/jc-ai-master (1).zip`
- Extracted folder: `.tmp/external-references/jc-ai-zip/jc-ai-master`

Reference modules inspected:

- `README.md`
- `jc-rag-kb`
- `jc-rag-kb-front`
- `eval-framework`
- `agent-ai`
- `agent-scope-ai`
- `mcp-tools-server`
- `mcp-tools-client`
- `a2a-server`
- `a2a-client`

## 4. Selected Skills

| Skill | Role |
|---|---|
| feature-development-workflow | 强制项目流程、S/M/L、文档和验收。 |
| ai-learning-architecture | 多 Agent、工具边界、状态和失败策略。 |
| educational-rag-pipeline | RAG retrieval、citations、expected source 和评测。 |
| agent-trace-governance | trace、tool call、model/token/cost 和审计。 |
| spring-ai-agent-backend | 后端模型网关、tool calling、SSE、结构化输出。 |
| vue-edu-admin-frontend | Vue 教师/admin 工作台和可视化。 |
| tech-review | 技术方案五维评审。 |
| security-review | MCP/A2A、外部调用、审批和依赖安全。 |

## 5. Subagent Plan

Use Subagents: project gate yes; live spawn no.

Reason:

- 任务达到 L 级并涉及 Agent/RAG，项目规则要求 Multi-Expert Gate。
- 当前工具约束要求只有用户明确要求子代理时才实际 spawn。
- 本轮采用文档化专家评审，报告见：

`docs/subagents/runs/RUN-20260611-jc-ai-reference-optimization-roadmap.md`

## 6. Files Allowed To Modify

- 本任务相关 `docs/research/github-references/`
- 本任务相关 `docs/product/`
- 本任务相关 `docs/requirements/`
- 本任务相关 `docs/specs/`
- 本任务相关 `docs/plans/`
- 本任务相关 `docs/tasks/`
- 本任务相关 `docs/context/`
- 本任务相关 `docs/subagents/runs/`
- 本任务相关 `docs/evidence/`
- 本任务相关 `docs/acceptance/`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`
- `docs/memory/DECISION_MEMORY.md`

## 7. Files Not Allowed To Modify

- `backend/**`
- `frontend/**`
- `docs/superpowers/**`
- dependency manifests
- DB migration files
- secret/config files

## 8. Verification Commands

```powershell
rg -n "jc-ai-reference-optimization-roadmap|jc-ai 参考|HyDE|MCP|A2A|红队" docs\research docs\product docs\requirements docs\specs docs\plans docs\tasks docs\context docs\evidence docs\acceptance docs\subagents docs\memory docs\changelog
git diff --stat
```

## 9. Current Recommended Next Task

```text
P0-1 红队与 Prompt 注入评测扩展
```

该任务应按 M 级流程重新创建 REQ/SPEC/PLAN/TASK/CONTEXT，并优先复用现有 Evaluation Set/Run。
