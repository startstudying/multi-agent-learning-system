# RAG 索引恢复调度与并发锁 PRD

## 背景

当前 RAG 文档索引已经支持 active task 去重和服务层超时恢复：同一文档存在 `PENDING/RUNNING` 任务时复用任务，超时 `RUNNING` 可由 `IndexService.recoverTimedOutRunningTasks(...)` 标记为 `FAILED`。但去重逻辑仍是普通查询后创建，在并发 reindex 场景下可能两个事务同时看不到 active task 并各自创建任务；恢复逻辑也缺少启动后或定时入口，需要人工调用 Service。

## 目标

- 为同一 `documentId` 的 active 索引任务创建增加数据级并发保护。
- 增加有界后台恢复入口，定期扫描超时 `RUNNING` 索引任务并复用已有恢复逻辑。
- 保持现有上传、reindex API 响应结构不变，不引入新依赖。

## 用户价值

- 重复点击或并发触发 reindex 时，不产生多个 active 业务结果。
- 服务重启后能自动收敛卡死的 `RUNNING` 索引任务。
- 为后续真实解析、embedding 和后台 worker 接入提供稳定任务语义。

## 非目标

- 不实现文档上传 `requestId` 或业务唯一键。
- 不新增公开管理 API。
- 不新增数据库迁移或外部锁服务。
- 不实现真实索引 worker、解析器或 embedding 调度。

## 成功标准

- 并发调用 `IndexService.createPendingTask(...)` 时，同一文档只保留一个 active task。
- `PENDING/RUNNING` 任务继续被复用，`FAILED/SUCCEEDED` 后仍允许新建 `PENDING`。
- 后台恢复组件启动后或定时调用 `recoverTimedOutRunningTasks(...)`，超时阈值和开关可配置且有默认值。
- 聚焦测试 `IndexServiceTest,DocumentControllerTest` 通过。
