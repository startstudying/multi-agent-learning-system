# Review Gate 强约束验收

## 验收结论

通过。

本轮 P0-4 最小安全切片已完成：未审核 AI 生成学习资源不会通过资源生成创建响应、任务详情响应或幂等重放响应返回 `markdownContent`；全部审核通过后，任务详情和正式学生资源接口可返回正文。

## 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| 创建资源生成任务后，响应中资源正文不可见 | PASS | `ResourceGenerationControllerTest.createsResourceGenerationTaskAndExposesAgentTrace` |
| 查询未审核任务详情时，资源正文不可见 | PASS | `ResourceGenerationControllerTest.createsResourceGenerationTaskAndExposesAgentTrace` |
| 重复 `requestId` 重放未审核任务时，资源正文仍不可见 | PASS | `ResourceGenerationControllerTest.returnsExistingTaskForRepeatedLearnerRequestIdWithoutDuplicatingResourcesOrTrace` |
| 未审核正式学生资源接口仍返回 403 | PASS | `ResourceGenerationControllerTest.learnerResourcesRemainHiddenUntilGovernanceAllowsRelease` |
| 全部审核通过后，任务详情和正式学生资源接口都返回正文 | PASS | `ResourceGenerationControllerTest.learnerResourcesRemainHiddenUntilGovernanceAllowsRelease` |
| 权限和审核判断在 Service 层完成 | PASS | `ResourceGenerationService.toResponse(...)` 与 `getLearnerResources(...)` |
| 全量后端测试通过 | PASS | `cd backend && mvn test`，91 tests，0 failures |

## 未纳入本轮的后续项

- 资源状态枚举仍需补齐 `DRAFT`、`PENDING_CRITIC`、`APPROVED`、`REVISION_REQUESTED`、`REJECTED`、`PUBLISHED`。
- 审核原因、引用检查结果、安全检查结果、修订建议仍需结构化字段。
- 教师/管理员审核详情视图需要在角色权限补齐后单独设计，避免和学生可见响应混用。
