# ACCEPT - P3-4-K 权限渗透测试矩阵补齐

## 1. 验收结论

P3-4-K 当前 transitional 权限渗透测试矩阵验收通过。

本次只验收 P3-4 已完成切片的跨模块回归矩阵和 Bearer roles 业务入口贯通，不代表 broader class/course 权限模型、formal OAuth2/JWK/Spring Security 或全量端点 RBAC 已完成。

## 2. 需求验收

| 需求 | 状态 | 证据 |
|---|---|---|
| REQ-P3-4-K-1 跨模块权限矩阵 | 通过 | 相邻测试类补齐 auth、analytics、course、RAG document；assessment/resource/review/trace 由 adjacent regression 覆盖。 |
| REQ-P3-4-K-2 Bearer 优先于 spoofed `X-User-Id` | 通过 | `overviewUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`、`tokenBudgetGovernanceUsesBearerAdminRoleAndRejectsSpoofedStudent`。 |
| REQ-P3-4-K-3 staging/prod 不信任 header-only auth | 通过 | `stagingMissingBearerTokenReturnsUnauthorizedEvenWhenUserIdHeaderExists`。 |
| REQ-P3-4-K-4 student 不能写 teacher-managed surface | 通过 | `studentCannotManageCourseGraphEvenWhenActivelyEnrolled`、`rejectsStudentCourseMetadataEvenWhenStudentOwnsPublicKnowledgeBaseAndIsEnrolled`，既有 review student deny 测试。 |
| REQ-P3-4-K-5 teacher foreign course deny | 通过 | `CourseKnowledgeControllerTest`、`AnalyticsControllerTest`、`ResourceReviewControllerTest` adjacent regression。 |
| REQ-P3-4-K-6 student foreign learner deny | 通过 | `AssessmentControllerTest`、`ResourceGenerationControllerTest`、`AnalyticsControllerTest` adjacent regression。 |
| REQ-P3-4-K-7 non-admin anti-enumeration | 通过 | answer/wrong-question、resource task/trace、document/reindex/index-task、review decision 既有矩阵在 adjacent regression 中通过。 |
| REQ-P3-4-K-8 admin global / missing semantics | 通过 | course、assessment、document、analytics 既有矩阵在 adjacent/full regression 中通过。 |
| REQ-P3-4-K-9 analytics governance admin-only | 通过 | `tokenBudgetGovernanceRejectsHeaderOnlyNonAdminAccess`，以及 Bearer admin role GREEN。 |
| REQ-P3-4-K-10 不新增依赖/schema/frontend | 通过 | 未修改 `backend/pom.xml`、migration、`frontend/**`。 |

## 3. 非功能验收

| 项 | 状态 | 说明 |
|---|---|---|
| Focused verification | 通过 | 65 run, 0 failures, 0 errors, 0 skipped |
| Adjacent regression | 通过 | 119 run, 0 failures, 0 errors, 0 skipped |
| Full backend verification | 通过 | 367 run, 0 failures, 0 errors, 1 skipped |
| RED/GREEN | 通过 | Bearer admin role analytics tests 先 RED，修复后 GREEN。 |
| 依赖审查 | 不需要 | 未新增依赖。 |
| 架构漂移 | 通过 | Controller 使用 role helper；未引入新架构边界。 |

## 4. 验证命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DevAuthFilterTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,DocumentControllerTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,DocumentControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,AnalyticsControllerTest,DevAuthFilterTest test
mvn test
```

最终全量结果：

```text
Tests run: 367, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 5. 未验收 / 后续项

- broader class/course 权限模型。
- formal OAuth2/JWK/Spring Security。
- PromptVersion / Evaluation endpoints full RBAC matrix。
- RAG KB management full permission matrix。
- Teacher class summary missing vs foreign course 语义复核。
