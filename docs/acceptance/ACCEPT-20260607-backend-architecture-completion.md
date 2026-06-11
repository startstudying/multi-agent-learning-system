# ACCEPT - 后端架构 TODO 完成计划

本验收文档随分阶段切片持续追加，每个 Task 一节。

## Task 1：P3-4-A Course / Knowledge Catalog 权限收口

### 1. 验收结论

通过。

四个课程/知识图谱写接口的写权限已收口到 Service 层过渡 RBAC：student 无法创建任何课程图谱数据，teacher 只能维护自己课程，admin 保持全局能力。

### 2. 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| student 创建 course 返回 403 | PASS | `CourseKnowledgeControllerTest.rejectsStudentCreatingCourse` |
| teacher 创建 course 保存 `teacherId=currentUserId` | PASS | `CourseKnowledgeControllerTest.teacherCourseCreationUsesCurrentTeacherIdWhenTeacherIdIsOmitted` |
| teacher 不可为其他 teacher 创建 course | PASS | `CourseKnowledgeControllerTest.teacherCannotCreateCourseForAnotherTeacher` |
| foreign teacher 不可新增 chapter | PASS | `CourseKnowledgeControllerTest.foreignTeacherCannotManageCourseGraph` |
| foreign teacher 不可新增 knowledge point | PASS | `CourseKnowledgeControllerTest.foreignTeacherCannotManageCourseGraph` |
| foreign teacher 不可新增 dependency | PASS | `CourseKnowledgeControllerTest.foreignTeacherCannotManageCourseGraph` |
| admin 可管理任意 course graph | PASS | `CourseKnowledgeControllerTest.adminCanManageAnyCourseGraph` |
| 既有全链路创建与 dependencyType 校验不回退 | PASS | `createsCourseChapterKnowledgePointsAndGraph`, `rejectsUnsupportedKnowledgeDependencyType` |
| 学习路径相关回归通过 | PASS | `LearningWorkflowControllerTest`（聚焦回归 BUILD SUCCESS） |
| 无 API / DB / dependency 变更 | PASS | Context Pack 与 diff 检查；无迁移、无 pom 变更 |

### 3. 测试验收

| 命令 | 结果 |
|---|---|
| `mvn --% -Dtest=CourseKnowledgeControllerTest test` | BUILD SUCCESS（exit 0） |
| `mvn --% -Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,AnalyticsControllerTest test` | BUILD SUCCESS（exit 0） |

### 4. Open Items

- 用正式 RBAC/JWT 替代临时 `X-User-Id` 字符串身份。
- Task 2（P3-4-B）已完成对象详情防枚举；答题记录独立详情矩阵仍待后续 P3-4 扩展。
- Task 3（P3-5-A）已完成：慢查询/慢模型/无来源 RAG/审核积压查询型告警。
- Task 4-8：模型网关结构化校验与日志、真实 Spring AI provider、Embedding/VectorDB、Hybrid retrieval、复杂 PDF/OCR（部分需依赖评审与外部资源）。

## Task 2：P3-4-B 对象详情防枚举与 scoped authorization

### 1. 验收结论

通过。

学习路径、资源生成任务、Agent Trace、RAG 文档、文档 reindex、索引任务详情接口已完成对象详情防枚举收口。非 admin 用户对 missing object 与 foreign object 均得到同类 `FORBIDDEN` 响应，响应无 `data`，且不包含被探测对象 id。

### 2. 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| foreign path 与 missing path 对非 admin 均返回 `FORBIDDEN` 且无 `data` | PASS | `LearningWorkflowControllerTest.learningPathGetDoesNotRevealMissingVersusForeignPathToNonAdmin` |
| foreign resource generation task 与 missing task 对非 admin 均返回 `FORBIDDEN` 且无 `data` | PASS | `ResourceGenerationControllerTest.taskAndTraceDetailsDoNotRevealMissingVersusForeignObjectsToNonAdmin` |
| foreign agent trace 与 missing trace task 对非 admin 均返回 `FORBIDDEN` 且无 `data` | PASS | `ResourceGenerationControllerTest.taskAndTraceDetailsDoNotRevealMissingVersusForeignObjectsToNonAdmin` |
| foreign RAG document 与 missing document 对非 admin 均返回 `FORBIDDEN` 且无 `data` | PASS | `DocumentControllerTest.documentAndIndexTaskDetailsDoNotRevealMissingVersusForeignObjectsToNonAdmin` |
| foreign reindex 与 missing reindex 对非 admin 均返回 `FORBIDDEN` 且无 `data` | PASS | `DocumentControllerTest.documentAndIndexTaskDetailsDoNotRevealMissingVersusForeignObjectsToNonAdmin` |
| foreign index task 与 missing index task 对非 admin 均返回 `FORBIDDEN` 且无 `data` | PASS | `DocumentControllerTest.documentAndIndexTaskDetailsDoNotRevealMissingVersusForeignObjectsToNonAdmin` |
| 越权响应不包含被探测对象 id | PASS | 新增测试断言 response body `doesNotContain(...)` 目标 id |
| 聚焦测试通过 | PASS | `mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,DocumentControllerTest test`，34 tests，0 failures/errors |
| 相邻权限/RAG/资源生成回归通过 | PASS | `mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,DocumentControllerTest,ChatControllerTest,RagQueryServiceTest,CourseKnowledgeControllerTest test`，66 tests，0 failures/errors |
| 完整后端测试通过 | PASS | `mvn test`，264 tests，0 failures/errors，1 skipped |
| 无 API / DB / dependency 变更 | PASS | 未修改 API path、迁移脚本或 build dependency 文件 |

### 3. 测试验收

| 命令 | 结果 |
|---|---|
| `mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,DocumentControllerTest test` | BUILD SUCCESS；Tests run: 34, Failures: 0, Errors: 0, Skipped: 0 |
| `mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,DocumentControllerTest,ChatControllerTest,RagQueryServiceTest,CourseKnowledgeControllerTest test` | BUILD SUCCESS；Tests run: 66, Failures: 0, Errors: 0, Skipped: 0 |
| `mvn test` | BUILD SUCCESS；Tests run: 264, Failures: 0, Errors: 0, Skipped: 1 |

### 4. Open Items

- 用正式 RBAC/JWT 替代临时 `X-User-Id` 字符串身份。
- P3-4 仍需扩展 class/course 授权矩阵、教师端全量数据范围、学生端答题记录访问矩阵和完整 teacher/student/admin RBAC 渗透测试。
- 答题记录目前没有独立按 `answerId` 查询接口；本切片未新增答题详情 API。
- Task 3（P3-5-A）已完成：慢查询/慢模型/无来源 RAG/审核积压查询型告警。
- Task 4-8：模型网关结构化校验与日志、真实 Spring AI provider、Embedding/VectorDB、Hybrid retrieval、复杂 PDF/OCR（部分需依赖评审与外部资源）。

## Task 3：P3-5-A 运维告警 API

### 1. 验收结论

通过。

`GET /api/analytics/ops/alerts` 已作为 admin-only 查询型运维告警 API 实现，可返回默认阈值和四类已触发告警：`SLOW_RAG_QUERY`、`SLOW_MODEL_CALL`、`RAG_NO_SOURCE`、`REVIEW_BACKLOG`。响应使用白名单 DTO，不直接返回 entity，不暴露 prompt/question/raw response/errorMessage/markdownContent/review 私有字段。

### 2. 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| `GET /api/analytics/ops/alerts` 返回统一 `ApiResponse` | PASS | `AnalyticsControllerTest.opsAlertsReturnsDefaultThresholdsWhenNoSignalsExist` |
| 非 admin 返回 `FORBIDDEN` | PASS | `AnalyticsControllerTest.opsAlertsRequiresAdminAccess` |
| 缺失 `X-User-Id` 返回 `FORBIDDEN` | PASS | `AnalyticsControllerTest.opsAlertsRequiresAdminAccess` |
| 无信号时返回默认 thresholds 和空 `alerts` | PASS | `AnalyticsControllerTest.opsAlertsReturnsDefaultThresholdsWhenNoSignalsExist` |
| 慢 RAG 查询 `latencyMs >= slowQueryMs` 触发 `SLOW_RAG_QUERY` | PASS | `AnalyticsControllerTest.opsAlertsReturnsFourSanitizedTriggeredAlerts` |
| 慢模型调用 `latencyMs >= slowModelMs` 触发 `SLOW_MODEL_CALL` | PASS | `AnalyticsControllerTest.opsAlertsReturnsFourSanitizedTriggeredAlerts` |
| RAG 无来源数量与比例同时达标触发 `RAG_NO_SOURCE` | PASS | `AnalyticsControllerTest.opsAlertsReturnsFourSanitizedTriggeredAlerts` |
| 审核积压数量和年龄达标触发 `REVIEW_BACKLOG` | PASS | `AnalyticsControllerTest.opsAlertsReturnsFourSanitizedTriggeredAlerts` |
| 响应不包含敏感字段名或敏感 seed 内容 | PASS | `AnalyticsControllerTest.opsAlertsReturnsFourSanitizedTriggeredAlerts` 的 response body 脱敏断言 |
| 非法时间窗口、非正阈值返回 `VALIDATION_ERROR` | PASS | `AnalyticsControllerTest.opsAlertsRejectsInvalidParameters` |
| 不新增依赖、不改 DB schema、不改前端 | PASS | 未修改 `pom.xml`、migration、frontend；完整后端测试通过 |
| Evidence / Acceptance / Memory / Changelog 更新 | PASS | 本验收文档、Evidence、TASK、TODO、Changelog、Memory 已更新 |

### 3. 测试验收

| 命令 | 结果 |
|---|---|
| `mvn --% -Dtest=AnalyticsControllerTest test` | BUILD SUCCESS；Tests run: 16, Failures: 0, Errors: 0, Skipped: 0 |
| `mvn --% -Dtest=AnalyticsControllerTest,StructuredRequestLoggingFilterTest,HealthServiceTest,HealthControllerTest,RagQueryServiceTest,ResourceReviewControllerTest test` | BUILD SUCCESS；Tests run: 57, Failures: 0, Errors: 0, Skipped: 0 |
| `mvn test` | BUILD SUCCESS；Tests run: 268, Failures: 0, Errors: 0, Skipped: 1 |

### 4. Open Items

- 用正式 RBAC/JWT 替代临时 `X-User-Id: admin` 判断。
- 告警仍是 query-time 聚合；外部推送、通知渠道、告警持久化和 dashboard 仍是后续生产化任务。
- `RAG_NO_SOURCE` 当前使用 `retrievalCount <= 0`；更细 no-source 结构化字段可后续增强。
- Task 4-8：模型网关结构化校验与日志、真实 Spring AI provider、Embedding/VectorDB、Hybrid retrieval、复杂 PDF/OCR（部分需依赖评审与外部资源）。
