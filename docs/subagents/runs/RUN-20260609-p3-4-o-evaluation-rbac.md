# RUN-20260609 P3-4-O Evaluation Set / Run RBAC 子代理分析

## 1. 任务

继续推进 `docs/planning/backend-architecture-todolist.md` 中 P3-4 权限矩阵，选择下一刀：

- `POST /api/evaluation-sets`
- `GET /api/evaluation-sets`
- `GET /api/evaluation-sets/{setId}`
- `POST /api/evaluation-runs`
- `GET /api/evaluation-runs/comparison`

目标是将 Evaluation Set / Run HTTP 管理主路径从 legacy subject 字符串推断迁移为 roles-first RBAC。

## 2. Skill Selection Gate

| Task Type | Selected Skills | Why |
|---|---|---|
| 后端安全/RBAC 修复 | `feature-development-workflow` | 项目强制 Spec-first 全流程 |
| 后端安全/RBAC 修复 | `auth-context-boundary` | Bearer JWT roles 与 `X-User-Id` fallback 边界 |
| 后端安全/RBAC 修复 | `object-scope-authorization` | Evaluation set/run 对象级授权与 IDOR 防护 |
| 后端安全/RBAC 修复 | `spring-boot-architecture` | Controller -> Service role facts 边界 |
| 后端安全/RBAC 修复 | `test-generator` | 权限渗透测试矩阵 |
| 后端安全/RBAC 修复 | `security-review` | Broken Access Control 风险审查 |
| 后端安全/RBAC 修复 | `test-driven-development` | 先 RED 再实现 |

Missing skills: 无。

GitHub research needed: No。该切片复用项目内既有 auth/RBAC 模式，不新增依赖。

New project-specific skill: 暂不创建。

## 3. Subagent Decision

Use Subagents: Yes

Reason: 涉及后端 API、Service 授权、安全、测试矩阵，且用户要求使用专家 subagent 并行开发。

Parallelism Level: L1 Parallel Analysis

Selected Subagents:

- Backend Expert / Spec Architect
- Security & Quality
- Test Engineer
- Integration Reviewer: Main Codex

Implementation Mode: Single Codex implementation after integration review.

## 4. Backend Expert 结论

- 当前 `EvaluationSetController` / `EvaluationRunController` 只传 `currentUserService.currentUserId()`。
- `EvaluationSetService` / `EvaluationRunService` 使用 `"admin"`、`"teacher"`、`teacher_` 前缀推断角色。
- Bearer `ADMIN sub=ops_admin` 会被降级；Bearer `STUDENT/USER sub=admin` 或 `USER sub=teacher_1` 可能被提权。
- 推荐 Controller 从 `UserContext.roles()` 计算 `currentUserAdmin/currentUserTeacher`，再传入 Service。
- 不改 API path、DTO、DB schema、Maven dependency。

## 5. Security & Quality 结论

风险等级：HIGH。

关键缺口：

- Evaluation API 没有使用 `UserContext.roles()`。
- Bearer roles 可被 subject-name 混淆。
- non-admin missing vs foreign object 存在 `404` / `403` oracle。
- Controller 测试缺少 Bearer spoof / role-confusion / teacher foreign scope 覆盖。

建议权限矩阵：

| 场景 | Evaluation Set | Evaluation Run |
|---|---|---|
| Bearer `ADMIN sub=ops_admin` + spoofed `X-User-Id` | allow | allow |
| Bearer `TEACHER sub=instructor_1` own created/course set | allow | allow |
| Bearer `TEACHER` foreign set/course | forbid | forbid |
| Bearer `STUDENT sub=admin` | forbid | forbid |
| Bearer `USER sub=teacher_1` | forbid | forbid |
| non-admin missing vs foreign | same `FORBIDDEN` | same `FORBIDDEN` |
| admin missing | `NOT_FOUND` | `NOT_FOUND` |

## 6. Test Engineer 结论

Controller RED tests 优先：

- Bearer admin + spoofed header 可 create/list/record/compare。
- Bearer teacher 不依赖 `teacher_` subject 前缀仍可访问 own created set。
- Bearer student/user 即使 subject 为 `admin` / `teacher_1` 也不能提权。
- Teacher foreign 与 missing 返回同类安全 `FORBIDDEN` envelope。

Service tests 可在 roles-first overload 落地后同步迁移，以避免 HTTP 主路径继续依赖 legacy method。

## 7. Integration Reviewer 决策

采纳方案：

1. 先新增 Controller 层 RED 测试，证明当前 legacy subject 推断导致 Bearer roles 不生效和 role-confusion。
2. 在 Controller 中读取 `UserContext currentUser = currentUserService.currentUser()`。
3. 仅从 `currentUser.roles()` 计算 `ADMIN/TEACHER` role facts，避免 `CurrentUserService.isAdmin()` 的 dev/test legacy inference 干扰 Bearer 场景。
4. `EvaluationSetService` 和 `EvaluationRunService` 的 HTTP 主路径改为接收 `currentUserId/currentUserAdmin/currentUserTeacher`。
5. 移除或停止使用无 role facts 的 public management API，Service 内授权不再从 userId 字符串推断角色。
6. 非管理员 detail/compare 对 missing 与 foreign evaluation set 返回同类 `FORBIDDEN`，管理员 missing 保留 `NOT_FOUND`。

## 8. Verification Plan

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest test
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,EvaluationSetServiceTest,EvaluationRunServiceTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,PromptVersionControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest test
mvn test
```

