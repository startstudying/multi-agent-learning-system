# SPEC - Review Gate 课程范围收口

状态：已完成（2026-06-07）。

## 1. 目标

在不改 API 合同和数据库 schema 的前提下，把 Review Gate 的资源审核权限从临时 `teacher/admin` 收缩到“管理员全局、教师仅自己课程”。

## 2. 追溯

- PRD：`docs/product/PRD-20260607-review-gate-course-scope-hardening.md`
- REQ：`docs/requirements/REQ-20260607-review-gate-course-scope-hardening.md`
- TODO：`docs/planning/backend-architecture-todolist.md` P3-4

## 3. 边界定义

### 3.1 权限模型

- `admin`：可访问全部 resource reviews 和 decision。
- `teacher`：仅可访问 `ResourceGenerationTask.goalId` 对应课程中 `Course.teacherId == currentUserId` 的 reviews。
- 其他用户：拒绝访问。

### 3.2 课程归属链路

```text
ResourceReview.generationTaskId
-> ResourceGenerationTask.id
-> ResourceGenerationTask.goalId
-> Course.id
-> Course.teacherId
```

### 3.3 服务边界

- `ResourceReviewController` 只传递当前用户和请求参数。
- `ReviewGovernanceService` 负责权限判定和课程范围过滤。
- `CourseRepository` 仅用于课程归属查询。

## 4. API 契约

### GET /api/reviews/resources

- `admin` 返回全部可见 review。
- `teacher` 返回自己课程的 review 子集。
- `student` 返回 `FORBIDDEN`。

### POST /api/reviews/resources/{reviewId}/decision

- `admin` 可决策全部 review。
- `teacher` 仅可决策自己课程的 review。
- 非授权用户返回 `FORBIDDEN`。
- 非授权决策不得泄露 review/resource/task 详情。

## 5. 实现规则

1. 在 `ReviewGovernanceService` 注入 `CourseRepository`。
2. 新增课程范围判断 helper，例如 `canAccessReview(userId, review)` / `assertCanReviewTask(userId, task)`。
3. `listResourceReviews(...)` 先做角色门禁，再按课程范围过滤 review。
4. `decide(...)` 在持久化前校验当前用户是否属于该 review 的课程教师或管理员。
5. 课程不存在时，教师视为无权限；管理员仍可处理原有记录。
6. 不新增数据库字段，不修改 `ResourceReview` 状态模型。

## 6. 错误处理

- 未授权：`FORBIDDEN` / `"Resource review access denied"`
- 记录缺失：沿用现有 `NOT_FOUND`

## 7. 测试策略

- `ResourceReviewControllerTest`：
  - teacher 只能列出自己课程 review
  - teacher 不能处理其他课程 review
  - admin 保持全局能力
  - student 继续被拒绝
- `ReviewGovernanceServiceTest`：
  - list 只返回授权课程
  - decide 只允许授权课程
  - course 归属缺失时 teacher 无权限
