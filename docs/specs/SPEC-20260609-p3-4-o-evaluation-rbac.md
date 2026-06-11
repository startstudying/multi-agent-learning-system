# SPEC-20260609 P3-4-O Evaluation Set / Run roles-first RBAC

## 1. 概述

本规格覆盖 Evaluation Set / Run HTTP 管理主路径的 roles-first RBAC 收口。核心变化是：Controller 不再只传 `currentUserId`，而是从 `UserContext.roles()` 显式计算 `currentUserAdmin/currentUserTeacher`，Service 使用这些 role facts 完成管理面和对象级授权。

## 2. 追踪

- PRD: `docs/product/PRD-20260609-p3-4-o-evaluation-rbac.md`
- REQ: `docs/requirements/REQ-20260609-p3-4-o-evaluation-rbac.md`
- Related specs:
  - `docs/specs/SPEC-20260608-real-auth-rbac-context.md`
  - `docs/specs/SPEC-20260609-p3-4-n-prompt-version-rbac.md`
  - `docs/specs/SPEC-20260606-evaluation-set-management.md`
  - `docs/specs/SPEC-20260606-prompt-version-quality-comparison.md`

## 3. 领域模型

不新增领域模型。

沿用：

- `EvaluationSet`
- `EvaluationSample`
- `EvaluationRun`
- `EvaluationRunMetric`
- `UserContext`

## 4. API 契约

API path、method、request DTO、response DTO 不变。

### Evaluation Set

```http
POST /api/evaluation-sets
GET /api/evaluation-sets?type=RAG_QUESTION
GET /api/evaluation-sets/{setId}
```

授权：

| Role facts | create | list | detail |
|---|---:|---:|---:|
| `ADMIN` | allow | all visible sets | any existing set |
| `TEACHER` | own created or own-course set | own created/own-course sets | own created/own-course set |
| `STUDENT` / `USER` | deny | deny | deny |

### Evaluation Run

```http
POST /api/evaluation-runs
GET /api/evaluation-runs/comparison
```

授权：

| Role facts | record | comparison |
|---|---:|---:|
| `ADMIN` | allow for any readable set | allow for any existing set |
| `TEACHER` | allow for own created/own-course set | allow for own created/own-course set |
| `STUDENT` / `USER` | deny | deny |

## 5. 前端交互

不修改前端。

## 6. 后端流程

目标 Controller 流程：

```text
HTTP request
-> DevAuthFilter sets UserContext
-> Evaluation*Controller.currentUserService.currentUser()
-> hasRole(currentUser, "ADMIN")
-> hasRole(currentUser, "TEACHER")
-> Evaluation*Service.method(currentUser.userId(), admin, teacher, ...)
-> Service authorization
-> repository read/write
```

禁止：

- Controller 使用 `CurrentUserService.isAdmin()` / `isTeacherUser()` 来计算 HTTP Bearer 场景 role facts。
- Service 使用 `"admin"`、`"teacher"`、`teacher_` 前缀推断 HTTP 管理权限。

## 7. Agent 工作流

不涉及。

## 8. RAG 工作流

不修改 RAG runtime。Evaluation set type 可为 `RAG_QUESTION`，但本切片仅收口管理 API 权限。

## 9. 数据库变更

无。

## 10. 状态流转

无新增状态。

沿用：

- Evaluation set: `DRAFT` / `ACTIVE` / `ARCHIVED`
- Evaluation run: `SUCCEEDED` / `FAILED`

## 11. 错误处理

| 错误码 | 说明 | 触发条件 |
|---|---|---|
| `FORBIDDEN` | 权限不足 | 非 admin/teacher 访问管理面；teacher 访问 foreign/missing set |
| `NOT_FOUND` | 对象不存在 | Admin 访问 missing evaluation set |
| `VALIDATION_ERROR` | 请求参数无效 | type/status/metric/promptCode 等业务校验失败 |

非管理员 missing 与 foreign evaluation set 必须同类返回 `FORBIDDEN`，响应不包含 `data`。

## 12. 权限规则

Bearer 规则：

- Bearer token 存在时，`X-User-Id` 不参与授权。
- Bearer `ADMIN sub=ops_admin` 允许管理，不要求 `sub=admin`。
- Bearer `STUDENT sub=admin` 不获得 admin。
- Bearer `USER sub=teacher_1` 不获得 teacher。
- Bearer `TEACHER sub=instructor_1` 可访问 own created set，不要求 `sub` 有 `teacher_` 前缀。

Teacher scope：

- `currentUserTeacher == true && currentUserId == set.createdBy` 可读/写该 set/run。
- `currentUserTeacher == true && set.courseId` 绑定的课程 `teacherId == currentUserId` 可读/写该 set/run。
- 其他情况拒绝。

Admin scope：

- `currentUserAdmin == true` 全局允许。
- Admin missing set 返回 `NOT_FOUND`。

## 13. Trace / 日志

不新增日志字段。错误响应走现有 `GlobalExceptionHandler`。

不得在错误消息中暴露 sample content、prompt text、raw output、foreign object id。

## 14. 测试策略

RED:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest test
```

GREEN focused:

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest test
```

Adjacent:

```powershell
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,EvaluationSetServiceTest,EvaluationRunServiceTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn --% -Dtest=EvaluationSetControllerTest,EvaluationRunControllerTest,PromptVersionControllerTest,CourseKnowledgeControllerTest,AnalyticsControllerTest test
```

Full:

```powershell
mvn test
```

## 15. 验收清单

- [ ] Bearer admin + spoofed `X-User-Id` 可 create/list/record/compare。
- [ ] Bearer teacher 不依赖 `teacher_` subject 前缀可访问 own set。
- [ ] Bearer `STUDENT sub=admin` 被拒绝。
- [ ] Bearer `USER sub=teacher_1` 被拒绝。
- [ ] Teacher foreign/missing set 同为 `FORBIDDEN`。
- [ ] Admin missing set 为 `NOT_FOUND`。
- [ ] response 不泄露 `promptText`、`answerText`、`inputJson`、`rawOutput`。
- [ ] 不修改 API/DB/dependency/frontend。

