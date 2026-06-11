# Subagent Run - RAG 索引 Worker / Progress 集成审查

## 1. 角色

- 角色：Integration Reviewer
- 范围：合并 RAG/Backend Expert 与 Security/Quality Expert 结论
- 模式：只读分析，不修改文件

## 2. 集成结论

P3-2 最小切片可以推进，但安全约束是准入条件，不是附加项。

## 3. 必须实现项

- `kb_index_task` 增加 `progress_percent`、`progress_phase`、`heartbeat_at`、`lease_owner`、`lease_until`、`next_retry_at`、`recoverable`。
- 新增 bounded index worker，定时扫描 due `PENDING` 任务并调用现有索引处理能力。
- worker claim 必须原子化，避免多个 worker 同时处理同一任务。
- worker claim 后写入 lease 和 heartbeat；处理阶段持续更新 progress / heartbeat。
- 超时恢复升级为基于 `lease_until` / `heartbeat_at`，不再只依赖 `updatedAt`。
- 失败后按 bounded retry/backoff 重入队；超过上限进入终态 `FAILED`。
- 错误信息使用安全错误码，不保存/返回原始异常消息。
- 新增 `GET /api/index-tasks/{taskId}`，并按 KB read 权限返回脱敏 detail。

## 4. 必须排除项

- 不做 VectorDB adapter。
- 不接真实 embedding provider。
- 不做 hybrid retrieval、RRF、reranker timeout fallback。
- 不升级 PDF/DOCX/OCR/parser adapter。
- 不做 token chunk、overlap、stable chunk hash、heading hierarchy。
- 不改 RAG 查询链路和 citation 生成逻辑。

## 5. 冲突与裁决

- RAG/Backend Expert 的最小切片方向正确。
- Security/Quality Expert 的 atomic claim、lease、错误脱敏、bounded retry 和权限要求必须进入 SPEC 和验收。
- 现有 `recoverTimedOutRunningTasks(updatedAt cutoff)` 可以保留兼容，但新语义应基于显式 lease/heartbeat。
- 现有 `safeError(exception)` 不能继续作为生产安全错误策略。

