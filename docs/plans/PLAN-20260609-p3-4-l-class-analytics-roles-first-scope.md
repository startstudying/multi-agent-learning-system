# PLAN-20260609 P3-4-L class analytics roles-first course scope

## 1. Skill Selection Report

### Task Type

Security hardening / backend authorization / class-course permission matrix.

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 按项目 Spec-first 闭环推进。 |
| `security-review` | 本切片修改授权行为，需要覆盖 IDOR、spoofing、anti-enumeration。 |
| `object-scope-authorization` | 定义 course owner、teacher/admin/student 对象级授权。 |
| `auth-context-boundary` | Bearer roles 与 `X-User-Id` 兼容边界。 |
| `test-driven-development` | 必须先写 RED，再最小 GREEN。 |
| `verification-before-completion` | 完成前必须有 fresh verification evidence。 |

### Missing Skills

无。

### GitHub Research Needed

No。本切片沿用项目既有授权规则，不引入外部框架或依赖。

### New Project-Specific Skill To Create

暂不创建。若后续多次复用 class analytics 授权模式，可扩展 `object-scope-authorization`。

## 2. Subagent Decision

Use Subagents: Yes

Reason: 用户要求使用专家 subagent；任务涉及 security、backend API、test matrix。

Parallelism Level: L1 Parallel Analysis / Design

Selected Subagents:

- Security & Quality：分析 class summary roles-first 与 anti-enumeration 风险。
- Backend Expert：定位 Controller / Service / CourseAccessService 影响面。
- Test Engineer：设计最小 RED 测试矩阵。

Implementation Mode: Single Codex implementation。原因：生产改动集中在 analytics controller/service，同文件并行写入收益低且冲突风险高。

## 3. Implementation Steps

1. [x] 读取 memory、skills、architecture baseline、TODO。
2. [x] 子代理并行分析 security/backend/test。
3. [x] 创建 PRD / REQ / SPEC / PLAN / TASK / CONTEXT。
4. [x] RED：新增/调整 `AnalyticsControllerTest` class summary roles-first 测试。
5. [x] GREEN：最小修改 `AnalyticsController` / `AnalyticsService`。
6. [x] Focused verification。
7. [x] Adjacent regression。
8. [x] Full backend Maven verification。
9. [x] Evidence / Acceptance / Retrospective。
10. [x] 更新 Changelog / Memory / backend TODO。

## 3.1 Verification Summary

```text
Focused:  29 run, 0 failures, 0 errors, 0 skipped
Adjacent: 56 run, 0 failures, 0 errors, 0 skipped
Full:     396 run, 0 failures, 0 errors, 1 skipped
```

## 3.2 Final Architecture Drift Result

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只传 current user / role flags；对象授权在 service 层。 |
| Frontend rules | PASS | 未修改 `frontend/**`。 |
| Agent / RAG rules | PASS | 未修改 Agent/RAG runtime。 |
| Security | PASS | 权限判断在后端代码完成；Bearer role + spoofed header class summary 已覆盖。 |
| API / Database | PASS | 未新增 API path、DTO、schema、migration 或依赖。 |

## 4. Allowed Files

- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `docs/product/PRD-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/requirements/REQ-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/specs/SPEC-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/plans/PLAN-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/tasks/TASK-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/context/CONTEXT-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/retrospectives/RETRO-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/subagents/runs/RUN-20260609-p3-4-l-class-analytics-roles-first-scope.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 5. Forbidden Files

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- RAG parser/OCR/vector/model provider files。

## 6. Test Commands

```powershell
cd backend
mvn --% -Dtest=AnalyticsControllerTest test
mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

## 7. Risks

| Risk | Mitigation |
|---|---|
| 误把 role `TEACHER` 当作全课程访问权限 | 仍要求 `currentUserId == Course.teacherId`。 |
| 破坏 legacy test/dev header 行为 | Controller 仍通过 `CurrentUserService.isTeacherUser()` 保留 dev/test legacy inference。 |
| 影响完整 `CourseAccessService` | 本切片不迁移全局服务，仅修 class summary 路径。 |
| P3-4 被误标完成 | 文档明确 broader class/course 与 formal OAuth2/JWK 仍 open。 |
