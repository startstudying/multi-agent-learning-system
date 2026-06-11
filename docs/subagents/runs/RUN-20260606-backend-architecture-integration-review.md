# RUN-20260606 后端架构计划 Integration Review

## 1. 输入报告

| 专家 | 报告 | 结论 |
|---|---|---|
| Backend / Spec Architect | `docs/subagents/runs/RUN-20260606-backend-architecture-cost-model-observability-architect.md` | P2-4 analytics 层治理已实现；调用前预算门禁、真实模型接入、可观测性仍属 P3。 |
| Agent/RAG Expert | `docs/subagents/runs/RUN-20260606-backend-architecture-rag-production-expert.md` | P2-2 归档脚本已有；P3-2 有轻量 parser/chunker 和恢复基础，但 production RAG 仍未完成。 |
| Security & Quality | `docs/subagents/runs/RUN-20260606-backend-architecture-security-quality-review.md` | P3 安全总体 HIGH 风险；真实 RBAC、IDOR、MySQL V6-V15 smoke、依赖审计仍未闭环。 |

## 2. 冲突与决议

| 冲突 | 决议 |
|---|---|
| P2-4 TODO 未勾选，但代码和测试已存在。 | 补齐 PRD/REQ/SPEC/PLAN/TASK/EVIDENCE/ACCEPT/RETRO 后，将 P2-4 analytics 治理项标记完成。调用前预算门禁保留到 P3。 |
| P2-2 写着“定期评估脚本或测试”，但脚本是本地归档而非自动周期 runner。 | 按原句“脚本或测试 + 可归档报告”关闭；自动周期 runner 明确写入后续生产化，不作为当前完成条件。 |
| P3-2 有轻量 parser/chunker，但 TODO 写的是生产化。 | 拆分为“轻量基础已完成”和“生产级 adapter / OCR / VectorDB / hybrid retrieval / worker progress 待完成”。 |
| P3-1 有 smoke test 文件和脚本，但缺少真实 MySQL 执行证据。 | 不关闭 P3-1，后续必须运行并归档 MySQL 8 smoke，最好扩展到 V1-V15。 |
| Security 报告指出 `X-User-Id` dev auth 可伪造。 | P3-4 必须优先处理生产认证、RBAC/course scope、Profile/Learning Path IDOR 和 RAG `kbIds` 渗透测试。 |

## 3. 当前可关闭项

- P2-2：可归档 RAG 评估脚本 / 测试 / 报告。
- P2-4：analytics 层 Token / Cost 预算治理统计、预算规则建议、高成本任务告警、异常模型调用识别。
- P3-2 的基础子项：轻量 parser/chunker；索引 active 幂等、文档行锁、超时失败恢复和 scheduler 恢复。

## 4. 当前必须保留开放项

- P3-1：MySQL 8 真实 smoke 执行 evidence / acceptance，且建议覆盖 V1-V15。
- P3-2：生产级 parser adapter、OCR、真实页码、token chunking、embedding、VectorDB、hybrid retrieval、RRF、reranker timeout fallback、worker progress。
- P3-3：真实 Spring AI / Spring AI Alibaba provider、schema validation、模型调用前预算门禁和日志闭环。
- P3-4：生产认证、RBAC/course scope、Profile/Learning Path IDOR、RAG `kbIds` API/Orchestrator 渗透测试。
- P3-5：结构化日志、Micrometer、深度健康检查和运维告警。

## 5. 推荐下一任务

优先级建议为 P3-4 权限与安全加固。原因：Security & Quality 报告判定当前 P3 安全为 HIGH 风险，`X-User-Id` dev auth 和 Profile/Learning Path IDOR 会影响后续所有生产化能力的可信边界。
