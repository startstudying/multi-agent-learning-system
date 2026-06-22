# SPEC - 基于 jc-ai 参考的系统优化路线

## 1. 架构原则

本轮为规划任务，不改变运行时架构。后续任何实现必须继续遵守当前基线：

```text
Vue 3 前端
-> Backend REST / Stream API
-> Application Service
-> Domain Service / Agent Orchestrator
-> Tool Gateway / RAG Service / Model Gateway
-> Mapper / Repository
-> MySQL / MinIO / VectorDB / External Provider
```

硬规则：

- 前端不直接调用 LLM、MCP、A2A、搜索 provider 或文件系统。
- Tool 只能调用 Service，不能直接访问 Mapper/Repository。
- Agent execution 必须写 trace，工具调用必须写脱敏摘要。
- RAG 输出必须包含 citations；无可靠来源时必须明确 no-source/fallback。
- 新依赖、新协议、新外部调用必须先做安全/依赖/ADR 审查。

## 2. 优化域设计

### 2.1 Capability Radar

建立一张能力雷达，把 jc-ai 的示例能力映射为本项目路线：

| 能力 | 当前状态 | 优化方向 |
|---|---|---|
| RAG KB | 已有 parser/chunk/embed/vector/hybrid/citation | 补 expected-source 标注和评测工作台 |
| Evaluation | 已有 evaluation set/run、RAG/grading/prompt eval | 补红队、Agent trace scoring、baseline regression |
| Agent | 已有 Orchestrator、Trace、Review Gate | 补高风险工具审批和 step board |
| MCP | 未接入 | 先 ADR/安全/依赖审查 |
| A2A | 未接入 | 先 ADR，定位跨服务互操作 |
| Frontend Workbench | 已有角色工作区和 AI QA UI | 补 KB index/eval/trace 运营工作台 |
| Ops Governance | 已有 health/metrics/alerts/token logs | 补 outbound allowlist 和 scoped budgets |

### 2.2 Red-Team Evaluation Extension

目标：在现有 Evaluation Set/Run 上扩展安全评测，而不是引入独立评测框架。

建议评测类型：

| Type | Example Risk | Expected Verdict |
|---|---|---|
| `PROMPT_INJECTION` | 诱导忽略系统规则或泄露 prompt | 拒绝或安全降级 |
| `JAILBREAK` | 要求绕过学习/安全边界 | 拒绝 |
| `PRIVILEGE_ESCALATION` | 学生诱导读取教师/admin 数据 | 拒绝且无数据泄露 |
| `DATA_EXFILTRATION` | 要求输出私有文档全文、密钥、用户数据 | 拒绝或最小摘要 |
| `CITATION_BYPASS` | 要求无来源编造课程答案 | no-source/fallback |
| `TOOL_ABUSE` | 诱导外部工具执行高风险动作 | pending approval 或拒绝 |

后续实现建议：

- 复用 `evaluation_set` / `evaluation_sample` / `evaluation_run`。
- 新增安全评测 sample category 和指标，而不是单独建一套评测系统。
- 评测结果进入 Evidence/Archive，供版本回归比较。

### 2.3 Agent Trace Scoring

基于现有 `agent_task`、`agent_trace`、`agent_tool_call`、`model_call_log`、`token_usage_log` 计算评分。

建议指标：

| Metric | Definition |
|---|---|
| `tool_accuracy` | 工具选择是否与任务目标匹配，是否读取了正确范围数据 |
| `step_efficiency` | 是否在 max rounds 内完成，是否存在重复/无效步骤 |
| `answer_quality` | 回答是否完整、清晰、符合任务意图 |
| `citation_compliance` | 使用课程/RAG 内容时是否引用有效来源 |
| `safety_compliance` | 是否正确拒绝越权、注入、无来源和高风险请求 |
| `cost_efficiency` | token/cost 与答案质量是否匹配 |

### 2.4 RAG Expected Source Annotation

借鉴 `jc-rag-kb-front` 的 expectedChunkIds，但映射到本项目时应使用安全摘要和权限过滤。

设计边界：

- 教师/admin 在评测样本中标注期望来源。
- 可标注 `documentId`、`chunkId`、`sectionTitle`、`pageNum` 或安全 excerpt。
- 标注页面只显示用户有权访问的 KB/document/chunk 摘要。
- 不把完整 chunk 内容暴露给无权限用户。

### 2.5 HyDE Optional Retrieval Branch

HyDE 可以作为 P1 增强，但必须满足：

- 默认关闭，配置显式开启。
- 仅后端调用模型网关生成 hypothetical answer/query。
- 先确定 allowed KB scope，再执行 HyDE 和 retrieval。
- HyDE 文本不进入前端响应，不记录原文到普通日志。
- token/cost 计入 query/workflow。
- 失败时降级回原始问题检索。
- 评测先行，必须证明 Recall/Citation 改善大于成本和风险。

### 2.6 High-Risk Tool Approval

未来工具风险分级：

| Risk | Examples | Runtime Behavior |
|---|---|---|
| Low | 只读课程 RAG、只读公开课程元数据 | 可直接执行并 trace |
| Medium | 外部 web search、跨 KB 读取、生成推荐 | 需要配置开关和审计 |
| High | 外部系统写操作、发送消息、MCP/A2A 调用、代码/文件分析 | 进入人审队列 |

审批状态建议：

```text
REQUESTED -> PENDING_APPROVAL -> APPROVED / REJECTED / EXPIRED -> EXECUTED / CANCELED
```

审批必须写入 trace，并绑定 `agentTaskId`、`traceId`、`toolCallId`、操作者、scope 和脱敏输入摘要。

### 2.7 Structured Working Memory / Step Board

不引入自由形态长期记忆。仅允许结构化、可审计的工作流状态：

- current goal
- selected learner/course/resource scope
- completed steps
- pending decisions
- retrieved source ids
- review verdict
- next action

这些字段应服务于学习路径、资源生成、QA 和评测工作流，不能替代 learner profile、learning event 或 Agent Trace。

### 2.8 MCP / A2A Boundary

MCP 和 A2A 先进入 ADR：

- 明确使用场景：内部工具暴露、外部 Agent 互操作、还是团队运维集成。
- 明确认证：JWT/API key/mTLS/allowlist。
- 明确授权 scope：用户、课程、KB、Agent、工具。
- 明确 traceId 传播和 tool call 审计。
- 明确依赖、版本、许可证、安全公告和关闭开关。

在 ADR 通过前，不添加运行时依赖，不暴露协议端口。

## 3. Frontend Spec

后续前端优化应围绕教师/管理员质量工作台：

- Evaluation Set 管理。
- RAG expected-source/chunk 标注。
- Evaluation Run 历史、对比和 baseline regression。
- Agent Trace scoring 明细。
- KB document index status、chunk count、parse/index failure 状态。
- CitationPanel、TraceTimeline、EvalMetricCard、IndexStatusBadge 等组件抽取。

不得：

- 复制 React/Ant Design 页面。
- 直接访问 LLM/search/MCP/A2A。
- 使用 GET EventSource 暴露 question/kbIds/token。
- 用假图表冒充真实治理数据。

## 4. Backend Spec

后续后端实现优先复用已有模块：

- Evaluation: 复用现有 set/run/sample/metric 模型。
- RAG: 复用 permission filter、hybrid/RRF、vector adapter、citation。
- Agent: 复用 Orchestrator、agent_task、agent_trace、agent_tool_call。
- Model: 复用 AiModelGateway、model_call_log、token_usage_log。
- Security: 复用 JWT/RBAC、CourseAccessService、object-scope authorization。

只有当现有 schema 无法表达时，才在子任务 SPEC 中设计 migration。

## 5. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS planned | 后续工具和协议必须走 Service/Tool/Orchestrator 边界。 |
| Frontend rules | PASS planned | 前端只消费后端 API 和 stream。 |
| Agent / RAG rules | PASS planned | Trace、citations、max rounds、permission filter 是硬门槛。 |
| Security | PASS planned | MCP/A2A/HyDE/outbound 均需安全审查或配置关闭。 |
| API / Database | PASS planned | 本轮不改 API/DB；后续子任务重新做 SPEC。 |
