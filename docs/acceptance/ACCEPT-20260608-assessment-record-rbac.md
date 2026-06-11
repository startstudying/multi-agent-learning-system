# ACCEPT - P3-4-E Assessment Record RBAC Matrix

## 1. 追溯

- PRD：`docs/product/PRD-20260608-assessment-record-rbac.md`
- REQ：`docs/requirements/REQ-20260608-assessment-record-rbac.md`
- SPEC：`docs/specs/SPEC-20260608-assessment-record-rbac.md`
- PLAN：`docs/plans/PLAN-20260608-assessment-record-rbac.md`
- TASK：`docs/tasks/TASK-20260608-assessment-record-rbac.md`
- Evidence：`docs/evidence/EVIDENCE-20260608-assessment-record-rbac.md`

## 2. 验收清单

### 功能验收

- [x] FR-01：`GET /api/assessment/answers/{answerId}` 已新增。
- [x] FR-02：`GET /api/assessment/wrong-questions/{wrongQuestionId}` 已新增。
- [x] FR-03：student 只能读取自己的 answer / wrong question。
- [x] FR-04：teacher 只能读取自己课程 active enrollment learner 的课程相关记录。
- [x] FR-05：admin 可读取任意已存在记录，missing 返回 `NOT_FOUND`。
- [x] FR-06：非 admin missing/foreign 返回同形 `FORBIDDEN` 且无 `data`。
- [x] FR-07：wrong-question detail 复用 assessment record 授权语义。
- [x] FR-08：Controller 不直接判断对象归属，授权位于 Service 层。

### 非功能验收

- [x] NFR-01：未新增依赖。
- [x] NFR-02：未新增 DB migration。
- [x] NFR-03：未修改 frontend。
- [x] NFR-04：响应不包含 `requestId`、`requestHash`、`responseJson`、`payloadJson`。
- [x] NFR-05：非 admin missing/foreign 不形成 `404/403` 枚举差异。

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

## 3. 测试摘要

| 测试项 | 结果 | 备注 |
|---|---|---|
| 聚焦测试 | PASS | `mvn --% -Dtest=AssessmentControllerTest test`；13 tests，0 failures/errors。 |
| 相邻回归 | PASS | `mvn --% -Dtest=AssessmentControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,LearningWorkflowControllerTest test`；53 tests，0 failures/errors。 |
| 后端全量测试 | PASS | `mvn test`；316 tests，0 failures，0 errors，1 skipped。 |

## 4. 遗留问题

| 问题 | 严重程度 | 后续 TASK |
|---|---|---|
| 正式 JWT/RBAC 未实现，当前仍是 `X-User-Id` 过渡身份。 | High | 后续 P3-4 真实 RBAC/JWT 切片 |
| answer / wrong-question list 与分页未实现。 | Medium | 后续 assessment list RBAC 切片 |
| `GradingEvaluationRequest` 无 `courseId`，暂不能做 course teacher scope。 | Medium | 后续 evaluation set/course scope 切片 |
| assessment 记录未冗余 `courseId`，teacher 授权当前依赖 `questionId -> knowledgePointId -> courseId` 推导。 | Medium | 后续 schema 归一化或仓储级 scoped query 切片 |

## 5. 验收结论

- [x] 通过
- [ ] 有条件通过
- [ ] 不通过

## 6. 签字

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-08 | Accepted |
