# RAG 索引恢复调度与并发锁任务

## TASK-01：服务层并发锁测试

- [x] 在 `IndexServiceTest` 保存一个可锁定 `KbDocument`。
- [x] 使用两个并发调用触发 `IndexService.createPendingTask(document)`。
- [x] 断言两个调用返回同一 `indexTaskId`。
- [x] 断言数据库中该文档只有一个 active task。

Done Criteria：测试在未实现文档行锁前能暴露竞态风险或缺失锁方法，最终通过。

## TASK-02：文档行锁实现

- [x] 在 `KbDocumentRepository` 增加 `PESSIMISTIC_WRITE` 查询方法。
- [x] `IndexService.createPendingTask(...)` 在事务中锁定文档。
- [x] 加锁后按最新任务状态复用或新建。
- [x] 不修改 `DocumentService` / `DocumentController`。

Done Criteria：现有 active 去重和恢复后 reindex 行为不回退。

## TASK-03：后台恢复调度测试

- [x] 新增 scheduler 单元测试或 Spring slice 测试。
- [x] 覆盖启动恢复调用。
- [x] 覆盖定时恢复调用。
- [x] 覆盖 disabled 不调用。

Done Criteria：测试不依赖外部服务，不新增依赖。

## TASK-04：后台恢复调度实现

- [x] 新增 `IndexRecoveryProperties`。
- [x] 新增 `IndexTaskRecoveryScheduler`。
- [x] 启用 scheduling 和配置属性。
- [x] 调度逻辑有界且复用 `IndexService.recoverTimedOutRunningTasks(...)`。

Done Criteria：scheduler 行为测试通过，默认配置清晰。

## TASK-05：验证与交付文档

- [x] 运行聚焦测试。
- [x] 创建 Evidence。
- [x] 创建 Acceptance。
- [x] 创建 Retrospective。
- [ ] 最终回复包含状态、改动文件、测试结果、关键行为和风险。

Done Criteria：不编辑 shared memory/changelog/backend todo。
