# RAG 索引恢复调度与并发锁需求

## 功能需求

| ID | Requirement |
|---|---|
| FR-01 | `IndexService.createPendingTask(KbDocument document)` 必须在事务中对目标文档或等价数据行加 pessimistic lock。 |
| FR-02 | 加锁后必须重新查询该文档最新索引任务，若状态为 `PENDING` 或 `RUNNING`，返回已有任务。 |
| FR-03 | 若不存在任务，或最新任务为 `FAILED` / `SUCCEEDED`，创建新的 `PENDING` 任务。 |
| FR-04 | 并发 reindex 不能为同一文档产生多个 active 业务结果。 |
| FR-05 | 增加后台恢复组件，在启动后和定时触发时扫描超时 `RUNNING` 任务。 |
| FR-06 | 后台恢复必须调用既有 `IndexService.recoverTimedOutRunningTasks(...)`，不重复实现恢复规则。 |
| FR-07 | 恢复超时阈值、调度间隔、启动后是否立即恢复应具备清晰默认值，且可通过配置覆盖。 |

## 非功能需求

| ID | Requirement |
|---|---|
| NFR-01 | 不新增第三方依赖。 |
| NFR-02 | 不改变现有 Controller/API DTO。 |
| NFR-03 | 不修改文档上传幂等相关文件。 |
| NFR-04 | 不修改 shared memory、changelog、backend todo，由主 Codex 统一更新。 |
| NFR-05 | 调度入口必须有界：单次只执行一次 repository 扫描和一次恢复调用，不做长循环。 |
| NFR-06 | 测试覆盖服务层并发锁语义、超时恢复语义和 scheduler 调用语义。 |

## 验收要求

- `IndexServiceTest` 覆盖并发创建只产生一个 active task。
- `IndexServiceTest` 覆盖超时 `RUNNING` 恢复行为。
- 新增 scheduler 测试覆盖启动恢复和定时恢复调用。
- `DocumentControllerTest` 现有 reindex 去重、恢复后新建任务行为不回退。
