# RAG 索引任务 Active 去重任务

## 任务清单

- [x] 读取项目记忆、RAG 记忆、IndexService、DocumentService 和测试。
- [x] 创建本轮 workflow 文档。
- [x] 写失败测试覆盖 active 去重和 terminal 新建。
- [x] 实现 `IndexService.createPendingTask(...)` active 去重。
- [x] 跑聚焦测试。
- [x] 跑全量后端测试。
- [x] 更新 Evidence、Acceptance、Memory、Changelog、总 TODO。

## Done Criteria

- [x] `PENDING` 最新索引任务重复 reindex 返回同一 `indexTaskId`。
- [x] `RUNNING` 最新索引任务重复 reindex 返回同一 `indexTaskId`。
- [x] `FAILED` 最新索引任务 reindex 新建 `PENDING` 任务。
- [x] `SUCCEEDED` 最新索引任务 reindex 新建 `PENDING` 任务。
- [x] `mvn test` 通过或记录失败原因。
