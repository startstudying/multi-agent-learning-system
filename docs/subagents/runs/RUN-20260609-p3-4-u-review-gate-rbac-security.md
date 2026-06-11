# RUN - P3-4-U 安全专家报告

## 结论

优先实施 `Review Gate ResourceReview roles-first RBAC`。风险等级：HIGH。

## 关键证据

- `ResourceReviewController` 当前只向 `ReviewGovernanceService` 传 `currentUserService.currentUserId()`。
- `ReviewGovernanceService` 通过 `"admin".equals(reviewerUserId)` 判断管理员。
- Teacher 权限通过 `courseRepository.existsByTeacherId(reviewerUserId)` 和 `Course.teacherId == reviewerUserId` 推断，没有显式使用 `UserContext.roles()`。
- `ResourceReviewControllerTest` 缺少 Bearer `ADMIN/TEACHER`、spoofed `X-User-Id`、`USER sub=admin/teacher_1` role-confusion 覆盖。

## 推荐测试点

1. Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id=alice` 可以 list/decision。
2. Bearer `USER sub=admin` 不能 list/decision，响应无 review/task/resource id。
3. Bearer `TEACHER sub=instructor_1` 可审核 `Course.teacherId=instructor_1` 的任务，不依赖 `teacher_` 前缀。
4. Bearer `USER sub=teacher_1` 即使匹配 `Course.teacherId` 也不能审核。
5. Bearer teacher 对 missing review 与 foreign review 返回安全 `FORBIDDEN`。

## 禁止混入范围

不改 formal OAuth2/JWK/Spring Security、数据库 schema、审核状态机、发布规则、NO_SOURCE 规则、ResourceGeneration 创建/Orchestrator 流程、全仓 legacy overload 或依赖。
