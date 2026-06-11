# EVIDENCE-20260609 P3-4 子任务：LearningPath create legacy overload cleanup

## Outcome

PASS。

本子任务已删除 LearningPath 创建路径中的 legacy subject-name admin 判断入口：

- 删除 `LearningWorkflowService.createPathForUser(String, CreateLearningPathRequest)`。
- 删除 `LearningWorkflowService.isAdmin(String)`。
- 保留 roles-first `createPathForUser(String, boolean, boolean, CreateLearningPathRequest)`。

## Subagent Evidence

| Expert | Report | Verdict |
|---|---|---|
| Architect | `docs/subagents/runs/RUN-20260609-p3-4-learning-path-create-legacy-overload-cleanup-architect.md` | 删除旧 overload/helper，不影响 HTTP 主路径 |
| Security Reviewer | `docs/subagents/runs/RUN-20260609-p3-4-learning-path-create-legacy-overload-cleanup-security.md` | MEDIUM 风险；删除为首选 |
| Test Engineer | `docs/subagents/runs/RUN-20260609-p3-4-learning-path-create-legacy-overload-cleanup-test.md` | 反射 guard + roles-first admin 回归测试 |
| Integration Review | `docs/subagents/runs/RUN-20260609-p3-4-learning-path-create-legacy-overload-cleanup-integration-review.md` | PASS |

## Code Evidence

检索确认生产代码中旧签名/helper 不存在：

```powershell
rg -n "createPathForUser\(String currentUserId, CreateLearningPathRequest|private boolean isAdmin\(String userId\)|createPathForUserLegacyOverloadIsRemoved|learningWorkflowServiceSubjectNameAdminHelperIsRemoved|createPathForUserRolesFirstOverloadAllowsExplicitAdminCrossLearnerCreation" backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java backend/src/test/java/com/learningos/learning/application/LearningWorkflowServiceTest.java
```

输出只剩测试方法：

```text
backend/src/test/java/com/learningos/learning/application/LearningWorkflowServiceTest.java:84:    void createPathForUserLegacyOverloadIsRemoved() {
backend/src/test/java/com/learningos/learning/application/LearningWorkflowServiceTest.java:94:    void learningWorkflowServiceSubjectNameAdminHelperIsRemoved() {
backend/src/test/java/com/learningos/learning/application/LearningWorkflowServiceTest.java:101:    void createPathForUserRolesFirstOverloadAllowsExplicitAdminCrossLearnerCreation() {
```

## RED

Command:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowServiceTest#createPathForUserLegacyOverloadIsRemoved+learningWorkflowServiceSubjectNameAdminHelperIsRemoved test
```

Result:

```text
Tests run: 2, Failures: 2, Errors: 0, Skipped: 0
```

失败原因符合预期：旧 public overload 和 private `isAdmin(String)` 仍存在。

## GREEN / Focused

Command:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowServiceTest#createPathForUserLegacyOverloadIsRemoved+learningWorkflowServiceSubjectNameAdminHelperIsRemoved+createPathForUserRolesFirstOverloadAllowsExplicitAdminCrossLearnerCreation test
```

Result:

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Compile Guard

Command:

```powershell
cd D:\多元agent\backend
mvn --% -DskipTests compile
```

Result:

```text
BUILD SUCCESS
```

## Adjacent

Command:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=LearningWorkflowServiceTest,LearningWorkflowControllerTest test
```

Result:

```text
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Full Backend

Command:

```powershell
cd D:\多元agent\backend
mvn test
```

Result:

```text
Tests run: 477, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## Acceptance Criteria

- [x] `LearningWorkflowService` no longer exposes public `createPathForUser(String, CreateLearningPathRequest)`.
- [x] `LearningWorkflowService` no longer contains private `isAdmin(String)` subject-name helper.
- [x] Roles-first `createPathForUser(String, boolean, boolean, CreateLearningPathRequest)` remains intact.
- [x] Explicit admin can still create a learning path for another learner through roles-first service call.
- [x] No REST API contract, DTO, DB, dependency, frontend, ResourceGeneration, Orchestrator, Agent Trace, Review Gate, or formal OAuth2/JWK/Spring Security change.
- [x] Focused, adjacent, and full backend tests passed.
- [x] Parent P3-4 remains not fully complete.

## Remaining Scope

P3-4 still has broader class/course authorization matrix, formal OAuth2/JWK/Spring Security, and broader permission penetration tests remaining.
