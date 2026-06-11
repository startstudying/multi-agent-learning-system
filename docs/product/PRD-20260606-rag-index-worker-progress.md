# PRD - RAG 索引 Worker 自动执行与进度可观测

## 1. 背景

后端 TODO P3-2 要求 RAG 索引生产化。目前仓库已经具备文档上传、active index task 去重、超时恢复调度和轻量 parser/chunker/index processor，但生产路径仍只创建 `PENDING` 任务；`processIndexTask(...)` 主要由测试直接调用，没有后台 worker 自动消费，也没有 task-level progress、heartbeat、自动 retry/requeue 和 task detail API。

## 2. 目标

关闭 P3-2 中的一个明确开放项：

```text
索引任务补齐 worker 自动执行、进度、heartbeat、自动重试/重新入队和 task detail API
```

## 3. 用户价值

- 上传文档后，系统可以自动推进索引任务，而不是停留在 `PENDING`。
- 前端或运维端可以查询索引任务进度、阶段、heartbeat、retry 和安全失败原因。
- worker 崩溃、任务 lease 超时或可恢复失败时，系统可以有界地重新入队，避免永久卡死或无限重试。

## 4. 非目标

- 不实现生产级 parser adapter、复杂 PDF/DOCX、OCR、真实页码和章节层级识别。
- 不实现 token chunk、overlap、stable chunk hash 和 heading hierarchy。
- 不新增真实 embedding provider 或 VectorDB adapter。
- 不实现 hybrid retrieval、RRF、reranker timeout fallback。
- 不修改 RAG query / citation / resource generation 链路。

## 5. 成功标准

- due `PENDING` index task 可被后台 worker 自动消费。
- 同一 task 在并发 worker 下只会被一个 worker claim。
- task detail API 按 KB read 权限返回脱敏状态。
- 任务处理阶段会更新 progress 和 heartbeat。
- 可恢复失败会按 backoff 自动重新入队，超过最大次数后进入终态 `FAILED`。
- 真实 MySQL V1-V16 migration smoke 通过，普通 `mvn test` 不依赖 MySQL。

## 6. 实施状态

2026-06-06 已完成本 PRD 的 worker/progress/heartbeat/retry/detail API 范围。证据和验收见：

- `docs/evidence/EVIDENCE-20260606-rag-index-worker-progress.md`
- `docs/acceptance/ACCEPT-20260606-rag-index-worker-progress.md`
