# PLAN - Review Gate 课程范围收口

状态：已完成（2026-06-07）。

## 1. 目标

完成 Review Gate 教师课程范围收口，阻断跨课程审核越权。

## 2. 文件变更

### 计划修改

- `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java`
- `backend/src/main/java/com/learningos/agent/api/ResourceReviewController.java`（仅如需传递额外当前用户信息，优先不改）
- `backend/src/test/java/com/learningos/agent/application/ReviewGovernanceServiceTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`（仅用于补齐 Review Gate 回归测试所需课程归属夹具）
- `docs/product/PRD-20260607-review-gate-course-scope-hardening.md`
- `docs/requirements/REQ-20260607-review-gate-course-scope-hardening.md`
- `docs/specs/SPEC-20260607-review-gate-course-scope-hardening.md`
- `docs/plans/PLAN-20260607-review-gate-course-scope-hardening.md`
- `docs/tasks/TASK-20260607-review-gate-course-scope-hardening.md`
- `docs/context/CONTEXT-20260607-review-gate-course-scope-hardening.md`

## 3. 实施步骤

1. [x] 先补测试，覆盖 teacher/admin/student 三类 review list/decision 行为。
2. [x] 在 `ReviewGovernanceService` 注入 `CourseRepository`。
3. [x] 增加课程归属 helper，基于 `ResourceGenerationTask.goalId -> Course.teacherId` 判定教师权限。
4. [x] 收紧 list 和 decision 的服务层权限判断。
5. [x] 回归审核状态流转与已有 `PUBLISHED` 逻辑。
6. [x] 处理 code review 发现的 reviewId 存在性 oracle。
7. [x] 更新 TODO、Changelog、Memory、Evidence、Acceptance、Retrospective。

## 4. 风险

- 现有测试数据若未创建课程记录，会导致 teacher 课程范围判断失效，需要同步修正测试夹具。
- `goalId` 语义在本项目中实际被用作课程标识，若未来改语义，需要再做一轮权限适配。
- 不应把完整 RBAC 一次性并进来，否则会扩大到 Knowledge / RAG / Assessment 多模块。

## 5. 验证命令

```bash
cd backend && mvn "-Dtest=ReviewGovernanceServiceTest,ResourceReviewControllerTest" test
cd backend && mvn test
```

## 6. 完成标准

- [x] teacher 只能处理自己课程的 review
- [x] admin 仍可全局处理
- [x] student 继续被拒绝
- [x] missing/foreign review 对非管理员不形成对象存在性 oracle
- [x] 测试通过
- [x] 文档、证据、验收、changelog、memory 完成更新
