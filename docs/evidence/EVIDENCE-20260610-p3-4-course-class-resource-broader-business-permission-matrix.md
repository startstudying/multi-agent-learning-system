# EVIDENCE-20260610-p3-4-course-class-resource-broader-business-permission-matrix

## 1. 任务

P3-4 子任务：course-class-resource broader business permission matrix。

## 2. 结论

验收结论：PASS。

本切片通过测试-only 方式补齐了 `course/class/resource` 更宽业务权限矩阵的 5 个高价值回归点。未发现需要升级为 M 的生产代码缺陷。

## 3. 改动摘要

新增测试：

- `CourseKnowledgeControllerTest.courseListBearerTeacherIgnoresSpoofedAdminHeaderAndReturnsOnlyOwnedCourses`
- `CourseKnowledgeControllerTest.courseListBearerStudentWithSpoofedTeacherHeaderReturnsOnlyActiveEnrollments`
- `ResourceGenerationControllerTest.courseBoundResourceGenerationCreateRejectsBearerOwnerWithDroppedEnrollmentWithoutSideEffects`
- `ResourceGenerationControllerTest.courseBoundResourceGenerationCreateRejectsBearerOwnerWithNoEnrollmentWithoutSideEffects`
- `AnalyticsControllerTest.bearerTeacherStudentSummaryRejectsDroppedLearnerInOwnCourseWithoutLeakingScope`

覆盖行为：

- Bearer `TEACHER` 列课忽略伪造 `X-User-Id: admin`，只返回 token subject 拥有课程。
- Bearer `STUDENT` 列课忽略伪造 `X-User-Id: teacher_*`，只返回 ACTIVE enrollment 课程，不返回 DROPPED enrollment 课程。
- Bearer owner 在 DROPPED 或 no enrollment 状态下不能创建 course-bound resource generation task，且无 ResourceGenerationTask / LearningResource / Review / AgentTask / AgentTrace / ModelCall / TokenUsage / SourceCitation 副作用。
- Bearer `TEACHER` 读取自己课程下 DROPPED learner student summary 时返回安全 `FORBIDDEN`，不泄漏 courseId、knowledgePointId、learningPathId、wrong-cause 文本或 resource task id。

## 4. Subagent Evidence

使用专家 subagent 并行：

- Course/Knowledge Expert：`docs/subagents/runs/RUN-20260610-p3-4-course-class-resource-broader-business-permission-matrix-course.md`
- Resource Generation Security Expert：`docs/subagents/runs/RUN-20260610-p3-4-course-class-resource-broader-business-permission-matrix-resource.md`
- Analytics Authorization Expert：`docs/subagents/runs/RUN-20260610-p3-4-course-class-resource-broader-business-permission-matrix-analytics.md`

说明：Course 与 Analytics 专家线程复用时被旧只读角色约束限制，产出只读分析；主 Codex 按其报告集成测试改动。Resource 专家直接完成资源测试改动并运行单类验证。

## 5. Verification

Focused：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest,ResourceGenerationControllerTest,AnalyticsControllerTest,ResourceReviewControllerTest test
```

结果：

```text
Tests run: 117, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Adjacent：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest,CourseAccessServiceTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest test
```

结果：

```text
Tests run: 143, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Full backend：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 566, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

## 6. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 本切片仅新增测试，未改 Controller / Service / Repository。 |
| Frontend rules | PASS | 未改前端。 |
| Agent / RAG rules | PASS | 未改 Agent/RAG runtime；资源生成副作用通过现有仓储断言验证。 |
| Security | PASS | 未新增 secret / 依赖；新增测试继续固定 backend code 权限边界。 |
| API / Database | PASS | 未改 API contract 或 schema。 |

## 7. Acceptance

- [x] S mini TASK 已存在并包含 Context Pack。
- [x] 新增测试覆盖 5 个待补矩阵点。
- [x] 未修改生产代码。
- [x] focused / adjacent / full backend 测试均通过。
- [x] combined Evidence / Acceptance 已创建。
- [x] Changelog / Memory / TODO 已更新。
- [x] P3-4 父项未错误标记为完成。

## 8. Remaining Risks / Follow-up

- P3-4 父项仍 open：dev/test legacy fallback cleanup、frontend production streaming client / sensitive SSE URL cleanup、broader forged-id / business object penetration matrix 仍需后续子任务继续。
- P3-2 工业级 PDF/DOCX layout/table/TOC/reading-order、native/cloud OCR、OCR confidence 和真实渲染页码仍 open。
