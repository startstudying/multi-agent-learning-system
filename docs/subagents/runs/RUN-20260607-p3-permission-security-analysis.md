# RUN-20260607 P3-4 权限与安全分析

## 结论

P3-4 当前处于“最小高风险收口已完成，但生产 RBAC 未完成”的状态。最高优先级是把 `teacher/admin` 字符串判断和全局审核队列改为基于 `Course.teacherId`、`ResourceGenerationTask.goalId`、`learnerId` 的对象级授权。

## 高风险点

1. `DevAuthFilter` 允许通过 `X-User-Id` 伪造身份，生产环境需要替换真实认证。
2. `ReviewGovernanceService` 只按固定 `teacher/admin` 判断，未限制教师课程范围。
3. `KnowledgeCatalogService` 写接口缺少权限校验，课程创建可指定任意 `teacherId`。

## 本轮推荐切片

先做 Review Gate 课程范围收口：

- `GET /api/reviews/resources`：teacher 只返回自己课程的 reviews，admin 全局。
- `POST /api/reviews/resources/{reviewId}/decision`：teacher 只能处理自己课程的 review，admin 全局。
- 不改 API 合同、不改 DB schema、不新增依赖。

## 建议测试

```bash
cd backend && mvn "-Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest" test
cd backend && mvn test
```

