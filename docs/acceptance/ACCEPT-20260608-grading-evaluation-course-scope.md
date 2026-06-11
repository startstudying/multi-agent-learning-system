# ACCEPT - P3-4-G Grading Evaluation Course Scope

## 1. 追溯

- PRD：`docs/product/PRD-20260608-grading-evaluation-course-scope.md`
- REQ：`docs/requirements/REQ-20260608-grading-evaluation-course-scope.md`
- SPEC：`docs/specs/SPEC-20260608-grading-evaluation-course-scope.md`
- PLAN：`docs/plans/PLAN-20260608-grading-evaluation-course-scope.md`
- TASK：`docs/tasks/TASK-20260608-grading-evaluation-course-scope.md`
- Evidence：`docs/evidence/EVIDENCE-20260608-grading-evaluation-course-scope.md`

## 2. 验收清单

### 功能验收

- [x] FR-01：`POST /api/assessment/grading-evaluations` 请求支持 `courseId`。
- [x] FR-02：teacher/admin HTTP 调用缺少 `courseId` 返回 `VALIDATION_ERROR`。
- [x] FR-03：student 即使传合法 course 也返回 `FORBIDDEN` 且无 `data`。
- [x] FR-04：teacher own-course grading evaluation 成功。
- [x] FR-05：teacher foreign course 返回 `FORBIDDEN` 且无 `data`。
- [x] FR-06：teacher missing course 返回 `FORBIDDEN` 且无 `data`。
- [x] FR-07：admin existing course grading evaluation 成功。
- [x] FR-08：admin missing course 返回 `NOT_FOUND`。
- [x] FR-09：`samples[].knowledgePointId` 非空且不属于请求 course 时返回 `VALIDATION_ERROR`。
- [x] FR-10：legacy score array 模式在合法 `courseId` 下保持指标计算。

### 非功能验收

- [x] NFR-01：未新增依赖。
- [x] NFR-02：未新增 DB migration。
- [x] NFR-03：未修改 frontend。
- [x] NFR-04：未改 grading metric 公式或响应指标字段。
- [x] NFR-05：Controller 不直接判断 course 归属，授权位于 Service 层。

### 架构验收

- [x] 前端未直接调用 LLM。
- [x] Agent / RAG / model provider 链路未修改。
- [x] 权限在后端落实，不依赖 Prompt。
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
| RED 验证 | PASS | 新增测试先失败：5 failures，当前接口忽略 course scope。 |
| 聚焦测试 | PASS | `mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest test`；29 tests，0 failures/errors。 |
| 相邻回归 | PASS | `mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest,CourseKnowledgeControllerTest,AnalyticsControllerTest test`；58 tests，0 failures/errors。 |
| 后端全量测试 | PASS | `mvn test`；329 tests，0 failures，0 errors，1 skipped。 |

## 4. 遗留问题

| 问题 | 严重程度 | 后续 TASK |
|---|---|---|
| 正式 JWT/RBAC 未实现，当前仍是 `X-User-Id` 过渡身份。 | High | 后续 P3-4 真实 RBAC/JWT 切片 |
| grading evaluation 未接入 evaluation set runner / scheduled evaluation。 | Medium | 后续 evaluation runner / P2-P3 生产化 |
| broader class/course 权限矩阵仍未完成。 | Medium | 后续 P3-4 broader class/course matrix |

## 5. 验收结论

- [x] 通过
- [ ] 有条件通过
- [ ] 不通过

## 6. 签字

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-08 | Accepted |
