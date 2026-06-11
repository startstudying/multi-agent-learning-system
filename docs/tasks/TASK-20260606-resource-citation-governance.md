# 资源生成引用与幻觉治理任务拆解

## Task 1 文档和并行分析

- [x] 读取 P1-4 TODO 和现有 resource / RAG / review 代码。
- [x] 启动只读 subagent 做 schema、review governance、测试设计分析。
- [x] 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT / subagent run。

## Task 2 TDD RED

- [x] 在 `ResourceGenerationControllerTest` 增加资源生成写入 `source_citation` 的断言。
- [x] 在 `ResourceGenerationControllerTest` 增加初始 review `citationCheck` 断言。
- [x] 在 `ResourceGenerationControllerTest` 增加 `NO_SOURCE` 不可审批发布断言。
- [x] 运行最小测试并确认 RED。

## Task 3 后端实现

- [x] `SourceCitationRepository` 增加按 `traceId` 查询/计数能力。
- [x] `ResourceGenerationService` 为有来源资源生成任务写入 `source_citation`。
- [x] `ResourceGenerationService` 为无来源资源写入 `NO_SOURCE` citation summary。
- [x] `persistReviews(...)` 写入 Critic citation check。
- [x] 保持无来源资源 `NEEDS_REVIEW` / `PENDING_CRITIC`。
- [x] `ReviewGovernanceService` 拦截 `NO_SOURCE` 资源的 `APPROVED` 决策。
- [x] `ReviewGovernanceService.canReleaseToLearner(...)` 增加 `NO_SOURCE` 发布兜底检查。

## Task 4 验证和交付

- [x] 运行 resource / review / RAG / migration 相关测试。
- [x] 更新 evidence / acceptance。
- [x] 更新 changelog / PROJECT / BACKEND / API / AGENT_RAG memory。
- [x] 更新 backend TODO。
- [x] 创建 retrospective。

## Done Criteria

- [x] 资源生成任务有来源时保存 `source_citation`。
- [x] Review 初始记录包含 citation check。
- [x] 无来源资源明确标记 `NO_SOURCE`。
- [x] 无来源资源不会绕过 review gate 发布。
- [x] 测试覆盖上述行为并通过。

## 本切片边界

- 当前使用 `source_citation.traceId` 作为任务级引用证据关联，不新增 V11 schema。
- 当前 deterministic course citation 用于后端治理闭环，不等价于真实 Course RAG retrieval grounding。
- 后续可在 P3 或单独切片中增加 resource-level citation 外键、真实检索命中引用和 citation accuracy 评估。
