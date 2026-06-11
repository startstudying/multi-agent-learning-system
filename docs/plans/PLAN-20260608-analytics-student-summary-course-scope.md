# PLAN-20260608 学生分析摘要课程范围权限收口

## 1. Skill Selection Report

### Task Type

Bug fix / security hardening / backend API authorization.

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 按项目 Spec-first 流程推进 P3 计划切片。 |
| `object-scope-authorization` | 定义 student/teacher/admin 对象级和课程级访问矩阵。 |
| `auth-context-boundary` | 使用当前认证上下文和 roles-first helper，不回退到不可信前端逻辑。 |
| `spring-boot-architecture` | 保持 Controller -> Service 分层。 |
| `test-driven-development` | 权限行为变更必须先 RED。 |
| `verification-before-completion` | 只有 fresh verification 后才能声明完成。 |

### Missing Skills

无。

### GitHub Research Needed

No。本切片使用项目已有 `CourseAccessService` 与 analytics 服务边界，不新增外部依赖。

### New Project-Specific Skill To Create

暂不需要；复用 `object-scope-authorization`。

## 2. Subagent Decision

Use Subagents: Yes

Reason: 总目标要求专家 subagent 并行开发；P3 剩余项已并行派发给 RAG parser、model provider、security matrix 专家。

Parallelism Level: L1 Parallel Analysis

Selected Subagents:

- P3-2 RAG Parser/OCR Expert
- P3-3 Model Provider Expert
- P3-4 Security Matrix Expert

Implementation Mode: Single Codex implementation for this narrow slice, because `AnalyticsControllerTest` / `AnalyticsService` 文件会集中修改，不适合并行写同一文件。

Note: P3-4 Security Matrix subagent 未在本切片开始前返回；主线依据已有 `object-scope-authorization` skill、历史 P3-4 docs 和当前代码继续推进，并在集成审查记录该风险。

## 3. Implementation Steps

1. [x] RED：在 `AnalyticsControllerTest` 添加 teacher/admin/student/course scoped summary 权限测试。
2. [x] GREEN：调整 `AnalyticsController.studentSummary(...)`，接受 `courseId` 并委托 service。
3. [x] GREEN：在 `AnalyticsService` 下沉 student summary 授权与 course scoped 聚合。
4. [x] REFACTOR：抽取最小 helper，避免 controller 写业务规则。
5. [x] 验证 focused、adjacent、full backend tests。
6. [x] 更新 evidence、acceptance、changelog、memory、planning TODO。

## 4. Allowed Files

- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `docs/product/PRD-20260608-analytics-student-summary-course-scope.md`
- `docs/requirements/REQ-20260608-analytics-student-summary-course-scope.md`
- `docs/specs/SPEC-20260608-analytics-student-summary-course-scope.md`
- `docs/plans/PLAN-20260608-analytics-student-summary-course-scope.md`
- `docs/tasks/TASK-20260608-analytics-student-summary-course-scope.md`
- `docs/context/CONTEXT-20260608-analytics-student-summary-course-scope.md`
- `docs/evidence/EVIDENCE-20260608-analytics-student-summary-course-scope.md`
- `docs/acceptance/ACCEPT-20260608-analytics-student-summary-course-scope.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 5. Forbidden Files

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- unrelated RAG parser / model provider / auth filter / assessment / resource implementation files

## 6. Test Commands

```powershell
cd backend
mvn --% -Dtest=AnalyticsControllerTest test
mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,AssessmentControllerTest test
mvn test
```

## 7. Risks

- `AnalyticsService` 当前大量使用 `findAll().stream()` 过滤；本切片保持行为兼容，生产级 repository scoped query 后续优化。
- teacher individual summary 对未报名 learner 返回 `FORBIDDEN`，与 list 接口的 empty page 语义不同；本接口是 detail-like summary，选择安全拒绝。
- roles-first admin/teacher 只在 controller 层通过 `CurrentUserService` 判断，本切片不迁移全部 `CourseAccessService` 到 role-aware 参数。

## 8. Completion Record

- 完成日期：2026-06-08。
- Focused verification：`mvn --% -Dtest=AnalyticsControllerTest test`，22 tests，0 failures，0 errors。
- Adjacent verification：`mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,AssessmentControllerTest test`，60 tests，0 failures，0 errors。
- Full backend verification：`mvn test`，350 tests，0 failures，0 errors，1 skipped。
- Evidence：`docs/evidence/EVIDENCE-20260608-analytics-student-summary-course-scope.md`。
- Acceptance：`docs/acceptance/ACCEPT-20260608-analytics-student-summary-course-scope.md`。
- Retrospective：`docs/retrospectives/RETRO-20260608-analytics-student-summary-course-scope.md`。
- Skill extraction：复用 `object-scope-authorization`，暂不新增 skill。
