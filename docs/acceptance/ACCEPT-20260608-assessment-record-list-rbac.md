# ACCEPT - P3-4-F Assessment Record List RBAC / Pagination

## 1. 追溯

- PRD：`docs/product/PRD-20260608-assessment-record-list-rbac.md`
- REQ：`docs/requirements/REQ-20260608-assessment-record-list-rbac.md`
- SPEC：`docs/specs/SPEC-20260608-assessment-record-list-rbac.md`
- PLAN：`docs/plans/PLAN-20260608-assessment-record-list-rbac.md`
- TASK：`docs/tasks/TASK-20260608-assessment-record-list-rbac.md`
- Evidence：`docs/evidence/EVIDENCE-20260608-assessment-record-list-rbac.md`

## 2. 验收清单

### 功能验收

- [x] FR-01：`GET /api/assessment/answers` 已新增。
- [x] FR-02：`GET /api/assessment/wrong-questions` 已新增。
- [x] FR-03：student 未传 `learnerId` 时只返回自己的记录。
- [x] FR-04：student 显式查询其他 `learnerId` 返回 `FORBIDDEN` 且无 `data`。
- [x] FR-05：teacher list 必须提供 `courseId`。
- [x] FR-06：teacher 只能读取 own-course active enrollment learner 的课程相关记录。
- [x] FR-07：teacher 查询自己课程下未 enrolled learner 返回空 page。
- [x] FR-08：teacher foreign/missing course 返回 `FORBIDDEN` 且无 `data`。
- [x] FR-09：admin 可全局分页查询，并可按 learner/course 过滤。
- [x] FR-10：admin missing course 返回 `NOT_FOUND`。
- [x] FR-11：`page < 0` 或 `size < 1 || size > 50` 返回 `VALIDATION_ERROR`。

### 非功能验收

- [x] NFR-01：未新增依赖。
- [x] NFR-02：未新增 DB migration。
- [x] NFR-03：未修改 frontend。
- [x] NFR-04：列表响应不包含 answer 原文、`requestId`、`requestHash`、`responseJson`、`payloadJson`。
- [x] NFR-05：列表响应不包含 `gradingResultId`、`causeAnalysis`、`replanRecordId`。
- [x] NFR-06：Controller 不直接判断对象归属，授权位于 Service 层。

### 架构验收

- [x] 前端未直接调用 LLM。
- [x] Agent / RAG 链路未修改。
- [x] 权限在后端落实。
- [x] 未提交密钥。
- [x] 无数据库 schema 漂移。

### 文档验收

- [x] Evidence 已创建。
- [x] Acceptance 已创建。
- [x] PLAN / TASK 已更新。
- [x] Memory 已更新。
- [x] Changelog 已更新。
- [x] Retrospective 已创建。
- [x] Project-specific skill 已更新。

## 3. 测试摘要

| 测试项 | 结果 | 备注 |
|---|---|---|
| RED 验证 | PASS | 新增测试先失败：6 failures，缺少 list endpoints。 |
| 聚焦测试 | PASS | `mvn --% -Dtest=AssessmentControllerTest test`；19 tests，0 failures/errors。 |
| 相邻回归 | PASS | `mvn --% -Dtest=AssessmentControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,LearningWorkflowControllerTest test`；59 tests，0 failures/errors。 |
| 后端全量测试 | PASS | `mvn test`；322 tests，0 failures，0 errors，1 skipped。 |

## 4. 遗留问题

| 问题 | 严重程度 | 后续 TASK |
|---|---|---|
| 正式 JWT/RBAC 未实现，当前仍是 `X-User-Id` 过渡身份。 | High | 后续 P3-4 真实 RBAC/JWT 切片 |
| `GradingEvaluationRequest` 无 `courseId`，暂不能做 course teacher scope。 | Medium | 后续 evaluation set/course scope 切片 |
| assessment 记录未冗余 `courseId`，answer list course 过滤依赖 `courseId -> knowledgePoint -> questionId` 推导。 | Medium | 后续 schema 归一化或仓储级 join 优化切片 |
| P3-4 仍不是完整权限终局。 | Medium | broader course/class matrix 与真实 RBAC/JWT |

## 5. 验收结论

- [x] 通过
- [ ] 有条件通过
- [ ] 不通过

## 6. 签字

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-08 | Accepted |
