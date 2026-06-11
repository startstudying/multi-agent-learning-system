# Review Gate 审核权限加固 PRD

## 背景

当前 Review Gate 已能通过状态模型阻止未发布资源面向学生释放，但审核队列和审核决策接口仍缺少教师/管理员权限边界。普通学生如果直接调用 `/api/reviews/resources` 或 `/api/reviews/resources/{reviewId}/decision`，可能看到或处理不属于学生端的数据。

## 目标

- 只有教师或管理员可以查看资源审核队列。
- 只有教师或管理员可以提交审核决策。
- 普通学生或默认开发用户访问审核接口时返回 403。
- 拒绝访问时不加载、不返回 review/task/resource 详情。

## 非目标

- 不引入完整 RBAC、JWT、Sa-Token 或数据库角色模型。
- 不改 Review Gate 状态模型。
- 不改 Orchestrator、RAG Document upload、IndexService。
- 不改前端页面。

## 临时权限策略

项目当前只有 `CurrentUserService` 和 `X-User-Id` 开发期用户机制，尚无真实角色模型。本切片采用最小临时契约：

```text
X-User-Id == teacher 或 X-User-Id == admin 可访问 Review Gate 审核接口
其他用户一律 403
```

后续应替换为真实 RBAC，并支持教师只访问授权课程/班级的审核记录。

## 成功标准

- student 调用 review list 返回 403。
- student 调用 review decision 返回 403。
- teacher 可正常 list 和 decision。
- admin 可正常 list 和 decision。
- 403 响应不包含 reviewId、resourceId、generationTaskId 等业务详情。
