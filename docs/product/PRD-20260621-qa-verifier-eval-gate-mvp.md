# PRD-20260621 Basic Verifier / Eval Gate MVP

## 1. 背景

P0-1 已完成 Memory/RAG 隐私守门，P0-2 已完成 `MemoryContextService`，P0-3 已把 `/api/ai/qa` 升级为 `QaRuntime` 并输出结构化回答字段。

当前缺口是：回答虽然结构化了，但还没有一个后端可回归的 gate 来判断 citation、no-source、隐私、schema 是否满足最低质量要求。若后续只靠人工感觉或 prompt 调整，质量不可复现。

## 2. 用户价值

- 学生端拿到的回答能明确标记是否通过基础质量校验。
- 教师/管理员后续可以基于 `verification` 和 eval gate 理解回答质量风险。
- 开发者可以把 prompt/model/tool 策略变更接入最小评测门禁，而不是只看单次样例。

## 3. MVP 范围

本切片完成：

- 新增 `AnswerVerifier`，校验 AI QA 响应的 citation/no-source/privacy/schema 基础规则。
- `/api/ai/qa` 响应新增 `verification` 结构化字段。
- `toolCalls` 新增 `AnswerVerifier` 低敏摘要。
- 新增 `QaEvalGate` 纯服务，用于根据样本数、关键指标和 baseline/candidate 信息输出 `PASS` / `FAIL` / `INSUFFICIENT_SAMPLE`。
- 复用现有 Evaluation Set/Run，新增最小 `AI_QA_ANSWER` 样本类型和 QA gate 指标白名单。

本切片不完成：

- 不新增 DB schema。
- 不新增 REST API 路径。
- 不新增依赖。
- 不做前端展示。
- 不调用真实模型 provider。
- 不做 P1 streaming/workbench。
- 不做 P2 memory lifecycle。

## 4. 成功标准

- grounded 回答必须通过 citation/schema/privacy 基础校验。
- no-source 回答必须不带 citation，必须标记 fallback、medium uncertainty 和 review。
- 任何泄露 prompt/provider key/teacher note/chain-of-thought 的响应必须被 verifier 判为 `FAIL`。
- 低样本 eval run 不能作为上线依据，gate 输出 `INSUFFICIENT_SAMPLE`。
- 缺少 citation/no-source/privacy/schema 关键指标时，gate 输出 `FAIL`。
- 策略变更必须有 baseline 与 candidate，否则 gate 输出 `FAIL`。
