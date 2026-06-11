# EVIDENCE - P3-4-K 权限渗透测试矩阵补齐

## 1. 范围

本证据记录 P3-4-K 的当前 transitional 权限渗透测试矩阵补齐：

- Auth Context：`staging` 缺少 Bearer 时不信任 `X-User-Id`。
- Analytics：Bearer `ADMIN` role 优先于 spoofed `X-User-Id`，并能驱动 `overview` / `token-budget governance` 这类 admin-only 入口。
- Course / Knowledge Graph：active enrolled student 仍不能写入课程图谱；student course list 不泄露 dropped enrollment course。
- RAG Document：student 即使拥有 public KB 且 enrolled，也不能伪造课程元数据上传课程文档。
- Assessment / Resource / Review / Trace：沿用既有矩阵测试覆盖 owner-only、teacher own-course、admin global、non-admin anti-enumeration。

非目标：

- 不迁移正式 OAuth2 / JWK / Spring Security。
- 不设计 broader class/course domain。
- 不新增依赖、DB schema、前端或模型/RAG runtime 改动。
- 不宣称整个 P3-4 完成。

## 2. Subagent 证据

| 报告 | 结论 |
|---|---|
| `docs/subagents/runs/RUN-20260609-p3-4-permission-matrix-security.md` | 建议优先补 Bearer roles controller 集成矩阵、student write deny、course list inactive/dropped 负向断言、token-budget governance admin-only。第一个专家按用户要求记录为 `gpt-5.5`。 |
| `docs/subagents/runs/RUN-20260609-p3-4-permission-matrix-test.md` | 建议统一 matrix test；实际实现采用相邻测试类补缺口，以复用现有 fixtures 并降低脆弱度。 |
| `docs/subagents/runs/RUN-20260609-p3-4-permission-matrix-architecture.md` | 建议 P3-4-K 只证明当前过渡权限不变量，不混入新安全框架或 class domain。 |

## 3. 代码证据

| 文件 | 证据 |
|---|---|
| `backend/src/test/java/com/learningos/common/auth/DevAuthFilterTest.java` | 新增 `stagingMissingBearerTokenReturnsUnauthorizedEvenWhenUserIdHeaderExists`，验证 staging header-only 身份建立返回 `UNAUTHORIZED`。 |
| `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java` | 新增 JWT helper 和测试配置；新增 `overviewUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`、`tokenBudgetGovernanceUsesBearerAdminRoleAndRejectsSpoofedStudent`、`tokenBudgetGovernanceRejectsHeaderOnlyNonAdminAccess`。 |
| `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java` | 新增 `studentCannotManageCourseGraphEvenWhenActivelyEnrolled`；强化 `courseListIsScopedByCurrentUserRole`，断言 student list 不包含 dropped course id/title。 |
| `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java` | 新增 `rejectsStudentCourseMetadataEvenWhenStudentOwnsPublicKnowledgeBaseAndIsEnrolled`。 |
| `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java` | `overview()` 和 `tokenBudgetGovernance()` 改用 `currentUserService.isAdmin()`，避免 `"admin"` 字符串身份判断被 Bearer role 绕不过。 |
| `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java` | `tokenBudgetGovernance(...)` 接收 `boolean currentUserAdmin` 并使用 role-derived admin gate。 |

## 4. RED 证据

新增 Bearer role 驱动 admin-only analytics 测试后，首次运行 focused 命令出现预期 RED：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DevAuthFilterTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,DocumentControllerTest test
```

关键失败：

```text
AnalyticsControllerTest.overviewUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader: expected 200 but was 403
AnalyticsControllerTest.tokenBudgetGovernanceUsesBearerAdminRoleAndRejectsSpoofedStudent: expected 200 but was 403
```

根因：

- analytics admin-only 入口仍依赖字符串 `"admin"`。
- Bearer token 已建立 `UserContext.roles=["ADMIN"]`，但 controller/service 没有使用 roles-first `CurrentUserService.isAdmin()` 作为 admin gate。

## 5. GREEN / Regression 证据

### 5.1 Focused verification

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=DevAuthFilterTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,DocumentControllerTest test
```

结果：

```text
Tests run: 65, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-09T02:03:08+08:00
```

### 5.2 Adjacent regression

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,DocumentControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,AnalyticsControllerTest,DevAuthFilterTest test
```

结果：

```text
Tests run: 119, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-06-09T02:04:30+08:00
```

### 5.3 Full backend verification

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 367, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Finished at: 2026-06-09T02:06:38+08:00
```

说明：Maven 输出包含 Mockito dynamic agent / JVM sharing warnings，不影响测试结果。

## 6. 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| Backend layering | PASS | 本次生产修复只在 analytics controller/service 使用 role-derived admin gate；对象级授权仍在 service 层。 |
| Frontend rules | PASS | 未修改 `frontend/**`。 |
| Agent / RAG rules | PASS | 未修改 Agent/RAG runtime；仅补 RAG document 权限测试。 |
| Security | PASS | 权限判断在后端代码完成；Bearer role 优先于 spoofed header 的业务入口测试已覆盖。 |
| API / Database | PASS | 未新增 API path、DTO、schema、migration 或依赖。 |

## 7. 限制

- P3-4-K 仅完成当前 transitional permission matrix。
- broader class/course 权限模型仍未完成。
- formal OAuth2 / JWK / Spring Security 迁移仍未完成。
- PromptVersion / Evaluation endpoints full RBAC matrix、RAG KB management full matrix 仍是后续安全切片。
- 当前工作区不是 git repository，本证据以文件内容和 Maven 输出为准，不依赖 git diff。
