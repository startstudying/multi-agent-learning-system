# PLAN - RAG 索引 Worker 自动执行与进度可观测

## Skill Selection Report

## Task Type

后端 RAG 索引生产化切片，涉及数据库迁移、后台 worker、API、权限、安全和测试。

## Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目要求每个需求按 PRD/REQ/SPEC/PLAN/TASK/CONTEXT 到 Evidence/Acceptance 的闭环执行。 |
| `educational-rag-pipeline` | 本任务涉及 RAG 文档索引、chunk、索引任务状态和引用前置管线。 |
| `test-driven-development` | 实现前必须先写失败测试，覆盖 worker、retry、detail API 和 migration。 |
| `security-review` | 新增 API 和后台 worker，必须覆盖权限、IDOR、错误脱敏和 retry 风险。 |
| `unit-test-generator` | 需要补 JUnit5 / MockMvc / JPA 测试。 |
| `verification-before-completion` | 完成前必须运行 fresh verification。 |
| `systematic-debugging` | 若出现测试失败，先定位根因再修复。 |
| `dispatching-parallel-agents` | 本任务已使用 RAG/Backend、Security/Quality、Integration Reviewer 并行只读审查。 |

## Missing Skills

无。

## GitHub Research Needed

No。使用 Spring 官方能力和项目既有模式，不新增依赖。

## Official Docs Checked

- Spring Framework scheduling：用于确认 fixed-delay 定时任务模式。
- Spring Boot externalized configuration：用于确认 `@ConfigurationProperties` 与 `Duration` 配置绑定。
- Spring Data JPA locking：用于确认 repository 查询方法可使用 `@Lock`。

## New Project-Specific Skill To Create

暂不创建。该模式先记录到 retrospective，后续若复用到其他后台任务再抽取。

## Confidence Check

| Check | Result |
|---|---|
| No duplicate implementation | PASS：已有 recovery scheduler，但没有 worker 自动消费、progress/heartbeat/detail API。 |
| Architecture compliance | PASS：沿用 Spring Boot、JPA、Service/Repository、Scheduled 配置模式。 |
| Official docs verified | PASS：已查 Spring scheduling、Boot configuration properties、Spring Data JPA locking 官方文档。 |
| Working implementation reference | PASS：项目内已有 `IndexTaskRecoveryScheduler` 和 `IndexRecoveryProperties` 可作为本地模式。 |
| Root cause identified | PASS：生产路径只创建 `PENDING` task，没有后台消费和任务可观测合约。 |

Confidence：0.95，可以进入实现。

## Subagent Decision

Use Subagents: Yes。

Reason：任务涉及 RAG、数据库、后台任务、安全和测试；用户要求专家 subagent 并行。

Parallelism Level：L1 并行分析 / 审查。

Selected Subagents：

- RAG / Backend Expert
- Security & Quality Expert
- Integration Reviewer

Implementation Mode：Main Codex 单线程实现，避免多个执行者修改同一 RAG 文件。

## GitHub Reference Gate

不需要。现有项目技能与官方 Spring 文档足够覆盖。

## 实施步骤

1. 归档 subagent 报告。
2. 创建 PRD / REQ / SPEC / PLAN / TASK / Context Pack。
3. TDD RED：
   - V16 migration 文本测试。
   - `IndexServiceTest` 覆盖 progress/heartbeat、retry/backoff、lease recovery。
   - worker test 覆盖自动消费和并发 claim。
   - `DocumentControllerTest` 覆盖 task detail 权限和脱敏。
4. GREEN：
   - 新增 V16 migration。
   - 扩展 `KbIndexTask`、`KbIndexTaskRepository`。
   - 新增 `IndexWorkerProperties`、`IndexTaskWorkerScheduler`。
   - 扩展 `IndexService`。
   - 新增/扩展 task detail API 和 DTO。
5. 运行聚焦测试。
6. 运行后端全量测试。
7. 运行真实 MySQL migration smoke。
8. 生成 Evidence / Acceptance / Retrospective。
9. 更新 TODO、Memory、Changelog。

## 实施结果

2026-06-06 已完成本计划的 worker/progress/heartbeat/retry/detail API 范围：

- V16 migration 增加 `kb_index_task` progress、phase、heartbeat、lease、next retry 和 recoverable 字段。
- `IndexTaskWorkerScheduler` 自动 claim 并处理 due `PENDING` task。
- `IndexService` 阶段 progress/heartbeat/lease 使用独立事务提交，task detail 可在长耗时处理中看到 durable 进度。
- `IndexTaskWorkerScheduler` 已对单个 claimed task 做异常隔离，一个缺失/删除文档不会阻断后续任务。
- `IndexTaskRecoveryScheduler` 已对齐 worker `maxRetryCount` / `retryBackoff`，并以当前时间执行 `leaseUntil < now` 恢复。
- `GET /api/index-tasks/{taskId}` 已新增，权限在 `DocumentService` 中按 KB read 检查。
- Evidence、Acceptance、Retro、Memory、Changelog 和 TODO 已更新。

验证命令见 `docs/evidence/EVIDENCE-20260606-rag-index-worker-progress.md`。

## 风险

- H2 的锁语义与 MySQL 不完全一致；需要 MySQL smoke 验证迁移，JPA 并发测试用于应用语义覆盖。
- 自动 worker 默认开启可能影响既有测试中期望 `PENDING` 的断言；测试 profile 需要显式关闭 worker，生产/dev 默认开启。
- task detail API 若未授权仍返回 detail，会造成 IDOR；必须通过 Service 权限检查。
