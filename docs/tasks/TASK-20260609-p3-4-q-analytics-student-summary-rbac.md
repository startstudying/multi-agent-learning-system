# TASK - P3-4-Q Analytics student summary roles-first RBAC

## 1. 追踪

- PLAN: `docs/plans/PLAN-20260609-p3-4-q-analytics-student-summary-rbac.md`
- SPEC: `docs/specs/SPEC-20260609-p3-4-q-analytics-student-summary-rbac.md`
- 任务编号: TASK-20260609-p3-4-q

## 2. 目标

将 Analytics student summary course scope 的课程读取迁移到 roles-first `CourseAccessService` overload，补齐 Bearer spoof、teacher no-prefix、subject-name role-confusion 和 missing/foreign anti-enumeration 回归测试。

## 3. 允许修改的文件

- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `docs/product/PRD-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/requirements/REQ-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/specs/SPEC-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/plans/PLAN-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/tasks/TASK-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/context/CONTEXT-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/retrospectives/RETRO-20260609-p3-4-q-analytics-student-summary-rbac.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 4. 禁止修改的文件

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- Assessment / GradingEvaluation / LearningPath / ResourceGeneration 代码。
- Formal OAuth2/JWK/Spring Security 配置。

## 5. 实施步骤

1. 补齐本切片 PRD/REQ/SPEC/PLAN/TASK/CONTEXT。
2. 新增 RED 测试：
   - Bearer admin spoofed header 仍可读取 course-scoped student summary。
   - Bearer teacher no-prefix 可读取 own-course active learner summary。
   - Bearer `USER sub=teacher_1` 不能通过 subject 前缀读取未 enrollment course-scoped self summary。
   - Bearer teacher 对 missing/foreign course 都返回 safe `FORBIDDEN`。
3. 运行 focused RED：`mvn --% -Dtest=AnalyticsControllerTest test`。
4. 修改 `AnalyticsService.requireCourseReadForStudentSummary(...)` 调用 role-aware overload。
5. 运行 focused / adjacent / full verification。
6. 创建 Evidence / Acceptance / Retro，更新 Changelog / Memory / TODO / PLAN / TASK。

## 6. 测试命令

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AnalyticsControllerTest test
mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
mvn test
```

## 7. 完成标准

- [x] PRD/REQ/SPEC/PLAN/TASK/CONTEXT 已存在。
- [x] RED 测试已先失败，失败原因命中 legacy role facts 丢失。
- [x] `AnalyticsService` 使用 role-aware `CourseAccessService.requireCourseRead(...)`。
- [x] focused/adjacent/full backend tests 已运行或限制已说明。
- [x] 无 API/DB/frontend/dependency drift。
- [x] Evidence 文档已创建。
- [x] Acceptance 报告已创建。
- [x] Changelog / Memory / TODO 已更新。

## 8. 状态

| 字段 | 值 |
|---|---|
| 状态 | 已完成 |
| 负责人 | Codex |
| 开始日期 | 2026-06-09 |
| 完成日期 | 2026-06-09 |
