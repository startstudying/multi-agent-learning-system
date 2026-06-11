# EVIDENCE-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion

## 1. 追踪

- TASK：`docs/tasks/TASK-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion.md`
- 父计划：`docs/planning/backend-architecture-todolist.md` / P3-4 权限与安全加固
- 日期：2026-06-10
- 类型：S Fast Lane，combined Evidence / Acceptance

## 2. 实现内容

本切片补齐 answer record / wrong-question RBAC 渗透测试矩阵，不修改生产代码。

新增覆盖点：

- `answerDetailAllowsBearerStudentRoleAndIgnoresSpoofedUserIdHeader`
- `answerDetailRejectsBearerUserSubjectTeacherPrefixRoleConfusion`
- `teacherDetailRejectsInactiveEnrollmentEvenForOwnCourseAssessmentRecords`
- `wrongQuestionDetailUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`
- `wrongQuestionDetailAllowsBearerStudentRoleAndIgnoresSpoofedUserIdHeader`
- `wrongQuestionDetailRejectsBearerUserSubjectAdminRoleConfusion`
- `answerListUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`
- `answerListAllowsBearerStudentRoleAndIgnoresSpoofedUserIdHeader`
- `wrongQuestionListAllowsBearerTeacherWithoutSubjectPrefixForOwnCourse`
- `wrongQuestionListRejectsBearerUserSubjectAdminRoleConfusion`
- `wrongQuestionListRejectsBearerUserSubjectTeacherPrefixRoleConfusion`

新增测试固定以下安全行为：

- Bearer `ADMIN` 使用 token role，忽略 spoofed `X-User-Id`。
- Bearer `TEACHER` 无 `teacher_` subject 前缀时仍可访问自己课程内 active learner 的错题列表。
- Bearer `USER sub=admin` / `USER sub=teacher_*` 不获得 admin / teacher 权限。
- Bearer 普通学生即使夹带 `X-User-Id=admin`，也只能读自己的 answer/wrong-question list/detail。
- 教师 detail 路径必须同时满足 own course 与 active enrollment；learner enrollment 被改为 `DROPPED` 后拒绝。
- 越权响应不返回 `data`，并通过响应体断言避免泄露 answer/wrong-question id。

## 3. 变更文件

| 文件 | 操作 | 摘要 |
|---|---|---|
| `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java` | 修改 | 新增 11 个 MockMvc RBAC 渗透测试和 `wrongQuestionIdForAnswer(...)` 测试 helper。 |
| `docs/tasks/TASK-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion.md` | 新增/更新 | S mini TASK，内嵌 Context Pack，记录完成状态。 |
| `docs/subagents/runs/RUN-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion-security.md` | 新增 | Security Reviewer 只读分析归档。 |
| `docs/subagents/runs/RUN-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion-test.md` | 新增 | Test Engineer 只读分析归档。 |
| `docs/evidence/EVIDENCE-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion.md` | 新增 | 本 combined Evidence / Acceptance。 |
| `docs/changelog/CHANGELOG.md` | 更新 | 记录本切片变更和验证结果。 |
| `docs/memory/PROJECT_MEMORY.md` | 更新 | 记录 P3-4 子任务完成状态。 |
| `docs/memory/BACKEND_MEMORY.md` | 更新 | 记录后端测试矩阵扩展和验证结果。 |
| `docs/memory/AGENT_RAG_MEMORY.md` | 更新 | 更新权限矩阵开放项状态。 |
| `docs/planning/backend-architecture-todolist.md` | 更新 | 标注 answer record 矩阵已补齐，P3-4 父项仍保持开放。 |

## 4. 测试结果

### 4.1 Focused

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest test
```

结果：

- `Tests run: 48, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### 4.2 Adjacent

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest,AssessmentServiceTest,CourseAccessServiceTest test
```

结果：

- `Tests run: 65, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### 4.3 Full backend

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- `Tests run: 547, Failures: 0, Errors: 0, Skipped: 1`
- `BUILD SUCCESS`

## 5. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 只补 Controller 层 MockMvc 测试；未改 Controller / Service / Repository 分层。 |
| Frontend rules | PASS | 未改前端，无 LLM/API key 风险。 |
| Agent / RAG rules | PASS | 未改 Agent/RAG 运行时。 |
| Security | PASS | 权限仍由后端代码验证；未新增 secret / dependency。 |
| API / Database | PASS | 未改 API contract、DTO 或 schema。 |

## 6. Acceptance

| 验收项 | 结果 | 证据 |
|---|---|---|
| Mini TASK 存在并包含目标、范围、允许文件、禁止文件、测试命令、验收标准 | 通过 | `docs/tasks/TASK-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion.md` |
| 专家 subagent 并行分析已使用并归档 | 通过 | `docs/subagents/runs/RUN-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion-security.md`、`docs/subagents/runs/RUN-20260610-p3-4-answer-record-rbac-penetration-matrix-expansion-test.md` |
| Answer list/detail Bearer admin/student/spoofed header 和 subject-name role-confusion 矩阵补齐 | 通过 | `AssessmentControllerTest` 新增测试 |
| Wrong-question list/detail Bearer admin/teacher/student/spoofed header 和 subject-name role-confusion 矩阵补齐 | 通过 | `AssessmentControllerTest` 新增测试 |
| Teacher detail active enrollment 约束补齐 | 通过 | `teacherDetailRejectsInactiveEnrollmentEvenForOwnCourseAssessmentRecords` |
| Focused / adjacent / full backend 验证通过 | 通过 | 本文第 4 节 |
| Changelog / Memory / TODO 更新 | 通过 | 相关文档已更新 |

## 7. 限制与风险

- 新增测试首次 focused 运行即 GREEN，未观察到生产代码 RED；本切片是安全回归矩阵补强，不是漏洞修复。
- 未新增 service-level 行为测试；专家判断本轮 MockMvc 更适合覆盖 Bearer、header spoofing 与 controller-to-service role-fact 链路。
- `AnswerRecord` 仍通过 `questionId -> knowledgePointId -> courseId` 推导课程 scope；长期可考虑 schema 标准化增加直接 course scope，但不属于本切片。
- P3-4 父项仍未完成：broader course/class/resource 权限矩阵、dev/test legacy fallback cleanup、frontend production SSE client / sensitive URL cleanup 仍需后续切片。

## 8. 验收结论

通过。
