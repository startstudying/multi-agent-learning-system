# EVIDENCE - P3-4-F Assessment Record List RBAC / Pagination

## 1. 追溯

- PRD：`docs/product/PRD-20260608-assessment-record-list-rbac.md`
- REQ：`docs/requirements/REQ-20260608-assessment-record-list-rbac.md`
- SPEC：`docs/specs/SPEC-20260608-assessment-record-list-rbac.md`
- PLAN：`docs/plans/PLAN-20260608-assessment-record-list-rbac.md`
- TASK：`docs/tasks/TASK-20260608-assessment-record-list-rbac.md`
- Context Pack：`docs/context/CONTEXT-20260608-assessment-record-list-rbac.md`
- 日期：2026-06-08

## 2. 实现内容

本切片新增 assessment answer / wrong-question 最小分页列表：

- `GET /api/assessment/answers`
  - student 默认只查询自己的 answer；显式跨 `learnerId` 返回 `FORBIDDEN`。
  - teacher 必须提供 `courseId`，只能查询自己课程 active enrollment learner 的课程相关 answer。
  - admin 可全局查询，也可按 `learnerId` / `courseId` 过滤；missing course 返回 `NOT_FOUND`。
- `GET /api/assessment/wrong-questions`
  - 使用与 answer list 一致的 student / teacher / admin scope。
  - course scope 通过 persisted `knowledgePointId` 过滤。
- 分页参数：
  - `page >= 0`
  - `size` 范围 `1..50`
  - 默认 `page=0,size=20`
- 响应使用 summary DTO，不返回 answer 原文、`requestId`、`requestHash`、`responseJson`、`payloadJson`、`gradingResultId`、`causeAnalysis`、`replanRecordId`。

未新增依赖、未新增 migration、未修改 frontend。

## 3. 变更文件

| 文件 | 操作 | 摘要 |
|---|---|---|
| `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java` | 修改 | 新增 answer list 和 wrong-question list 两个 GET 端点，Controller 只传当前用户与 query 参数。 |
| `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java` | 修改 | 新增 list scope、分页校验、course -> question/knowledge scope 推导和 summary 映射。 |
| `backend/src/main/java/com/learningos/assessment/domain/AnswerRecord.java` | 修改 | 增加 `getCreatedAt()` 供 summary DTO 使用。 |
| `backend/src/main/java/com/learningos/assessment/domain/WrongQuestion.java` | 修改 | 增加 `getCreatedAt()` 供 summary DTO 使用。 |
| `backend/src/main/java/com/learningos/assessment/dto/AssessmentPageResponse.java` | 新增 | 通用 assessment 分页 envelope。 |
| `backend/src/main/java/com/learningos/assessment/dto/AssessmentRecordSummaryResponse.java` | 新增 | answer list 白名单 summary DTO。 |
| `backend/src/main/java/com/learningos/assessment/dto/WrongQuestionSummaryResponse.java` | 新增 | wrong-question list 白名单 summary DTO。 |
| `backend/src/main/java/com/learningos/assessment/repository/AnswerRecordRepository.java` | 修改 | 增加 learner/question scoped paging 查询方法。 |
| `backend/src/main/java/com/learningos/assessment/repository/WrongQuestionRepository.java` | 修改 | 增加 learner/knowledge scoped paging 查询方法。 |
| `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java` | 修改 | 覆盖 student/teacher/admin list 矩阵、分页校验和敏感字段不暴露。 |
| `docs/subagents/runs/RUN-20260608-assessment-record-list-rbac-*.md` | 新增 | 固化 Backend / Security / Integration subagent 分析报告。 |

## 4. 测试结果

### 4.1 RED 验证

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest test
```

结果：

- BUILD FAILURE
- Tests run: 19
- Failures: 6
- Errors: 0
- Skipped: 0
- 失败原因：`GET /api/assessment/answers` / `GET /api/assessment/wrong-questions` list endpoint 尚不存在，返回 `INTERNAL_ERROR`。

### 4.2 聚焦测试

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest test
```

结果：

- BUILD SUCCESS
- Tests run: 19
- Failures: 0
- Errors: 0
- Skipped: 0
- Finished at: 2026-06-08T17:35:03+08:00

### 4.3 相邻回归

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,LearningWorkflowControllerTest test
```

结果：

- BUILD SUCCESS
- Tests run: 59
- Failures: 0
- Errors: 0
- Skipped: 0
- Finished at: 2026-06-08T17:35:56+08:00

### 4.4 后端全量测试

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- BUILD SUCCESS
- Tests run: 322
- Failures: 0
- Errors: 0
- Skipped: 1
- Finished at: 2026-06-08T17:38:05+08:00

## 5. 安全审查

### 5.1 Subagent 安全审查

Security & Quality subagent 结论：

- 风险等级：MEDIUM。
- list API 必须把 scope 写入查询条件，不能 `findAll()` 后过滤。
- list DTO 不应复用 detail DTO。
- teacher 必须强 scope，student foreign learner 需拒绝。

本实现采纳：

- student / teacher / admin scope 在 `AssessmentService` 完成。
- teacher 强制 `courseId`。
- repository 使用 learner/question 或 learner/knowledge scoped paging 查询。
- summary DTO 不返回 answer 原文、幂等快照、payload、gradingResultId、causeAnalysis 或 replanRecordId。

### 5.2 Java 安全审查报告

**被审查文件**：

- `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java`
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/main/java/com/learningos/assessment/dto/AssessmentPageResponse.java`
- `backend/src/main/java/com/learningos/assessment/dto/AssessmentRecordSummaryResponse.java`
- `backend/src/main/java/com/learningos/assessment/dto/WrongQuestionSummaryResponse.java`
- `backend/src/main/java/com/learningos/assessment/repository/AnswerRecordRepository.java`
- `backend/src/main/java/com/learningos/assessment/repository/WrongQuestionRepository.java`

**审查时间**：2026-06-08

#### 高危问题

未发现。

#### 中危问题

未发现。

#### 低危问题

未发现。

#### 无问题项

- SQL 注入：通过。新增查询使用 Spring Data JPA repository 方法，无原生 SQL 拼接。
- 越权访问：通过。列表返回前在 Service 层执行 student owner、teacher own-course + active enrollment、admin 全局矩阵。
- 敏感信息泄露：通过。列表响应使用 DTO 白名单，测试断言不返回 answer 原文、幂等快照和内部 payload。
- 枚举防护：通过。student foreign learner 返回 `FORBIDDEN`；teacher foreign/missing course 返回 `FORBIDDEN`；teacher 未 enrolled learner 返回空 page。
- 硬编码密钥：通过。本切片未新增密钥。
- SSRF、路径遍历、不安全反序列化：不适用。本切片无外部 URL、文件路径或多态反序列化输入。

## 6. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只处理 HTTP/current user，授权和分页 scope 在 `AssessmentService`。 |
| Frontend rules | PASS | 未修改 frontend，未引入前端 LLM/API key。 |
| Agent / RAG rules | PASS | 未修改 Agent/RAG 执行链路。 |
| Security | PASS | 权限在后端代码执行；未新增依赖；未写入秘密。 |
| API / Database | PASS | SPEC 已记录新增列表 API；未修改 schema/migration。 |

## 7. 验证限制

- 当前身份仍基于过渡 `X-User-Id` 字符串模型，不是正式 JWT/RBAC。
- `AnswerRecord` 未持久化 `courseId`，course 过滤通过 `KnowledgePoint` 推导 `questionId`。
- list summary 为最小响应；需要完整 `answer`、`causeAnalysis` 等字段时仍应使用详情接口并通过详情 RBAC。
- `POST /api/assessment/grading-evaluations` 仍只有 teacher/admin gate，尚无 course-scoped grading evaluation。
