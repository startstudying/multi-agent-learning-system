# RAG 索引恢复调度与并发锁计划

## Skill Selection Report

## Task Type

后端 RAG 长任务幂等与恢复治理，属于 P0-3。

## Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目要求先产出 PRD/REQ/SPEC/PLAN/TASK/CONTEXT 再实现。 |
| `ai-learning-agent-development` | 保持 AI 学习系统后端分层和 RAG/Agent 约束。 |
| `educational-rag-pipeline` | 约束文档索引任务状态、恢复与后续 RAG pipeline 兼容性。 |
| `test-driven-development` | 先补服务层并发锁与 scheduler 行为测试。 |
| `unit-test-generator` | 生成 Java/JUnit5 聚焦测试覆盖正常、边界和恢复调用。 |
| `verification-before-completion` | 完成前运行聚焦测试并以输出为准。 |

## Missing Skills

无。

## GitHub Research Needed

No。本轮使用 Spring Data JPA 官方 `@Lock`/`LockModeType` 能力和项目既有 Spring Scheduling，不复制外部实现。

## New Project-Specific Skill To Create

无。该模式可作为 RAG 长任务治理经验记录在 Retrospective，暂不抽新技能。

## Confidence Check

| Check | Result |
|---|---|
| No duplicate implementation | PASS：已有 active 去重和服务恢复，但没有 document 行锁和自动恢复调度。 |
| Architecture compliance | PASS：沿用 `Application Service -> Repository`，scheduler 只调用 Service。 |
| Official docs verified | PASS：Spring Data JPA 官方文档支持在 repository 查询方法上用 `@Lock` 指定 `LockModeType`。 |
| OSS reference | PASS：不需要 OSS 参考；使用标准框架能力和现有项目模式。 |
| Root cause | PASS：普通“查询后创建”在并发无 active task 时存在竞态窗口，且恢复方法没有自动入口。 |

Confidence：0.94，可以进入实现。

## Subagent Decision

Use Subagents: No。

Reason：本 Worker 已是主 Codex 派发的后端切片执行者；影响集中在 RAG 后端索引任务一个模块，文件边界明确。涉及 RAG 与并发质量风险，由本 Worker 在文档和测试中覆盖，不再派生额外实现子代理。

Parallelism Level：Single Codex。

Implementation Mode：单任务顺序实现。

## GitHub Reference Gate

不需要。现有技能和 Spring Data JPA 官方能力覆盖本需求，不新增依赖。

## 执行步骤

1. 创建本切片 PRD/REQ/SPEC/PLAN/TASK/CONTEXT。
2. 扩展 `IndexServiceTest`：
   - 并发 `createPendingTask` 只创建一个 active task。
   - 超时恢复既有行为保持。
3. 新增 `IndexTaskRecoverySchedulerTest`：
   - 启动恢复根据配置调用一次 Service。
   - 定时恢复使用 timeout 计算 cutoff 并调用 Service。
   - disabled 时不调用恢复。
4. 运行聚焦测试确认 RED。
5. 扩展 `KbDocumentRepository` 文档行锁查询。
6. 调整 `IndexService` 注入 document repository 并在创建任务时锁文档行。
7. 新增 `IndexRecoveryProperties` 和 `IndexTaskRecoveryScheduler`，在启动类启用配置与调度。
8. 运行 `cd backend; mvn "-Dtest=IndexServiceTest,DocumentControllerTest,IndexTaskRecoverySchedulerTest" test`。
9. 运行用户要求的最小测试 `cd backend; mvn "-Dtest=IndexServiceTest,DocumentControllerTest" test`。
10. 创建 Evidence、Acceptance、Retrospective。

## 风险

- H2/MySQL 的悲观锁行为不完全等价；本轮用服务层并发测试覆盖应用语义，真实 MySQL 仍建议在 P3 migration smoke 中验证。
- 没有新增唯一索引，若未来绕过 `IndexService` 直接写 task，仍可能产生重复 active task。
- Scheduler 只把超时 `RUNNING` 标记失败，不负责重新入队；重试仍由 reindex 或后续 worker 策略触发。
