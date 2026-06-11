# REQ-20260607 后端架构 TODO 完成计划

## Task 3 需求：P3-5-A 运维告警 API

| ID | Requirement | Priority |
|---|---|---|
| REQ-P3-5-A-1 | 提供 `GET /api/analytics/ops/alerts` 查询型告警 API。 | P0 |
| REQ-P3-5-A-2 | API 仅允许临时 `admin` 访问；非 admin 和缺失 `X-User-Id` 返回 `FORBIDDEN`。 | P0 |
| REQ-P3-5-A-3 | 返回四类告警：`SLOW_RAG_QUERY`、`SLOW_MODEL_CALL`、`RAG_NO_SOURCE`、`REVIEW_BACKLOG`。 | P0 |
| REQ-P3-5-A-4 | 支持 `from`、`to`、`slowQueryMs`、`slowModelMs`、`noSourceRateThreshold`、`noSourceMinCount`、`reviewBacklogHours`、`reviewBacklogCount` 参数。 | P0 |
| REQ-P3-5-A-5 | 阈值边界使用 `>=`；非法时间窗口、非正阈值和非法比例返回 `VALIDATION_ERROR`。 | P0 |
| REQ-P3-5-A-6 | 响应必须使用白名单 DTO，不直接返回 entity，不复用包含 `errorMessage` 的异常模型调用 DTO。 | P0 |
| REQ-P3-5-A-7 | 响应不得包含 `prompt`、`question`、`responseJson`、`sourcesJson`、`errorMessage`、`markdownContent`、`citationCheck`、`revisionSuggestion`、raw provider error、secret。 | P0 |
| REQ-P3-5-A-8 | 不新增依赖、不改 DB schema、不改前端。 | P0 |

## 1. 总体需求

| ID | Requirement | Priority |
|---|---|---|
| REQ-1 | 使用专家 subagent 并行分析剩余 P3 工作，并保存 subagent run 报告。 | P0 |
| REQ-2 | 每个实现切片必须先有 PRD/REQ/SPEC/PLAN/TASK/CONTEXT。 | P0 |
| REQ-3 | 每个切片必须运行聚焦测试，能运行时运行全量测试。 | P0 |
| REQ-4 | 不新增依赖；如必须新增，先创建 `docs/security/` dependency review。 | P0 |
| REQ-5 | 更新 TODO、memory、changelog、evidence、acceptance。 | P0 |

## 2. 第一轮切片需求：Course / Knowledge Catalog 权限收口

| ID | Requirement | Priority |
|---|---|---|
| REQ-P3-4-A-1 | `POST /api/courses` 禁止普通 student 创建课程。 | P0 |
| REQ-P3-4-A-2 | teacher 创建课程时，请求 `teacherId` 必须为空或等于当前用户；保存时 teacherId 使用当前用户。 | P0 |
| REQ-P3-4-A-3 | admin 可全局创建课程，并可指定 `teacherId`；未指定时使用 admin。 | P0 |
| REQ-P3-4-A-4 | `POST /api/courses/{courseId}/chapters` 只允许 admin 或该 course 的 teacher。 | P0 |
| REQ-P3-4-A-5 | `POST /api/knowledge-points` 只允许 admin 或对应 course 的 teacher；带 chapter 时 chapter 必须属于 course。 | P0 |
| REQ-P3-4-A-6 | `POST /api/knowledge-dependencies` 只允许 admin 或对应 course 的 teacher；跨 course dependency 继续拒绝。 | P0 |
| REQ-P3-4-A-7 | 读接口暂保持兼容，不在本切片收紧。 | P1 |
| REQ-P3-4-A-8 | 越权响应不得返回对象详情，错误码为 `FORBIDDEN`。 | P0 |

## 3. 约束

- 不改数据库 schema。
- 不改 API path。
- 不引入新依赖。
- 不改变已有 Review Gate course scope 语义。

## 4. 第二轮切片需求：对象详情防枚举与 scoped authorization

Status: Done。验收证据见 `docs/evidence/EVIDENCE-20260607-backend-architecture-completion.md` 与 `docs/acceptance/ACCEPT-20260607-backend-architecture-completion.md`。

| ID | Requirement | Priority |
|---|---|---|
| REQ-P3-4-B-1 | 非 owner 访问 foreign learning path 与 missing learning path 时响应同类 `FORBIDDEN`，且不返回 `data`。 | P0 |
| REQ-P3-4-B-2 | 非 owner 访问 foreign resource generation task 与 missing task 时响应同类 `FORBIDDEN`，且不返回 task/resource/trace 数据。 | P0 |
| REQ-P3-4-B-3 | 非 owner 访问 foreign agent trace 与 missing trace task 时响应同类 `FORBIDDEN`，且不返回 trace 数据。 | P0 |
| REQ-P3-4-B-4 | 非 owner 访问 foreign RAG document / index task 与 missing document / index task 时响应同类 `FORBIDDEN`，且不返回对象数据。 | P0 |
| REQ-P3-4-B-5 | `admin` 可保留真实 `NOT_FOUND` 语义；不在本切片新增完整 RBAC/JWT。 | P1 |
| REQ-P3-4-B-6 | 答题记录当前无独立详情接口；本切片记录为无直接 IDOR 暴露面，不新增 answer detail API。 | P1 |
