# Review Gate 审核权限加固证据

## 范围

本切片加固 Review Gate 审核接口权限：

- `GET /api/reviews/resources`
- `POST /api/reviews/resources/{reviewId}/decision`

临时策略为：

```text
X-User-Id == teacher 或 admin 可访问审核接口
其他用户返回 403
```

## 代码证据

- `backend/src/main/java/com/learningos/agent/api/ResourceReviewController.java`
  - 注入 `CurrentUserService`。
  - list/decision 调用服务前传入 `currentUserService.currentUserId()`。
- `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java`
  - `listResourceReviews(reviewerUserId, status)` 在查询 review 前执行 `assertReviewerAccess(...)`。
  - `decide(reviewerUserId, reviewId, request)` 在校验 decision 和查询 review/resource/task 前执行 `assertReviewerAccess(...)`。
  - 非 `teacher/admin` 统一抛出 `ApiException(ErrorCode.FORBIDDEN, "Resource review access denied")`。
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java`
  - 覆盖 student list 403、student decision 403、admin list 正常、admin decision 正常。
  - 403 body 断言不包含 `taskId/reviewId/resourceId`。
- `backend/src/test/java/com/learningos/agent/application/ReviewGovernanceServiceTest.java`
  - 覆盖 student list/decision 在缺少 review fixture 时直接 403，证明 guard 在详情查询前执行。

## TDD 过程

### RED

先添加权限测试后运行：

```powershell
cd backend
mvn "-Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest" test
```

第一次运行未到达 Review Gate 测试，编译阶段被无关 RAG Document upload 签名不一致阻断：

```text
DocumentController.java: DocumentService.upload 参数数量不匹配
```

该文件不在 Worker C 允许修改范围内，未修改。

### GREEN

实现 Review Gate 权限 guard 后再次运行：

```powershell
cd backend
mvn "-Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest" test
```

结果：

```text
ResourceReviewControllerTest: Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
ReviewGovernanceServiceTest: Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

合计：

```text
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
```

## 安全检查

- MyBatis `${}` 扫描：

```text
No risky dollar-brace usage found
```

- hardcoded secret 扫描脚本未能运行：本地技能脚本 `find-hardcoded-secrets.ps1` 存在字符串终止符解析错误。未将该脚本失败视为项目漏洞。

## 架构漂移检查

| 检查项 | 结果 | 说明 |
|---|---|---|
| Controller only handles HTTP/current user | PASS | Controller 只传入当前用户并委托 Service |
| Service contains permission and business logic | PASS | 权限 guard 位于 `ReviewGovernanceService` |
| No prompt-based permission | PASS | 权限在后端代码中检查 |
| No new dependency | PASS | 未改 `pom.xml` |
| No DB change | PASS | 未新增 migration |
| No Orchestrator/RAG change | PASS | 未修改相关生产代码 |

## 限制

- 当前 `teacher/admin` 是临时 header/user-id 策略，不是真实 RBAC。
- 后续需要课程/班级级授权，避免教师全局查看所有 review。
