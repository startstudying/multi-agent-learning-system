# PLAN-20260611 后端架构后续增强计划收口

## Skill Selection Report

### Task Type

后端架构计划收口 / RAG VectorDB 运维 / Provider smoke / 安全质量验证。

### Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 项目强制 S/M/L 工作流 |
| multi-agent-coder | 用户要求专家 subagent 并行开发 |
| educational-rag-pipeline | P3-2 RAG parser/vector 增强判断 |
| agent-trace-governance | 保持 AI workflow 可审计边界 |
| security-review | 外部 provider、webhook、VectorDB、权限矩阵风险 |
| verification-before-completion | 完成前必须有测试证据 |

### Missing Skills

无。DashScope 专用 SDK、native/cloud OCR 未来若启动，需要重新做官方文档/依赖审查。

### GitHub Research Needed

No。本轮不新增依赖，不复制外部项目模式。

### New Project-Specific Skill To Create

暂不创建。

## Size Classification

- Size: L
- Reason: 覆盖 RAG/VectorDB、Provider、Ops、Security、文档收口多个模块，并使用多专家并行评审。
- Required Documents: PRD / REQ / SPEC / PLAN / TASK / CONTEXT / EVIDENCE / ACCEPT。
- Can Skip: 新依赖审查，因为本轮不新增依赖。
- Upgrade Trigger: 若引入 native/cloud OCR、DashScope 专用 SDK、outbound allowlist 生产策略或新 DB schema，需要另起独立 L/M 任务。

## Subagent Decision

- Use Subagents: Yes
- Reason: 用户明确要求，且任务影响 3+ 模块。
- Parallelism Level: L1/L2
- Selected Subagents: Agent/RAG Expert, Backend Expert, Security & Quality
- Implementation Mode: 子代理并行分析，主 Codex 负责最小代码补丁与集成验收。

## 执行步骤

1. 读取项目记忆、技能注册表、子代理注册表、架构基线和计划文件。
2. 启动专家 subagent 并行分析。
3. 本地核验现有实现状态。
4. 将 external smoke 占位测试升级为真实 opt-in smoke。
5. 运行聚焦和相邻测试。
6. 更新计划、证据、验收、changelog、memory。

## 风险

- 本机没有外部 Qdrant/provider 凭证，opt-in smoke 只能验证默认跳过和编译。
- Backend Expert 未在当前等待窗口内返回，集成评审记录该限制。
- 出站 URL allowlist 属于后续安全治理，不在本轮临时拼接。
