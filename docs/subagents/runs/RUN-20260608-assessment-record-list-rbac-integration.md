# RUN-20260608 Assessment Record List RBAC / Pagination Integration Review

## 1. Conclusion

P3-4-F 应作为 P3-4-E 的列表扩展切片，补齐 `answer record list/pagination RBAC`。本切片只做 assessment answer / wrong-question 列表读接口，不推进 JWT/RBAC、前端页面、grading evaluation course scope、schema 大改或完整 class/course 权限矩阵。

GitHub research：不需要。现有 `object-scope-authorization`、`CourseAccessService`、P3-4-D enrollment scope、P3-4-E detail RBAC 足够支撑。

## 2. Task Boundary

纳入：

- `GET /api/assessment/answers`
- `GET /api/assessment/wrong-questions`
- `page` / `size` 分页，采用当前 SPEC 的 `page=0,size=20,size<=50`
- `learnerId` 和 `courseId` 过滤
- list 专用白名单 DTO
- student owner-only
- teacher own-course + active enrollment learner
- admin global

不纳入：

- JWT / RBAC 框架
- 前端页面
- answer detail / wrong-question detail
- 答题提交、评分、wrong-cause、mastery 更新链路
- LLM、RAG、Agent、model gateway 逻辑
- 新依赖或 migration

## 3. Implementation Strategy

建议单线程实现。

原因：

- 主要落点在 assessment API / service / repository / tests。
- 权限安全复用 P3-4-E detail 模式。
- 并行改代码容易在 `AssessmentControllerTest`、`AssessmentService` 上产生文件重叠。

Subagent 适合 L1 parallel analysis，不建议并行实现。

## 4. Document Requirements

继续使用并更新：

- `docs/product/PRD-20260608-assessment-record-list-rbac.md`
- `docs/requirements/REQ-20260608-assessment-record-list-rbac.md`
- `docs/specs/SPEC-20260608-assessment-record-list-rbac.md`
- `docs/plans/PLAN-20260608-assessment-record-list-rbac.md`
- `docs/tasks/TASK-20260608-assessment-record-list-rbac.md`
- `docs/context/CONTEXT-20260608-assessment-record-list-rbac.md`

完成后新增：

- `docs/evidence/EVIDENCE-20260608-assessment-record-list-rbac.md`
- `docs/acceptance/ACCEPT-20260608-assessment-record-list-rbac.md`
- `docs/retrospectives/RETRO-20260608-assessment-record-list-rbac.md`

并更新 memory / changelog / TODO / skill。

## 5. Acceptance Commands

```powershell
cd backend
mvn --% -Dtest=AssessmentControllerTest test
mvn --% -Dtest=AssessmentControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,LearningWorkflowControllerTest test
mvn test
```

## 6. Integration Judgment

P3-4-F 完成后不能把 P3-4 全部标为完成。TODO 仍需保留真实 JWT/RBAC、broader course/class matrix、grading evaluation course scope 等后续项。
