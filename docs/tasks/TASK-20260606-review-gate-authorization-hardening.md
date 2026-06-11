# Review Gate 审核权限加固任务

## 目标

补齐资源审核队列和审核决策接口的教师/管理员权限边界，阻止普通学生查看或处理 review。

## 允许修改文件

- `backend/src/main/java/com/learningos/agent/api/ResourceReviewController.java`
- `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java`
- `backend/src/test/java/com/learningos/agent/application/ReviewGovernanceServiceTest.java`
- `docs/product/PRD-20260606-review-gate-authorization-hardening.md`
- `docs/requirements/REQ-20260606-review-gate-authorization-hardening.md`
- `docs/specs/SPEC-20260606-review-gate-authorization-hardening.md`
- `docs/plans/PLAN-20260606-review-gate-authorization-hardening.md`
- `docs/tasks/TASK-20260606-review-gate-authorization-hardening.md`
- `docs/context/CONTEXT-20260606-review-gate-authorization-hardening.md`
- `docs/evidence/EVIDENCE-20260606-review-gate-authorization-hardening.md`
- `docs/acceptance/ACCEPT-20260606-review-gate-authorization-hardening.md`

## 禁止修改文件

- Orchestrator 生产代码
- RAG Document upload 生产代码
- IndexService
- shared memory/changelog/backend todo
- 前端文件
- 构建配置

## 步骤

1. 写失败测试。
2. 实现最小权限 guard。
3. 运行聚焦测试。
4. 写证据和验收。

## 测试命令

```powershell
cd backend
mvn "-Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest" test
```

## 完成标准

- [x] student list 403。
- [x] student decision 403。
- [x] teacher list/decision 正常。
- [x] admin list/decision 正常。
- [x] 403 不泄露 review/task/resource 详情。
- [x] 聚焦测试通过。

## 状态

已完成。
