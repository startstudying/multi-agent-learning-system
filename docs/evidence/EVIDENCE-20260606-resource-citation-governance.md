# 资源生成引用与幻觉治理证据

## 1. 追踪

- PRD：`docs/product/PRD-20260606-resource-citation-governance.md`
- REQ：`docs/requirements/REQ-20260606-resource-citation-governance.md`
- SPEC：`docs/specs/SPEC-20260606-resource-citation-governance.md`
- PLAN：`docs/plans/PLAN-20260606-resource-citation-governance.md`
- TASK：`docs/tasks/TASK-20260606-resource-citation-governance.md`
- CONTEXT：`docs/context/CONTEXT-20260606-resource-citation-governance.md`
- 日期：2026-06-06

## 2. 实现内容

- `ResourceGenerationService` 注入 `SourceCitationRepository`，资源生成有来源时按任务 `traceId` 保存 `source_citation`。
- 资源生成响应中的 `citationSummary` 区分 `COURSE_RAG` 和 `NO_SOURCE`。
- `ResourceReview.citationCheck` 在初始 Critic review 中写入引用检查结果。
- `ReviewGovernanceService.decide(...)` 拦截 `APPROVED + NO_SOURCE`，返回 `409 CONFLICT`。
- `ReviewGovernanceService.canReleaseToLearner(...)` 增加发布兜底检查，资源或 review 含 `NO_SOURCE` 时不允许学生端 release。
- `SourceCitationRepository` 增加 `countByTraceId(...)` 和 `findByTraceIdOrderByCreatedAtAsc(...)`。

## 3. TDD 证据

### RED

命令：

```powershell
cd backend
mvn "-Dtest=ResourceGenerationControllerTest#resourceGenerationPersistsSourceCitationsAndCriticCitationCheck+noSourceGeneratedResourcesRequireReviewAndRejectApproval" test
```

失败摘要：

```text
Tests run: 2, Failures: 2, Errors: 0, Skipped: 0
JSON path "$.data.resources[0].citationSummary"
Expected: a string containing "COURSE_RAG"
but: was "Cites Course RAG sources before critic approval."

JSON path "$.data.resources[0].citationSummary"
Expected: a string containing "NO_SOURCE"
but: was "Cites Course RAG sources before critic approval."
BUILD FAILURE
```

### GREEN

命令：

```powershell
cd backend
mvn "-Dtest=ResourceGenerationControllerTest#resourceGenerationPersistsSourceCitationsAndCriticCitationCheck+noSourceGeneratedResourcesRequireReviewAndRejectApproval" test
```

结果：

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 相关回归

命令：

```powershell
cd backend
mvn "-Dtest=ResourceGenerationControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest,RagQueryServiceTest,SchemaConvergenceMigrationTest" test
```

结果：

```text
Tests run: 45, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Orchestrator 回归

命令：

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest,RagQueryServiceTest,SchemaConvergenceMigrationTest" test
```

结果：

```text
Tests run: 69, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 4. 架构检查

- [x] Controller 保持薄层，业务规则在 Service。
- [x] 未新增依赖。
- [x] 未新增数据库 migration，本切片复用现有 `source_citation.traceId`。
- [x] Agent / RAG 证据继续通过 `traceId` 关联。
- [x] AI 生成资源仍需 Critic/教师 review 后才能 release。
- [x] `NO_SOURCE` 资源不能直接 approve 或发布给学生。

## 5. 已知限制

- 当前 citation 是任务级证据，通过 `traceId` 关联，不是 resource-level 外键。
- 当前有来源路径使用 deterministic course citation，只证明后端治理闭环，不代表真实检索命中。
- 真实 Course RAG retrieval、resource-level citation schema、citation accuracy / groundedness 评估仍是后续增强项。
- 临时 `teacher/admin` 审核权限仍未替代真实 RBAC。
