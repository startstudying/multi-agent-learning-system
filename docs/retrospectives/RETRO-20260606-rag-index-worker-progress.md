# RAG 索引 Worker 自动执行与进度可观测复盘

## 有效做法

- 把 P3-2 拆成 worker/progress/heartbeat/retry/detail API 单独切片，避免把 VectorDB、reranker 和 parser 生产化混在一起。
- 用 V16 + focused tests + full suite + real MySQL smoke 同时覆盖 schema、服务语义、API 权限和 MySQL 可执行性。
- 收口时发现 recovery scheduler 仍走旧兼容策略，补红灯测试后让 lease recovery 复用 worker retry/backoff 配置。
- 代码审查后的 RED 测试很有效：明确暴露了阶段进度事务不可见、lease cutoff 叠加超时、worker batch 被单任务异常中断三个问题。

## 问题

- 初始实现中 worker 失败路径有 retry/backoff，但旧 recovery scheduler 的 lease expired 路径没有使用同一策略，语义不一致。
- 初始实现把 `processIndexTask(...)` 放在一个大事务里，导致 progress/heartbeat/lease 的可观测性只存在于内存对象，无法被 task detail API 实时看到。
- 初始 recovery scheduler 对 explicit `leaseUntil` 又叠加 `runningTimeout`，让 lease 语义变成双重超时。
- 初始 worker loop 没有对每个 claimed task 做异常隔离，一个缺失文档会阻断 batch 后续任务。
- 当前 claim 并发主要依赖 JPA pessimistic lock 和事务边界；还没有真实 MySQL 多 worker 压测。
- task detail API 只返回当前任务状态，没有事件流或历史阶段日志。

## 后续改进

- P3-2 后续继续拆分：parser adapter/OCR、token chunk/overlap/hash、Embedding/VectorDB、hybrid retrieval/RRF/reranker fallback。
- 若后续出现更多后台任务，可抽取统一的 lease/retry/heartbeat 项目技能或共享组件，但本轮先不创建新 skill。
- 生产部署前建议补 MySQL 并发 worker 压测，验证多实例 claim 和 lease recovery 在真实隔离级别下的行为。
- 若后续保留 `runningTimeout` 配置，需要明确它只服务旧兼容恢复入口；生产 worker 的 lease recovery 应以 `leaseUntil < now` 为准。
