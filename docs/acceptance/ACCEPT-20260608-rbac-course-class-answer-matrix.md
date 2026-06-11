# ACCEPT - P3-4-C 权限矩阵安全前置

## 1. 追溯

- PRD：`docs/product/PRD-20260608-rbac-course-class-answer-matrix.md`
- REQ：`docs/requirements/REQ-20260608-rbac-course-class-answer-matrix.md`
- SPEC：`docs/specs/SPEC-20260608-rbac-course-class-answer-matrix.md`
- PLAN：`docs/plans/PLAN-20260608-rbac-course-class-answer-matrix.md`
- TASK：`docs/tasks/TASK-20260608-rbac-course-class-answer-matrix.md`
- 证据：`docs/evidence/EVIDENCE-20260608-rbac-course-class-answer-matrix.md`

## 2. 验收清单

### 功能验收

- [x] FR-01：`GET /api/courses` 对 `admin` 返回全部课程。
- [x] FR-02：`GET /api/courses` 对 teacher 只返回自己课程。
- [x] FR-03：`GET /api/courses` 对普通 student 暂返回空列表，不返回 foreign course。
- [x] FR-04：`GET /api/courses/{courseId}` 对 authorized teacher/admin 返回课程详情。
- [x] FR-05：`GET /api/courses/{courseId}` 对 foreign teacher/student 与 missing course 的非 admin 均返回 `FORBIDDEN` 且无 `data`。
- [x] FR-06：`GET /api/courses/{courseId}/knowledge-graph` 复用课程读取授权。
- [x] FR-07：`POST /api/assessment/grading-evaluations` 允许 teacher/admin，拒绝普通 student。
- [x] FR-08：Controller 不直接判断课程归属，权限判断位于 Service 层。

### 非功能验收

- [x] NFR-01：未新增依赖。
- [x] NFR-02：未修改数据库 schema。
- [x] NFR-03：未修改前端。
- [x] NFR-04：非 admin missing/foreign course 不形成 `404/403` 枚举差异。

### 架构验收

- [x] 前端未直接调用 LLM。
- [x] Agent / RAG 链路未修改。
- [x] 权限在后端落实。
- [x] 未提交密钥。
- [x] 无 API path / schema 漂移。

### 文档验收

- [x] Evidence 已创建。
- [x] Acceptance 已创建。
- [x] PLAN / TASK 已更新。
- [x] Memory 已更新。
- [x] Changelog 已更新。

## 3. 测试摘要

| 测试项 | 结果 | 备注 |
|---|---|---|
| 聚焦测试 | PASS | `mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest test`；19 tests，0 failures/errors。 |
| 相邻回归 | PASS | `mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,DocumentControllerTest test`；71 tests，0 failures/errors。 |
| 后端全量测试 | PASS | `mvn test`；302 tests，0 failures，0 errors，1 skipped。 |

## 4. 遗留问题

| 问题 | 严重程度 | 后续 TASK |
|---|---|---|
| 正式 JWT/RBAC 未实现，当前仍是 `X-User-Id` 过渡身份。 | High | 后续 P3-4 真实 RBAC/JWT 切片 |
| student enrolled course 读取需要 class/enrollment schema 支撑。 | Medium | 后续 class/course 权限矩阵切片 |
| `GradingEvaluationRequest` 无 `courseId`，暂不能做 course teacher scope。 | Medium | 后续 evaluation set/course scope 切片 |
| answer record 独立详情/list 矩阵未实现。 | Medium | 后续 answer record 权限矩阵切片 |

## 5. 验收结论

- [x] 通过
- [ ] 有条件通过
- [ ] 不通过

## 6. 签字

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-08 | Accepted |
