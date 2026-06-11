# RAG 索引任务 Active 去重计划

## Skill Selection Report

## Task Type

后端 RAG 幂等/长任务治理，属于 P0-3。

## Selected Skills

| Skill | Why Needed |
|---|---|
| `ai-learning-agent-development` | 保持 AI 学习系统后端闭环方向 |
| `educational-rag-pipeline` | 文档上传、索引任务和 RAG pipeline 规则 |
| `test-driven-development` | 先写失败测试固定 active 去重语义 |
| `verification-before-completion` | 完成前跑聚焦和全量测试 |

## Missing Skills

无。

## GitHub Research Needed

No。本轮不新增依赖，不涉及外部 API，只按现有 RAG 服务补幂等语义。

## Confidence Check

| Check | Result |
|---|---|
| No duplicate implementation | PASS：当前 `IndexService.createPendingTask` 每次新建，没有 active 去重。 |
| Architecture compliance | PASS：沿用 `DocumentService -> IndexService -> Repository`。 |
| Official docs verified | N/A：不使用新外部 API。 |
| OSS reference | PASS：沿用用户提供的 RAG/LMS 参考方向，不复制外部代码。 |
| Root cause | PASS：重复 reindex 时缺少 active task 查询。 |

Confidence：0.92，可以进入实现。

## Subagent Decision

Use Subagents: Yes, but no new implementation subagent。

Reason: 上一轮 Sartre 已完成 P0-3 RAG 幂等只读审查并给出最小切片建议；本轮实现范围只有 `IndexService` 与测试，不需要再并行派实现者。

## 执行步骤

1. 创建本轮 PRD/REQ/SPEC/PLAN/TASK/Context。
2. 修改 `DocumentControllerTest`，先写失败测试：
   - `PENDING` reindex 复用任务。
   - `RUNNING` reindex 复用任务。
   - `FAILED/SUCCEEDED` reindex 新建任务。
3. 运行聚焦测试确认 RED。
4. 修改 `IndexService.createPendingTask(...)`。
5. 运行 `DocumentControllerTest` 和全量后端测试。
6. 更新 Evidence、Acceptance、Memory、Changelog、总 TODO。

## 风险

- 当前没有任务状态更新 API，测试需要直接用 repository 修改任务状态。
- 没有数据库唯一约束时，并发请求仍可能在极小窗口下重复创建；本轮只做单事务 active 去重，后续生产化可增加业务锁或唯一约束。
