# Review Gate 审核权限加固规格

## 范围

本切片只加固 Review Gate 审核接口权限：

- `GET /api/reviews/resources`
- `POST /api/reviews/resources/{reviewId}/decision`

## 权限模型

临时策略：

```text
reviewerUserId in ["teacher", "admin"] => allow
otherwise => 403 FORBIDDEN
```

说明：

- `reviewerUserId` 来自 `CurrentUserService.currentUserId()`。
- 该策略写在 `ReviewGovernanceService`，Controller 只负责传入当前用户。
- 拒绝访问必须发生在查询 review/resource/task 前，避免通过错误信息或响应泄露详情。

## API 行为

### GET /api/reviews/resources

Allowed:

```http
X-User-Id: teacher
X-User-Id: admin
```

返回现有 `List<ResourceReviewSummary>`。

Denied:

```http
X-User-Id: alice
X-User-Id: student
缺省 dev_user
```

返回：

```json
{
  "code": "FORBIDDEN",
  "message": "Resource review access denied",
  "data": null
}
```

### POST /api/reviews/resources/{reviewId}/decision

Allowed 用户可以沿用现有审核决策逻辑。

Denied 用户返回 403，并且不校验 reviewId 是否存在、不返回 review/resource/task 详情。

## 服务设计

```text
ResourceReviewController
-> CurrentUserService.currentUserId()
-> ReviewGovernanceService.listResourceReviews(reviewerUserId, status)
-> assertReviewerAccess(reviewerUserId)
-> query review list
```

```text
ResourceReviewController
-> CurrentUserService.currentUserId()
-> ReviewGovernanceService.decide(reviewerUserId, reviewId, request)
-> assertReviewerAccess(reviewerUserId)
-> validate decision
-> load review/resource/task
-> persist decision
```

## 测试策略

- `ResourceReviewControllerTest.studentCannotListResourceReviews`
- `ResourceReviewControllerTest.studentCannotSubmitReviewDecision`
- `ResourceReviewControllerTest.adminCanListResourceReviews`
- `ResourceReviewControllerTest.adminCanSubmitReviewDecision`
- `ReviewGovernanceServiceTest.deniesReviewListBeforeLoadingDetailsForStudent`
- `ReviewGovernanceServiceTest.deniesReviewDecisionBeforeLoadingDetailsForStudent`

## 架构漂移检查

| 检查项 | 结果 |
|---|---|
| Controller 只处理 HTTP 和 current user 传递 | PASS |
| Service 承载权限和业务逻辑 | PASS |
| 不新增依赖 | PASS |
| 不改数据库 | PASS |
| 不改 Orchestrator/RAG | PASS |

## Open Issues

- 后续替换为真实 RBAC 时，应移除硬编码 `teacher/admin` 用户 id。
- 后续应增加教师课程/班级级别授权过滤，而不是全局可见所有 review。
