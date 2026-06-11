# RAG 索引任务超时恢复计划

## Skill Selection Report

## Task Type

后端 RAG 长任务恢复治理，属于 P0-3。

## Selected Skills

| Skill | Why Needed |
|---|---|
| `ai-learning-agent-development` | 保持 AI 学习系统后端闭环方向 |
| `educational-rag-pipeline` | 索引任务状态、失败恢复和 RAG pipeline 规则 |
| `test-driven-development` | 先写失败测试固定恢复语义 |
| `verification-before-completion` | 完成前跑聚焦和全量测试 |

## Missing Skills

无。

## GitHub Research Needed

No。本轮不新增依赖，不涉及外部 API，只使用现有表字段和 Spring Data JPA Repository。

## Confidence Check

| Check | Result |
|---|---|
| No duplicate implementation | PASS：代码中没有索引超时恢复服务或扫描方法。 |
| Architecture compliance | PASS：沿用 `IndexService -> KbIndexTaskRepository`，不把逻辑放入 Controller。 |
| Official docs verified | N/A：不使用新外部 API。 |
| OSS reference | PASS：沿用项目已有 RAG 长任务治理方向，不复制外部代码。 |
| Root cause | PASS：`RUNNING` 卡死任务会被 active 去重复用，缺少超时失败化。 |

Confidence：0.93，可以进入实现。

## Subagent Decision

Use Subagents: Yes。

Reason: 用户要求启动多 subagent 并行开发；本轮 P0 剩余项横跨 Orchestrator、RAG 幂等恢复和 Review Gate 状态治理，已派 Orchestrator 架构审查与安全幂等审查并行分析。

Parallelism Level: L1。

Selected Subagents:

- Franklin：P0-1 Orchestrator Workflow 下一步切片审查。
- Cicero：P0-3/P0-4 安全与幂等治理审查。

Implementation Mode: Single Codex implementation with parallel analysis。

## 执行步骤

1. 创建本轮 PRD/REQ/SPEC/PLAN/TASK/Context。
2. 新增或扩展 `IndexServiceTest`，先写失败测试：
   - 超时 `RUNNING` 恢复为 `FAILED`。
   - 未超时 `RUNNING` 不变。
   - `PENDING/SUCCEEDED/FAILED` 不变。
3. 扩展 `DocumentControllerTest`，覆盖恢复后 `reindex` 新建任务。
4. 运行聚焦测试确认 RED。
5. 补齐 `KbIndexTask` 恢复字段 getter/setter。
6. 扩展 `KbIndexTaskRepository` 超时查询。
7. 实现 `IndexService.recoverTimedOutRunningTasks(...)`。
8. 运行聚焦测试和全量后端测试。
9. 更新 Evidence、Acceptance、Memory、Changelog、总 TODO、Retrospective。

## 风险

- 当前没有真实后台 worker，本轮只交付 Service 能力；自动扫描需要后续调度切片。
- 通过 `updatedAt` 判断超时依赖任务执行过程正确刷新更新时间；真实 worker 接入后需要持续维护 heartbeat 或进度更新。
- 本轮不解决并发窗口下重复创建索引任务的问题。
