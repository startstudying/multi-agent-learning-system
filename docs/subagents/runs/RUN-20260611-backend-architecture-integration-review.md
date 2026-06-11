# RUN-20260611 backend architecture 集成评审

## Subagent Decision

- Use Subagents: Yes
- Parallelism Level: L1/L2
- Selected Subagents: Agent/RAG Expert, Backend Expert, Security & Quality, Integration Reviewer
- Implementation Mode: 主 Codex 集成，子代理并行只读分析

## 子代理结果

| 专家 | 状态 | 集成结论 |
|---|---|---|
| Agent/RAG Expert | 已返回 | Qdrant external smoke 是当前最明确缺口；工业级 Parser/OCR 继续拆后续 M/L 子任务 |
| Security & Quality | 已返回 | 外部 provider / webhook 出站治理、token 分域门禁、VectorDB dimension gate 是主要后续风险 |
| Backend Expert | 当前等待窗口未返回 | 主 Codex 基于本地证据确认 provider registry、embedding registry、ops alert persistence、token gate 已有实现 |

## 冲突处理

- 不把工业级 Parser/OCR、native/cloud OCR、DashScope 专用 SDK 一次性并入本轮实现，避免新增依赖和凭证治理失控。
- 将本轮代码变更限定为外部 smoke 实测化，默认跳过，显式 opt-in 才外连。
- 计划文件只同步已有实现和本轮证据，不声称真实外部服务 smoke 已在本机执行。

## 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Smoke 测试只验证端口与外部服务，不改变业务分层 |
| Frontend rules | PASS | 未修改前端 |
| Agent / RAG rules | PASS | VectorDB 仍经 `VectorIndexAdapter`，RAG 内容仍回库过滤 |
| Security | PASS with risk | 默认不外连；SSRF/outbound allowlist 作为后续治理风险记录 |
| API / Database | PASS | 未新增 API 或 DB schema |

## 集成决策

本轮以“计划收口 + opt-in smoke 实测化 + 证据更新”为完成标准。后续工业级 Parser/OCR、DashScope 专用 provider、outbound URL policy、分域 token budget gate 可作为独立任务继续推进。
