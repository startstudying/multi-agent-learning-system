# ACCEPT - Review Gate 课程范围收口

## 1. 验收结论

通过。

本切片已把资源审核 list/decision 权限从临时 `teacher/admin` 收口为：

- `admin` 全局可查看和处理 review。
- `teacher` 只能查看和处理自己课程的 review。
- student/无关用户返回 `FORBIDDEN`，不返回 review/resource/task 明细。

代码审查发现的“非管理员可通过 `404` vs `403` 探测 reviewId 是否存在”问题已修复并补回归测试。

## 2. 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| teacher list 只返回自己课程 review | PASS | `ResourceReviewControllerTest.teacherListsOnlyReviewsForOwnCourses` |
| teacher 不能处理其他课程 review | PASS | `ResourceReviewControllerTest.teacherCannotDecideReviewForForeignCourse` |
| teacher 不能通过 missing reviewId 区分对象存在性 | PASS | `ResourceReviewControllerTest.teacherCannotDistinguishMissingReviewFromForbiddenReview` |
| admin list/decision 保持全局能力 | PASS | `adminCanListResourceReviews`, `adminCanSubmitReviewDecision` |
| student list/decision 继续返回 403 | PASS | `studentCannotListResourceReviews`, `studentCannotSubmitReviewDecision` |
| NO_SOURCE 审核冲突逻辑不回退 | PASS | `ResourceGenerationControllerTest.noSourceGeneratedResourcesRequireReviewAndRejectApproval` |
| 发布后学生资源可见逻辑不回退 | PASS | `ResourceGenerationControllerTest.learnerResourcesRemainHiddenUntilGovernanceAllowsRelease` |
| 无 API / DB / dependency 变更 | PASS | Context Pack 与 diff 检查；无迁移、无 pom 变更 |

## 3. 测试验收

| 命令 | 结果 |
|---|---|
| `mvn --% -Dtest=ResourceReviewControllerTest#teacherCannotDistinguishMissingReviewFromForbiddenReview test` | 1 test, 0 failures |
| `mvn --% -Dtest=ReviewGovernanceServiceTest,ResourceReviewControllerTest,ResourceGenerationControllerTest test` | 31 tests, 0 failures |
| `mvn --% -Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest,ResourceGenerationControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,AssessmentControllerTest,AgentTraceControllerTest,RagQueryServiceTest,DocumentControllerTest test` | 82 tests, 0 failures |
| `mvn test` | 238 tests, 0 failures, 0 errors, 1 skipped |

## 4. 代码审查结论

子代理代码审查发现 2 个 Medium：

- reviewId 存在性 oracle：已修复。非管理员 missing review 和 foreign review 均返回 `FORBIDDEN`。
- list 接口 N+1 / in-memory filtering：记录为后续生产化风险，本切片不扩展为仓储层 scoped query。

## 5. Open Items

- 用正式 RBAC/JWT 替代临时 `X-User-Id` 字符串身份。
- 将 Review Gate list 查询改为 scoped repository query 或批量加载，避免大数据量 N+1。
- 继续扩展 RAG KB、学习资源、答题记录、课程/班级数据的完整权限矩阵。
