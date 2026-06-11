# REQ - Review Gate 课程范围收口

状态：已完成（2026-06-07）。

## 1. 需求说明

系统必须把资源审核权限收敛为：

- `admin` 可查看和处理所有资源审核
- `teacher` 只能查看和处理自己课程的资源审核
- 其他用户拒绝访问

## 2. 业务规则

1. `GET /api/reviews/resources` 返回当前用户可访问的审核列表。
2. `POST /api/reviews/resources/{reviewId}/decision` 只能对当前用户有权限的课程 review 生效。
3. 教师课程归属通过 `ResourceGenerationTask.goalId` 关联 `Course.id`，再判断 `Course.teacherId`。
4. 如果 review 属于非授权课程，接口返回 `FORBIDDEN`。
5. 越权响应不得返回 review、resource、task 的可读明细。

## 3. 输入输出约束

- 输入用户身份由 `CurrentUserService.currentUserId()` 提供。
- 当前切片不修改请求体结构。
- 响应结构沿用现有 `ResourceReviewSummary`。

## 4. 验收条件

- teacher 仅能列出自己课程的 review。
- teacher 仅能决策自己课程的 review。
- admin 不受课程限制。
- student 访问 review 接口返回 `FORBIDDEN`。
- 现有审核状态流转与结构化审核字段保持不变。
