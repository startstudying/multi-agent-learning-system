# Agent Trace 治理看板后端最小切片开发计划

## Skill Selection Report

### Task Type

后端 Agent Trace 治理功能切片，包含 API、Service、JPA、Flyway migration、测试与治理文档。

### Selected Skills

| Skill | Why Needed |
|---|---|
| `spring-boot-architecture` | 需要遵守 Controller -> Service -> Repository 分层 |
| `api-contract-design` | 需要定义 trace list 和 trace detail 返回结构 |
| `database-design` | 需要检查 migration 最大版本并新增 V15 |
| `agent-trace-design` | 任务核心是 Agent Trace 与 tool call 治理 |
| `security-review` | 需要清理 secret、token、private document |
| `test-generator` | 必须 TDD，先写 RED 测试 |
| `architecture-drift-check` | 开发前后检查分层、Agent/RAG、安全规则 |

### Missing Skills

无。

### GitHub Research Needed

No。当前项目已有 trace/task/model/token 表、JPA 和 API envelope 模式，直接复用项目内模式即可。

### New Project-Specific Skill To Create

无。本轮复用 `docs/skills/project-specific/agent-trace-design.md`。

## Subagent Decision

Use Subagents: Yes

Reason: 本任务涉及 Agent、数据库、安全、测试；用户已将本实例指定为并行后端 worker，其他 worker 同时处理 `rag` / `assessment`。

Parallelism Level: L3 by external orchestration, but this worker uses single-task implementation inside allowed files.

Selected Subagents: Backend Worker, Agent/RAG Expert, Security & Quality boundary handled in this worker report.

Implementation Mode: 当前 worker 只修改允许文件，不触碰其他 worker 模块，不修改共享收口文件。

## 计划

1. 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT / worker run 文档。
2. 写 RED 测试：
   - `AgentTraceControllerTest` 覆盖过滤参数、trace detail toolCalls、retentionPolicy。
   - `AgentRunRecorderTest` 覆盖 tool call 写入和敏感字段清理。
3. 运行 focused 测试看 RED。
4. 实现：
   - `AgentToolCall` entity / repository。
   - `AgentTraceGovernanceService` 查询聚合。
   - `AgentRunRecorder.recordToolCall(...)`。
   - DTO 扩展。
   - `AgentTraceController` 接入 list/detail。
   - `V15__agent_tool_call_trace_governance.sql`。
5. 运行 focused 测试并修复。
6. 写 Evidence / Acceptance。

## 风险

- 当前目录不是 git repository，无法用 git status 检测并行改动，只能严格按允许路径编辑。
- V2 已有 `agent_tool_call`，V15 只补列和索引，真实 MySQL smoke 由主线程后续统一做。
- 本轮只返回 retention policy，不执行清理任务。

## 架构漂移预检

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 调 Service，Service/Recorder 调 Repository |
| Frontend rules | PASS | 不涉及前端 |
| Agent / RAG rules | PASS | 仅补 Agent Trace/tool call 治理 |
| Security | PASS | 不新增依赖，清理敏感字段 |
| API / Database | PASS | API 与 SPEC 对齐，DB 只新增 V15 |
