# EVIDENCE - 后端架构 TODO 完成计划

本证据文档随分阶段切片持续追加，每个 Task 一节。

## Task 1：P3-4-A Course / Knowledge Catalog 权限收口

### 1. 变更概述

将 `POST /api/courses`、`POST /api/courses/{courseId}/chapters`、`POST /api/knowledge-points`、`POST /api/knowledge-dependencies` 四个写接口的权限收口到 Service 层过渡 RBAC：

- `admin`：可创建/维护任意课程图谱，可为指定 `teacherId` 创建课程。
- `teacher`：只能创建归属自己的课程，只能维护自己课程的章节、知识点、依赖。
- `student` / 其他用户：写操作返回 `FORBIDDEN`，不创建任何业务数据。

未新增依赖、未修改 API 合同、未修改数据库 schema。

### 2. 关键实现证据

- `CurrentUserService` 提供 `isAdmin()` / `isTeacherUser()` 角色辅助（基于过渡 `X-User-Id` 字符串身份）。
- `CourseController` / `KnowledgePointController` 只读取 `currentUserService.currentUserId()` 并传入 Service，权限判断不在 Controller。
- `KnowledgeCatalogService.resolveCourseTeacherId(...)`：student 创建课程抛 `FORBIDDEN`；teacher 指定其他 `teacherId` 抛 `FORBIDDEN`；teacher 省略 `teacherId` 时以当前用户为 owner；admin 可指定任意 `teacherId`。
- `KnowledgeCatalogService.requireCourseTeacherOrAdmin(...)`：章节、知识点、依赖写操作要求当前用户是课程 `teacherId` 或 admin，否则 `FORBIDDEN`。
- 依赖创建保留既有 self-dependency、cross-course、dependencyType 校验，并在校验通过后追加课程归属检查。
- `CourseKnowledgeControllerTest` 覆盖：teacher 正常全链路创建、unsupported dependencyType 400、student 创建课程 403、teacher 越权指定 teacherId 403、teacher 省略 teacherId 时后端赋值、foreign teacher 维护课程图谱 403、admin 维护任意课程图谱。

### 3. 测试命令

#### 3.1 聚焦测试

```powershell
cd backend
mvn --% -Dtest=CourseKnowledgeControllerTest test
```

结果：

- BUILD SUCCESS（exit 0）
- 结构化请求日志显示：student `POST /api/courses` -> `status=403 errorCode=FORBIDDEN`；teacher 越权 chapter/point/dependency -> `status=403 errorCode=FORBIDDEN`；admin 维护外部课程 -> `status=200`。

#### 3.2 相邻模块回归

```powershell
cd backend
mvn --% -Dtest=CourseKnowledgeControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,AnalyticsControllerTest test
```

结果：

- BUILD SUCCESS（exit 0）
- 学习路径相关用例使用 `teacher_path` 身份创建课程图谱，未被新权限规则误伤；学生越权访问 profile/learning-path 仍返回 403。

### 4. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 仅委托 Service，权限判断在 `KnowledgeCatalogService` |
| Frontend rules | PASS | 未修改 frontend |
| Agent / RAG rules | PASS | 未改变 Agent/RAG 执行链路 |
| Security | PASS | 权限在后端代码执行，不依赖 Prompt；未新增依赖；未写入秘密 |
| API / Database | PASS | 未修改 API 请求/响应 path 与合同；未新增迁移 |

### 5. 验证限制

- 当前身份仍基于过渡 `X-User-Id` 字符串用户，不是完整 JWT/RBAC；正式 RBAC 留待后续切片。
- 本切片只收口 Course / Knowledge Catalog 写路径；对象详情防枚举已在 Task 2（P3-4-B）补齐，完整 JWT/RBAC、class/course 与答题记录矩阵仍待后续 P3-4 扩展。

## Task 2：P3-4-B 对象详情防枚举与 scoped authorization

### 1. 变更概述

将以下对象详情/操作接口的对象级授权下沉到 Service 层，并对非 admin 统一收敛 missing object 与 foreign object 响应：

- `GET /api/learning-paths/{pathId}`
- `GET /api/resources/generation-tasks/{taskId}`
- `GET /api/agent/tasks/{taskId}/trace`
- `GET /api/documents/{documentId}`
- `POST /api/documents/{documentId}/reindex`
- `GET /api/index-tasks/{taskId}`

非 admin 普通用户访问不存在对象或无权对象时均返回 `FORBIDDEN`，响应无 `data`，且响应体不包含被探测的对象 id。`admin` 保留运维审计所需的真实 `NOT_FOUND` 语义。未新增依赖、未修改 API path、未修改数据库 schema。

### 2. 关键实现证据

- `LearningPathController` 仅传入 `currentUserId`；`LearningWorkflowService.getPathForUser(...)` 对非 admin 将 missing path 转换为 `FORBIDDEN`，并拒绝访问非本人 `learnerId` 的学习路径。
- `ResourceGenerationService.getTask(...)` / `getLearnerResources(...)` / `getTrace(...)` 统一通过 `loadGenerationTaskForDetail(...)`、`ensureTaskOwnerOrAdmin(...)`、`ensureTaskOwner(...)`、`ensureTraceOwner(...)` 执行资源任务与 trace owner 检查；`scopedMissing(...)` 对非 admin 返回 `FORBIDDEN`。
- `AgentTraceGovernanceService.getTrace(...)` 对 trace detail 先加载 `AgentTask`，再校验 `ownerUserId`；missing trace task 对非 admin 返回 `FORBIDDEN`。
- `DocumentService.getDocument(...)` / `reindex(...)` / `getIndexTask(...)` 先按 detail 安全加载 document/index task，再通过 `ensureCanRead(...)` / `ensureCanWrite(...)` 校验 KB 读写权限；missing document/index task 对非 admin 返回 `FORBIDDEN`。
- 新增测试覆盖：
  - `LearningWorkflowControllerTest.learningPathGetDoesNotRevealMissingVersusForeignPathToNonAdmin`
  - `ResourceGenerationControllerTest.taskAndTraceDetailsDoNotRevealMissingVersusForeignObjectsToNonAdmin`
  - `DocumentControllerTest.documentAndIndexTaskDetailsDoNotRevealMissingVersusForeignObjectsToNonAdmin`

### 3. 测试命令

#### 3.1 聚焦测试

```powershell
cd backend
mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,DocumentControllerTest test
```

结果：

- BUILD SUCCESS（exit 0）
- Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
- 结构化请求日志包含非 admin `bob` 对 learning path、resource generation task、agent trace、document、reindex、index task 的 `status=403 errorCode=FORBIDDEN` 记录。

#### 3.2 相邻模块回归

```powershell
cd backend
mvn --% -Dtest=LearningWorkflowControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,DocumentControllerTest,ChatControllerTest,RagQueryServiceTest,CourseKnowledgeControllerTest test
```

结果：

- BUILD SUCCESS（exit 0）
- Tests run: 66, Failures: 0, Errors: 0, Skipped: 0
- 覆盖学习路径、资源生成、Review Gate、RAG 文档、Chat/RAG 查询、Course/Knowledge Catalog 权限相邻链路。

#### 3.3 完整后端测试

```powershell
cd backend
mvn test
```

结果：

- 第二次完整运行：BUILD SUCCESS（exit 0）
- Tests run: 264, Failures: 0, Errors: 0, Skipped: 1
- Total time: 01:56 min

说明：

- 第一次完整 `mvn test` 使用 122 秒工具超时，未产生 Maven BUILD 结论；检查后未发现本项目残留测试进程。
- 随后使用更长超时重新执行同一命令并取得上述 BUILD SUCCESS。

### 4. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 仅读取当前用户并委托 Service；对象归属与 missing/foreign 收敛在 Service 层 |
| Frontend rules | PASS | 未修改 frontend |
| Agent / RAG rules | PASS | 未改变 Agent tool 边界；Agent Trace 详情仍通过治理 Service 查询 |
| Security | PASS | 权限在后端代码执行，不依赖 Prompt；响应不暴露被探测对象 id；未新增依赖；未写入秘密 |
| API / Database | PASS | 未修改 API path/合同；未新增迁移 |

### 5. 验证限制

- 当前身份仍是过渡 `X-User-Id` 字符串模型，不是完整 JWT/RBAC。
- 本切片覆盖对象详情防枚举与已有对象详情接口授权；答题记录目前没有独立按 `answerId` 查询接口，未新增 API。
- P3-4 仍未全部完成：真实 RBAC/JWT、class/course 授权矩阵、教师端全量数据范围、学生端答题记录矩阵、完整渗透测试仍是后续工作。

## Task 3：P3-5-A 运维告警 API

### 1. 变更概述

新增管理员查询型运维告警接口：

```text
GET /api/analytics/ops/alerts
```

本切片聚合四类风险信号：

- `SLOW_RAG_QUERY`：RAG 查询延迟达到慢查询阈值。
- `SLOW_MODEL_CALL`：模型调用延迟达到慢模型阈值。
- `RAG_NO_SOURCE`：RAG 无来源数量与比例同时达到阈值。
- `REVIEW_BACKLOG`：待审核/待修订 review 的积压数量和年龄达到阈值。

本切片只提供 query-time API，不做外部推送，不新增依赖，不修改数据库 schema，不修改前端。

### 2. 关键实现证据

- `AnalyticsController.opsAlerts(...)` 暴露 `/api/analytics/ops/alerts`，并通过 `currentUserService.isAdmin()` 做临时 admin-only 检查；非 admin 和缺失 `X-User-Id` 均返回 `FORBIDDEN`。
- `AnalyticsService.opsAlerts(...)` 提供默认 24 小时窗口，并校验 `from < to`、正数阈值和 `0..1` 比例阈值；非法参数返回 `VALIDATION_ERROR`。
- 默认阈值：
  - `slowQueryMs=1000`
  - `slowModelMs=2000`
  - `noSourceRateThreshold=0.2`
  - `noSourceMinCount=3`
  - `reviewBacklogHours=24`
  - `reviewBacklogCount=10`
- 数据来源保持在已有表/实体：
  - `KbQueryLog`：RAG 延迟与 `retrievalCount <= 0` 无来源口径。
  - `ModelCallLog`：模型延迟口径。
  - `ResourceReview`：`PENDING_CRITIC` / `REVISION_REQUESTED` 审核积压口径。
- 响应只使用白名单 DTO：`OpsAlertSummary`、`OpsAlertThresholds`、`OpsAlertItem`；没有直接序列化 entity，也没有复用包含 `errorMessage` 的 `AbnormalModelCall`。
- `REVIEW_BACKLOG` 不按 `from` 排除老 review；只要求 review `createdAt <= windowEnd` 且年龄达到阈值，避免真实积压因为早于查询窗口开始而被漏报。
- `AnalyticsControllerTest` 覆盖 admin-only、默认阈值、四类告警触发、敏感字段脱敏、非法参数。

### 3. TDD 与测试命令

#### 3.1 TDD RED 记录

开发阶段先在 `AnalyticsControllerTest` 增加告警 API 测试，再运行聚焦测试；在 Controller / Service 尚未实现时，`/api/analytics/ops/alerts` 路由不存在，新增的 4 个用例失败，符合 RED 预期。随后实现 Controller / Service 并进入 GREEN。

#### 3.2 聚焦测试

```powershell
cd backend
mvn --% -Dtest=AnalyticsControllerTest test
```

结果：

- BUILD SUCCESS（exit 0）
- Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
- Finished at: 2026-06-08T00:33:37+08:00

关键覆盖：

- `opsAlertsRequiresAdminAccess`
- `opsAlertsReturnsDefaultThresholdsWhenNoSignalsExist`
- `opsAlertsReturnsFourSanitizedTriggeredAlerts`
- `opsAlertsRejectsInvalidParameters`

#### 3.3 相邻模块回归

```powershell
cd backend
mvn --% -Dtest=AnalyticsControllerTest,StructuredRequestLoggingFilterTest,HealthServiceTest,HealthControllerTest,RagQueryServiceTest,ResourceReviewControllerTest test
```

结果：

- BUILD SUCCESS（exit 0）
- Tests run: 57, Failures: 0, Errors: 0, Skipped: 0
- Finished at: 2026-06-08T00:34:49+08:00

覆盖 analytics、结构化请求日志、深度健康检查、RAG 查询、Review Gate 相邻链路。

#### 3.4 完整后端测试

```powershell
cd backend
mvn test
```

结果：

- BUILD SUCCESS（exit 0）
- Tests run: 268, Failures: 0, Errors: 0, Skipped: 1
- Total time: 01:52 min
- Finished at: 2026-06-08T00:36:53+08:00

### 4. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只处理 HTTP 参数和 admin-only 入口检查；聚合逻辑在 `AnalyticsService` |
| Frontend rules | PASS | 未修改 frontend；没有前端直接调用 LLM/API key 变更 |
| Agent / RAG rules | PASS | 未改变 Agent Tool、RAG 检索或模型调用链路；只读取既有日志/审核数据 |
| Security | PASS | 权限在后端代码执行；响应使用白名单 DTO；不暴露 prompt、question、raw response、errorMessage、markdownContent、review 私有字段或 seed 敏感内容；未新增依赖；未写入秘密 |
| API / Database | PASS | 新增 documented API；未修改数据库 schema；未新增迁移 |

### 5. 验证限制

- 当前 admin-only 仍基于过渡 `X-User-Id: admin`，不是生产级 JWT/RBAC。
- 本切片只提供查询型告警聚合；不包含外部告警推送、通知渠道、告警持久化或 dashboard。
- `RAG_NO_SOURCE` 暂以 `retrievalCount <= 0` 作为无来源口径；更细粒度的 no-source schema 可后续增强。
- 阈值通过 query 参数传入或使用默认值；未提供持久化阈值配置。

## Task 6：P3-2-A Embedding Service / VectorDB Adapter 边界（noop）

### 1. 变更概述

在不新增 Maven 依赖、不改 DB schema、不改公开 RAG API 的前提下，补齐 embedding/vector 扩展边界：

- `EmbeddingService` 提供批量 `embedDocumentChunks(...)` contract 与 `isEnabled()` 判断（默认 `provider=none` 时为 DISABLED）。
- `VectorIndexAdapter` + `NoopVectorIndexAdapter` 提供可选 vector upsert/search 边界（默认 disabled）。
- `IndexService` 索引链路调整为 `CHUNKING -> save chunks -> EMBEDDING -> VECTOR_UPSERT -> SUCCEEDED`，chunk metadata 写入 `embeddingStatus` / `vectorIndexStatus`。
- `ChunkService` 在 adapter/embedding 均 enabled 时预留 vector RRF 分支；noop 默认行为保持 `vectorEnabled=false`。

### 2. 测试命令

```powershell
cd backend
mvn --% -Dtest=EmbeddingServiceTest,NoopVectorIndexAdapterTest,IndexServiceTest,IndexServiceParserFailureTest,IndexTaskWorkerSchedulerTest,RagQueryServiceTest,RrfRankerTest test
mvn test
```

结果：

- 聚焦回归：BUILD SUCCESS（exit 0）
- 全量后端：BUILD SUCCESS（exit 0）

### 3. 验证限制

- 未接入真实 Spring AI embedding provider 或 VectorDB SDK。
- `provider!=none` 且配置了 `embeddingModel` 时，当前边界会返回 `EMBEDDING_PROVIDER_NOT_CONFIGURED` 并使索引失败；真实 provider 接入在 Task 5/后续切片处理。
