# 资源生成引用与幻觉治理 Subagent Run

## 任务

为 P1-4 引用与幻觉治理做只读并行分析，输出 schema、review governance 和测试设计建议。

## 子 Agent

| Agent | 任务 | 状态 | 结果 |
|---|---|---|---|
| James | RAG citation / schema 影响分析 | errored / closed | 模型通道 503，未产出可用报告；由 Tesla 分析补位 |
| Tesla | RAG citation / schema 影响分析 | completed / closed | 建议复用 `SourceCitationRecord`、`SourceCitationRepository`、`ResourceGenerationTask.traceId`；本切片先做任务级 citation，不新增 V11 schema |
| Arendt | Review governance 风险分析 | completed / closed | 指出原 release gate 只看状态和数量，不检查 citation evidence；建议审批拦截和 release 兜底双层防线 |
| Plato | TDD RED 测试设计 | completed / closed | 建议覆盖 source citation 保存、review `citationCheck`、`NO_SOURCE` 不可发布 |

## 主线整合结论

- `source_citation` 表、`SourceCitationRecord` 和 `SourceCitationRepository` 已存在。
- RAG 问答已能按 `traceId` 写 citation；资源生成此前未写 citation。
- `ResourceReview.citationCheck` 可复用为 Critic Agent 引用检查结果。
- `ContentSafetyService.reviewDraftResource(...)` 已有 citation / no-source 状态基础。
- 本切片采用 `traceId` 任务级 citation 关联，不新增 resource-level schema。

## 已实施结果

- `SourceCitationRepository` 增加 `countByTraceId(...)` 和 `findByTraceIdOrderByCreatedAtAsc(...)`。
- `ResourceGenerationService` 有来源时写入任务级 `source_citation`，无来源时写入 `NO_SOURCE` summary。
- `ResourceGenerationService` 为 Critic review 写入 `citationCheck`。
- `ReviewGovernanceService` 拦截 `NO_SOURCE` 的 `APPROVED` 决策，并在 learner release 中兜底阻断。

## 风险与后续

- 当前 citation 是任务级证据，不是 resource-level 证据。
- deterministic citation 只证明治理闭环，不代表真实 RAG retrieval grounding。
- 真实 Course RAG retrieval、resource-level citation schema、citation accuracy 评估和真实 RBAC 仍是后续任务。

## 验证

```powershell
cd backend
mvn "-Dtest=ResourceGenerationControllerTest#resourceGenerationPersistsSourceCitationsAndCriticCitationCheck+noSourceGeneratedResourcesRequireReviewAndRejectApproval" test
mvn "-Dtest=OrchestratorWorkflowControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest,RagQueryServiceTest,SchemaConvergenceMigrationTest" test
```

结果：

```text
2 tests passed
69 tests passed
```
