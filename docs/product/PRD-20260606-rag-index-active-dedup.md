# RAG 索引任务 Active 去重 PRD

## 背景

当前文档上传和重建索引会调用 `IndexService.createPendingTask(document)`，该方法每次都会创建新的 `kb_index_task`。如果用户在索引仍处于 `PENDING` 或 `RUNNING` 时重复点击 reindex，会产生多个并行索引任务，后续异步索引、恢复和状态查询会变得不可控。

## 目标

完成 P0-3 的最小 RAG 幂等切片：同一文档已有 active 索引任务时，重复 reindex 返回已有任务，不新建重复任务。

## 用户价值

- 避免重复点击造成重复索引任务。
- 为后续索引恢复、重试和进度查询提供稳定任务语义。
- 降低真实解析/embedding 接入后的重复成本。

## 非目标

- 本轮不实现上传 `requestId`。
- 本轮不实现 RAG query 响应重放。
- 本轮不新增数据库迁移。
- 本轮不实现后台索引 worker。

## 成功标准

- 同一文档最新任务为 `PENDING` 时，reindex 返回同一个 `indexTaskId`。
- 同一文档最新任务为 `RUNNING` 时，reindex 返回同一个 `indexTaskId`。
- 最新任务为 `FAILED` 或 `SUCCEEDED` 时，reindex 新建 `PENDING` 任务。
- 现有上传、列表、权限测试不回退。
