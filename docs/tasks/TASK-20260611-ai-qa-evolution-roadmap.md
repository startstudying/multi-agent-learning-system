# TASK - AI 问答能力六阶段演进路线

## 1. Goal

将用户提出的六阶段 AI 问答演进目标整理成项目内 L 级路线文档，明确产品边界、技术合同、拆分顺序和上下文包，为后续实现切片做准备。

## 2. Task Type

新产品能力路线规划 / Agent workflow / RAG / Frontend + Backend API linkage。

## 3. Size

L

Reason:

- 覆盖 Vue、Spring Boot、SSE、RAG、工具调用、多 Agent、Trace 和安全。
- 本轮只做路线和规格文档，不实施代码。

## 4. Scope

In scope:

- 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT。
- 创建专家分析报告。
- 更新 changelog 和 project memory。
- 明确后续推荐实现顺序。

Out of scope:

- 不修改 backend / frontend 生产代码。
- 不新增 API、DTO、DB migration 或依赖。
- 不实现联网搜索、文件检索、代码分析和多 Agent runtime。

## 5. Context Pack

### Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`
- `docs/api/contract.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/architecture/overview.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/subagents/SUBAGENT_REGISTRY.md`

### Selected Skills

- feature-development-workflow
- brainstorming
- ai-learning-architecture
- spring-ai-agent-backend
- vue-edu-admin-frontend
- educational-rag-pipeline
- agent-trace-governance
- security-review

### Subagent Plan

本轮采用文档化 L1/L2 专家分析，不启动并行代码实现。专家结论落盘到：

- `docs/subagents/runs/RUN-20260611-ai-qa-evolution-roadmap.md`

### Files Allowed To Modify

- `docs/product/PRD-20260611-ai-qa-evolution-roadmap.md`
- `docs/requirements/REQ-20260611-ai-qa-evolution-roadmap.md`
- `docs/specs/SPEC-20260611-ai-qa-evolution-roadmap.md`
- `docs/plans/PLAN-20260611-ai-qa-evolution-roadmap.md`
- `docs/tasks/TASK-20260611-ai-qa-evolution-roadmap.md`
- `docs/context/CONTEXT-20260611-ai-qa-evolution-roadmap.md`
- `docs/subagents/runs/RUN-20260611-ai-qa-evolution-roadmap.md`
- `docs/evidence/EVIDENCE-20260611-ai-qa-evolution-roadmap.md`
- `docs/acceptance/ACCEPT-20260611-ai-qa-evolution-roadmap.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`

### Files Not Allowed To Modify

- `backend/**`
- `frontend/**`
- `docs/superpowers/**`
- DB migration files
- dependency manifests

### Test Commands

本轮无代码改动，使用文档存在性和内容检查：

```powershell
Test-Path docs/product/PRD-20260611-ai-qa-evolution-roadmap.md
Test-Path docs/requirements/REQ-20260611-ai-qa-evolution-roadmap.md
Test-Path docs/specs/SPEC-20260611-ai-qa-evolution-roadmap.md
Test-Path docs/plans/PLAN-20260611-ai-qa-evolution-roadmap.md
Test-Path docs/tasks/TASK-20260611-ai-qa-evolution-roadmap.md
Test-Path docs/context/CONTEXT-20260611-ai-qa-evolution-roadmap.md
Test-Path docs/subagents/runs/RUN-20260611-ai-qa-evolution-roadmap.md
```

后续实现切片再运行 backend/frontend focused and adjacent tests。

## 6. Acceptance Criteria

- [x] 六个版本目标被整理成路线表。
- [x] 明确 L 级分类和原因。
- [x] 明确 V1-V6 推荐拆分顺序。
- [x] 明确工具调用和多 Agent 安全边界。
- [x] 明确不返回原始 chain-of-thought，只返回安全思考摘要。
- [x] 明确目标效果为 `gpt-5.5` + `reasoning.effort` + 上下文工程 + 工具/自检/结构化输出的机制复刻，而不是会员权益复刻。
- [x] 明确本轮不改代码、不新增依赖。
- [x] 更新 changelog 和 project memory。

## 7. Current Boundary

本任务完成“路线规划和规格准备”。下一步建议创建并实现 Slice 1：V1-V3 统一问答合同，包括 `gpt-5.5` 主模型配置、后端 `reasoning.effort` 策略映射和安全 `reasoningSummary`。
