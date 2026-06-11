# Review Gate 审核权限加固验收

## 验收结论

通过。

本切片已为 Review Gate list 和 decision 接口补齐临时教师/管理员权限边界。普通学生返回 403，teacher/admin 可以正常查看和处理审核记录。拒绝访问时不返回 review/task/resource 详情。

## 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| student list 返回 403 | PASS | `ResourceReviewControllerTest.studentCannotListResourceReviews` |
| student decision 返回 403 | PASS | `ResourceReviewControllerTest.studentCannotSubmitReviewDecision` |
| teacher list/decision 正常 | PASS | `ResourceReviewControllerTest.listsPendingResourceReviewsAndApprovesAllResourcesForTask` |
| admin list 正常 | PASS | `ResourceReviewControllerTest.adminCanListResourceReviews` |
| admin decision 正常 | PASS | `ResourceReviewControllerTest.adminCanSubmitReviewDecision` |
| 未授权时不泄露 review/task/resource 详情 | PASS | 403 body 断言不包含 `taskId/reviewId/resourceId` |
| Service guard 在详情查询前执行 | PASS | `ReviewGovernanceServiceTest.deniesReviewDecisionBeforeLoadingDetailsForStudent` |
| 聚焦测试通过 | PASS | 15 tests, 0 failures, BUILD SUCCESS |

## Open Items

- 用真实 RBAC 替换硬编码 `teacher/admin`。
- 增加教师课程/班级范围过滤。
- 修复本地 `java-security-review` hardcoded-secret 扫描脚本编码/parser 问题，便于后续安全巡检。
