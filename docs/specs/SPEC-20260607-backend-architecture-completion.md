# SPEC-20260607 后端架构 TODO 完成计划

## Task 3 规格：P3-5-A 运维告警 API

### API

```text
GET /api/analytics/ops/alerts
```

权限：临时 admin-only。`X-User-Id` 缺失或不等于 `admin` 时返回 `FORBIDDEN`。

### Query 参数

| 参数 | 默认值 | 校验 |
|---|---:|---|
| `from` | `to - 24h` | 必须早于 `to` |
| `to` | `now` | 必须晚于 `from` |
| `slowQueryMs` | `1000` | `> 0` |
| `slowModelMs` | `2000` | `> 0` |
| `noSourceRateThreshold` | `0.2` | `0 <= value <= 1` |
| `noSourceMinCount` | `3` | `>= 1` |
| `reviewBacklogHours` | `24` | `>= 1` |
| `reviewBacklogCount` | `10` | `>= 1` |

### 响应 DTO

```java
OpsAlertSummary(
    Instant windowStart,
    Instant windowEnd,
    OpsAlertThresholds thresholds,
    List<OpsAlertItem> alerts
)
```

`alerts` 只返回已触发告警。无告警时返回空数组。

### 告警口径

| Type | 数据来源 | 触发条件 |
|---|---|---|
| `SLOW_RAG_QUERY` | `KbQueryLog` | 窗口内 `latencyMs >= slowQueryMs` |
| `SLOW_MODEL_CALL` | `ModelCallLog` | 窗口内 `latencyMs >= slowModelMs` |
| `RAG_NO_SOURCE` | `KbQueryLog` | `retrievalCount <= 0` 的数量和比例同时达标 |
| `REVIEW_BACKLOG` | `ResourceReview` | `PENDING_CRITIC` / `REVISION_REQUESTED` 中超过年龄阈值的数量达标 |

### 脱敏规则

响应禁止返回以下字段名或内容：

- `prompt`
- `question`
- `responseJson`
- `sourcesJson`
- `errorMessage`
- `markdownContent`
- `citationCheck`
- `revisionSuggestion`
- raw provider error
- secret / token / connection string / API key

### 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 处理 HTTP 和 current user；Service 聚合业务口径 |
| Frontend rules | PASS | 不改前端 |
| Agent / RAG rules | PASS | 不改 Agent/RAG 执行链路，只读取已有治理日志 |
| Security | PASS | admin-only，白名单 DTO，不返回敏感字段 |
| API / Database | PASS | 新增后端 analytics endpoint；不改 schema |

## 1. 总体设计

以 `docs/planning/backend-architecture-todolist.md` 为权威 TODO，按 P3 剩余项拆分切片：

1. 权限与安全
2. 可观测性告警
3. 模型接入边界
4. RAG 索引生产化

每个切片遵循：

```text
测试先行 -> 最小实现 -> 聚焦测试 -> 架构漂移检查 -> Evidence -> Acceptance -> Memory/Changelog
```

## 2. 第一轮切片：Course / Knowledge Catalog 权限收口

### 2.1 当前问题

当前 `CourseController` 与 `KnowledgePointController` 未读取当前用户：

- `POST /api/courses`
- `POST /api/courses/{courseId}/chapters`
- `POST /api/knowledge-points`
- `POST /api/knowledge-dependencies`

上述写接口直接进入 `KnowledgeCatalogService`，导致任意用户可以创建课程、章节、知识点和依赖。

### 2.2 权限模型

本切片采用过渡 RBAC：

- `admin`：当前用户 id 等于 `admin`。
- `teacher`：当前用户是目标 course 的 `teacherId`，或创建课程时当前用户显式作为 teacher。
- `student`：除 admin / teacher 之外的普通用户。

完整 JWT/RBAC 不在本切片实现。

### 2.3 Service API

新增或调整：

```java
Course createCourse(String currentUserId, CreateCourseRequest request)
Chapter createChapter(String currentUserId, String courseId, CreateChapterRequest request)
KnowledgePoint createKnowledgePoint(String currentUserId, CreateKnowledgePointRequest request)
KnowledgeDependency createDependency(String currentUserId, CreateKnowledgeDependencyRequest request)
```

读方法保持不变：

```java
Course getCourse(String courseId)
List<Course> listCourses()
KnowledgeGraphResponse getKnowledgeGraph(String courseId)
```

### 2.4 Controller 规则

Controller 只负责：

- 读取 current user id。
- 传入 Service。
- 返回 `ApiResponse`。

权限判断必须在 Service 层执行。

### 2.5 错误语义

| 场景 | ErrorCode | HTTP |
|---|---|---|
| student 创建 course/chapter/point/dependency | `FORBIDDEN` | 403 |
| teacher 创建 course 但指定其他 teacherId | `FORBIDDEN` | 403 |
| teacher 维护 foreign course graph | `FORBIDDEN` | 403 |
| missing course/chapter/point | `NOT_FOUND` | 404 |
| invalid dependency type / cross-course dependency | `VALIDATION_ERROR` | 400 |

### 2.6 测试要求

聚焦测试类：

- `CourseKnowledgeControllerTest`

新增用例：

- student cannot create course
- teacher course creation uses current teacher id
- teacher cannot create course for another teacher
- foreign teacher cannot add chapter
- foreign teacher cannot create knowledge point
- foreign teacher cannot create dependency
- admin can manage foreign course graph

## 3. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 权限与业务规则在 Service。 |
| Frontend rules | N/A | 不改前端。 |
| Agent / RAG rules | N/A | 不改 Agent/RAG 执行。 |
| Security | PASS | 后端代码权限检查，不依赖 Prompt。 |
| API / Database | PASS | 不改 path，不改 schema。 |

## 4. 第二轮切片：对象详情防枚举与 scoped authorization

Status: Done。实现与验证已记录在 Evidence / Acceptance；后续 P3-4 仍保留真实 RBAC/JWT、class/course 矩阵与答题记录权限矩阵。

### 4.1 当前问题

P3-4-A 已收口课程/知识图谱写权限；P3-4-B 已收口以下对象详情接口，避免普通用户通过 `404 NOT_FOUND` 与 `403 FORBIDDEN` 区分“对象不存在”和“对象存在但无权访问”：

- `GET /api/learning-paths/{pathId}`
- `GET /api/resources/generation-tasks/{taskId}`
- `GET /api/agent/tasks/{taskId}/trace`
- `GET /api/documents/{documentId}`
- `GET /api/index-tasks/{taskId}`
- `POST /api/documents/{documentId}/reindex`

答题记录当前没有独立按 `answerId` 查询接口；`POST /api/assessment/answers` 已按 `learnerId` 校验请求所有权，本切片不新增答题详情 API。

### 4.2 权限模型

本切片沿用过渡 RBAC：

- `admin`：运维/审计角色，可保留真实 `NOT_FOUND` 语义，并可访问需要运维审计的任务/trace 详情。
- 对象 owner：只能访问自己的学习路径、资源生成任务、Agent Trace；RAG 文档/索引任务按 KB read/write 权限判断。
- 非 owner 普通用户：foreign id 与 missing id 返回同类安全错误 `FORBIDDEN`，响应不包含对象详情。

### 4.3 Service 规则

新增或调整：

```java
LearningPathResponse getPathForUser(String currentUserId, String pathId)
ResourceGenerationResponse getTask(String userId, String taskId)
LearnerResourceListResponse getLearnerResources(String userId, String taskId)
AgentTraceResponse getTrace(String currentUserId, String taskId)
IndexTaskDetailResponse getIndexTask(String userId, String taskId)
KbDocument getDocument(String userId, String documentId)
DocumentUploadResponse reindex(String userId, String documentId)
```

规则：

1. Controller 只传 `currentUserId`。
2. Service 先判断当前用户能否看到目标对象，再返回详情。
3. 非 admin 缺失对象与越权对象必须返回同类 `FORBIDDEN`。
4. 返回错误时 `data` 为空，message 不包含目标对象 id、父对象 id、traceId 或资源标题。

### 4.4 错误语义

| 场景 | 非 admin | admin |
|---|---|---|
| own learning path / task / trace / index task | 200 | 200 或按服务能力 |
| foreign object | `FORBIDDEN` | 200（如该服务支持 admin 审计） |
| missing object | `FORBIDDEN` | `NOT_FOUND` |
| 未发布 learner resource | `FORBIDDEN` | `FORBIDDEN`（发布门禁不是管理员绕过项） |

### 4.5 测试要求

聚焦测试类：

- `LearningWorkflowControllerTest`
- `ResourceGenerationControllerTest`
- `DocumentControllerTest`

新增用例：

- non-owner 对 foreign path 与 missing path 均得到 `FORBIDDEN`，且无 `data`。
- non-owner 对 foreign resource task / missing resource task 均得到 `FORBIDDEN`，且无 `data`。
- non-owner 对 foreign agent trace / missing agent trace 均得到 `FORBIDDEN`，且无 `data`。
- non-owner 对 foreign document / missing document 均得到 `FORBIDDEN`，且无 `data`。
- non-owner 对 foreign index task / missing index task 均得到 `FORBIDDEN`，且无 `data`。
