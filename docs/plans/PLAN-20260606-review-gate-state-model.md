# Review Gate 状态模型加固计划

## 1. 追溯

- PRD：`docs/product/PRD-20260606-review-gate-state-model.md`
- REQ：`docs/requirements/REQ-20260606-review-gate-state-model.md`
- SPEC：`docs/specs/SPEC-20260606-review-gate-state-model.md`

## 2. Skill Selection Report

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 本任务是后端 Review Gate 增强，必须走文档和验收链路。 |
| `ai-learning-agent-development` | 约束教育多 Agent 系统模块边界。 |
| `critic-review-agent` | 涉及资源审核状态、教师决策和发布治理。 |
| `spring-ai-agent-backend` | 涉及 Spring Boot Controller/Service/Repository 分层。 |
| `test-driven-development` | 行为变更必须先写失败测试。 |
| `verification-before-completion` | 完成前运行聚焦测试和必要全量测试。 |

Missing skills: 无。

GitHub Research Needed: No。当前 Review Gate 已有基础实现，本轮按本仓库状态模型加固，不需要外部参考。

New Project-Specific Skill To Create: No。

## 3. Subagent Decision

Use Subagents: Yes。

Reason: 用户明确要求多 subagent 并行开发，且完整 TODO 涉及 Orchestrator、RAG、Review Gate、安全与测试。

Parallelism Level: L1。

Selected Subagents:

- 架构审查：P0 剩余项和下一任务建议。
- 测试策略：Review Gate、RAG、workflow 剩余项测试建议。
- 安全审查：权限、幂等、审计风险。
- RAG/Agent 审查：RAG query replay、citation governance。
- 后端实现审查：选择最小可落地 P0 切片。
- 文档集成审查：文档链路和验收文案。

Implementation Mode: Single Codex。本切片写入文件集中，避免并行实现冲突。

## 4. Confidence Check

| Check | Status | Evidence |
|---|---|---|
| No duplicate implementation | PASS | 现有 Review Gate 有基础状态，但无 `REJECTED/PUBLISHED` 和结构化审核字段。 |
| Architecture compliance | PASS | 复用 `ReviewGovernanceService`、JPA entity、MockMvc/JPA tests。 |
| Official docs verified | N/A | 不新增外部 API 或依赖。 |
| OSS references | N/A | 本切片是本仓库业务状态加固，不需要外部代码。 |
| Root cause identified | PASS | TODO P0-4 明确要求补齐状态和结构化审核日志。 |

Confidence: 0.92。可以进入 TDD 实现。

## 5. 实施阶段

| 阶段 | 说明 | 关联任务 | 状态 |
|---|---|---|---|
| 1 | 写 RED 测试覆盖 `REJECTED/PUBLISHED/structured fields` | TASK | 已完成 |
| 2 | 实现状态常量、entity 字段、service 决策逻辑、controller DTO | TASK | 已完成 |
| 3 | 新增 V6 migration 和文本测试 | TASK | 已完成 |
| 4 | 运行聚焦测试和全量测试 | TASK | 已完成 |
| 5 | 更新 Evidence / Acceptance / Memory / Changelog / TODO | TASK | 已完成 |

## 6. 文件变更清单

| 文件 | 操作 | 阶段 | 负责人 |
|---|---|---|---|
| `backend/src/main/java/com/learningos/agent/application/AgentRuntimeConstants.java` | 修改 | 2 | Codex |
| `backend/src/main/java/com/learningos/agent/domain/ResourceReview.java` | 修改 | 2 | Codex |
| `backend/src/main/java/com/learningos/agent/api/ResourceReviewController.java` | 修改 | 2 | Codex |
| `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java` | 修改 | 2 | Codex |
| `backend/src/main/resources/db/migration/V6__resource_review_governance.sql` | 新增 | 3 | Codex |
| `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java` | 修改 | 1 | Codex |
| `backend/src/test/java/com/learningos/agent/application/ReviewGovernanceServiceTest.java` | 修改 | 1 | Codex |
| `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java` | 修改 | 3 | Codex |

## 7. 风险评估

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| `APPROVED` 与 `PUBLISHED` 语义变化影响旧测试 | 中 | 明确 review 决策保留 `APPROVED`，任务/资源 release 状态用 `PUBLISHED` |
| V6 未做真实 MySQL smoke | 中 | 保留 P3-1，增加 migration 文本测试 |
| 教师权限未加固 | 中 | 本轮不扩权限模型，后续 P3 权限任务处理 |

## 8. 测试策略

- 聚焦：`mvn "-Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest,SchemaConvergenceMigrationTest" test`
- 回归：`mvn "-Dtest=ResourceGenerationControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest" test`
- 全量：`mvn test`
