# Subagent Run - RAG 索引 Worker / Progress 后端专家

## 1. 角色

- 角色：RAG / Backend Expert
- 范围：P3-2「索引任务补齐 worker 自动执行、进度、heartbeat、自动重试/重新入队和 task detail API」
- 模式：只读分析，不修改文件

## 2. 当前已有能力

- 文档上传会创建 `kb_document` 和 `PENDING kb_index_task`。
- `IndexService.createPendingTask(...)` 已支持 active task 去重。
- `IndexService.processIndexTask(...)` 已有同步处理入口，能完成轻量 Markdown / TXT / PDF / DOCX 解析、清洗、去重、切块和 chunk metadata 写入。
- `IndexTaskRecoveryScheduler` 只做超时 `RUNNING` 恢复，不消费 `PENDING`，也不重新入队。

## 3. 专家建议

本切片应聚焦：

- 新增 bounded worker/scheduler，自动消费少量 due `PENDING` 任务。
- 为 `kb_index_task` 补齐 `progress_percent`、`progress_phase`、`heartbeat_at`、`lease_owner`、`lease_until`、`next_retry_at`、`recoverable` 等运行态字段。
- 保持 `processIndexTask(...)` 为核心处理入口，但补阶段进度和 heartbeat。
- 新增 task detail API：`GET /api/index-tasks/{taskId}`。
- task detail 必须按 `task.kbId` 执行 KB read 权限检查。

## 4. 排除项

- 不接真实 embedding provider。
- 不做 VectorDB adapter。
- 不做 hybrid retrieval / RRF / reranker fallback。
- 不重写 parser/chunker，不做 OCR、token chunk、stable chunk hash 或 heading hierarchy。

## 5. 风险

- worker 并发消费同一 `PENDING` 任务会导致 chunk 删除/写入竞争，必须使用 atomic claim 或任务级锁。
- `updatedAt` 不适合作为隐式 heartbeat，应新增显式 `heartbeatAt` / lease 字段。
- 自动 retry 必须有最大次数和 backoff，避免 poison document 热循环。

