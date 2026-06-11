# Review Gate 状态模型加固验收

## 验收结论

通过。

本轮 P0-4 状态模型加固已完成：Review 决策支持 `REJECTED`，审核原因、引用检查、安全检查和修订建议可持久化并返回；全部审核通过后，任务和资源进入 `PUBLISHED`；学生端 release 判断只接受 `PUBLISHED` 且全部 review 为 `APPROVED` 的任务。

## 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| Review 状态集合包含 `DRAFT/PENDING_CRITIC/APPROVED/REVISION_REQUESTED/REJECTED/PUBLISHED` | PASS | `AgentRuntimeConstants.REVIEW_STATUSES` |
| `POST /api/reviews/resources/{reviewId}/decision` 支持 `REJECTED` | PASS | `ResourceReviewControllerTest.rejectedDecisionPersistsStructuredAuditFieldsAndKeepsResourceUnpublished` |
| 审核结构化字段可持久化和返回 | PASS | `ResourceReview.reason/citationCheck/safetyCheck/revisionSuggestion` 与 controller/service tests |
| `REVISION_REQUESTED` 后资源和任务不可发布 | PASS | `ResourceReviewControllerTest.revisionRequestUpdatesOnlyTargetResourceAndKeepsTaskPending` |
| 任一 review `REJECTED` 后资源和任务不可发布 | PASS | `ReviewGovernanceServiceTest.rejectedReviewBlocksLearnerReleaseAndStoresAuditReason` |
| 全部 review `APPROVED` 后任务和资源进入 `PUBLISHED` | PASS | `ResourceReviewControllerTest.listsPendingResourceReviewsAndApprovesAllResourcesForTask` |
| `canReleaseToLearner` 只允许 `PUBLISHED` 且全部 review approved | PASS | `ReviewGovernanceServiceTest.learnerReleaseRequiresPublishedTaskAndResourcesNotOnlyApprovedReviews` |
| V6 migration 文本覆盖新增字段 | PASS | `SchemaConvergenceMigrationTest.v6MigrationAddsStructuredResourceReviewGovernanceColumns` |
| 全量后端测试通过 | PASS | `cd backend; mvn test`，121 tests，0 failures |

## 未纳入本轮的后续项

- Review Gate 教师/管理员角色权限仍未加固，普通用户是否可调用审核接口需要 P3-4 权限任务处理。
- 真实 Critic Agent LLM 自动审核没有接入，本轮只处理人工/已有 review 记录的治理状态。
- V6 migration 已有文本测试，但尚未做真实 MySQL 8 Flyway smoke，归入 P3-1。
