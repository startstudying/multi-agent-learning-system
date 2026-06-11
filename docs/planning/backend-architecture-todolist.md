# 后端架构优化 TODO List

课题定位：大模型个性化学习资源生成与多智能体自适应学习系统。

核心架构线：`LLM + Personalized Learning + Multi-Agent Orchestration + RAG + Assessment Feedback Loop`。

本文档只记录后端架构和工程闭环待办，状态以当前仓库实现为准。

## 计划状态

**Status: Completed（MVP / 最小可答辩闭环） — 2026-06-11**

- P0、P1、P2、P3-1、P3-2（最小生产化）、P3-3（最小 provider 边界）、P3-4（MVP 权限矩阵）、P3-5 主计划项均已完成并有测试/验收证据。
- 验证：`backend` full Maven test `601 run, 0 failures, 0 errors, 1 skipped`。
- 证据：`docs/evidence/EVIDENCE-20260611-backend-architecture-todolist-completion.md`
- 工业级 parser/OCR、Qdrant 真实 smoke、DashScope 专用 provider、持续权限矩阵抽样等后续增强见文末 **后续增强（不阻塞本计划）**。

## 0. 当前基线

- [x] Spring Boot 3.x + Java 21 后端基础工程
- [x] MySQL/Flyway 迁移脚本基础，覆盖 RAG、学习闭环、Agent Trace、资源生成、测评反馈等核心表
- [x] `Controller -> Application Service -> Repository` 基本分层
- [x] `CurrentUserService`、Trace Filter、统一响应 envelope 和异常处理
- [x] RAG KB、文档、chunk、query log、source citation 基础结构
- [x] 学习画像、学习目标、学习路径、知识点 DAG、掌握度记录基础结构
- [x] 资源生成任务、学习资源、资源审核、Agent Task、Agent Trace、Model Call Log、Token Usage Log 基础结构
- [x] 答题提交、自动评分、错因诊断、掌握度更新、路径重规划基础闭环
- [x] 答题反馈已从最新 `mastery_record` 读取更新前掌握度，并执行 BKT-lite 动态掌握度更新
- [x] 答题提交已支持 `requestId` 幂等、响应快照重放、payload 冲突 409 和并发唯一键冲突兜底重放
- [x] 资源发布已接入 Review Gate，未审核资源不直接面向学生发布
- [x] 资源生成已支持 `requestId` 幂等和模型调用失败重试
- [x] 已补入 RAG 质量评估、自动批改质量评估、token/model 维度统计的初步服务和接口

## P0：先把后端主闭环做扎实

目标：保证系统不是几个孤立接口，而是一条可追踪、可恢复、可验收的学习闭环。

### P0-1 统一 Orchestrator Workflow

- [x] 建立统一工作流入口，覆盖学习目标、课程资料问答、资源生成、答题反馈等 AI 链路
- [x] 将 `RESOURCE_GENERATION` 子流程统一到工作流上下文中，输出统一 `workflowId / agentTaskId / traceId`
- [x] 将 `RAG_QA` 子流程统一到工作流上下文中，RAG query log 和 citation 复用 Orchestrator traceId
- [x] 将 `ANSWER_SUBMISSION` 子流程统一到工作流上下文中，答题业务记录复用 Orchestrator traceId，并补齐 requestId 服务层校验、workflow envelope 精确 replay、trace drift transient task/trace 清理
- [x] 明确每个工作流节点的输入 DTO、输出 DTO、失败策略和可重试策略
- [x] 增加工作流查询接口：按 `workflowId` 返回当前状态、最近失败步骤、trace 摘要和可继续动作
- [x] task 创建后的 `RAG_QA` 运行期 `ApiException` 可持久化为 `FAILED` workflow 证据，且不写入伪成功 query/citation

验收：

- [x] `RESOURCE_GENERATION` 可通过 `workflowId` 找到完整链路
- [x] `RAG_QA` 可通过 `workflowId` 找到完整链路
- [x] `ANSWER_SUBMISSION` 可通过 `workflowId` 找到完整链路，重复提交可返回首次 winning workflow 而不留下第二个 workflow task
- [x] 异常不只返回 500，必须能查到失败节点和失败原因（已覆盖资源生成模型失败、RAG_QA 运行期 `ApiException`、通用 `RuntimeException`；`RESOURCE_GENERATION` 已有最小 retry endpoint，逐节点 failure policy 继续在上方策略项细化）

### P0-2 Agent 状态机收敛

- [x] 已有 `PENDING`、`RUNNING`、`WAITING_REVIEW`、`DONE`、`FAILED`、`CANCELLED` 等任务状态基础
- [x] 给 Agent 任务统一补齐状态集合和状态流转约束，避免任意字符串状态写入
- [x] 失败时写入 `agent_task.status = FAILED`、失败 `agent_trace.summary`、失败 `model_call_log` 和 `recoverable` 标记
- [x] 增加取消任务能力：`PENDING`、`RUNNING`、`WAITING_REVIEW` 可取消为 `CANCELLED`，终态任务取消返回 409

验收：

- [x] 单测覆盖成功、失败、取消状态流转
- [x] `agent_trace` 能还原 Agent 步骤执行顺序

### P0-3 幂等、重试和长任务恢复

- [x] 资源生成任务支持 `requestId` 幂等
- [x] 模型结构化生成支持最多 2 次重试
- [x] 答题提交支持 `requestId` 幂等：同 payload 重放首次响应，不同 payload 返回 409，并用 `(learner_id, request_id)` 唯一索引兜底
- [x] 文档索引任务支持 active 去重：同一文档最新任务为 `PENDING/RUNNING` 时重复 reindex 复用任务；`FAILED/SUCCEEDED` 后允许新建 `PENDING`
- [x] 文档索引任务支持服务层超时恢复：超时 `RUNNING` 可标记为 `FAILED`，递增 `retry_count`，写入 `error_message`，设置 `finished_at`
- [x] 文档上传补齐 `requestId` 或业务唯一键
- [x] RAG 检索补齐 query replay / response snapshot
- [x] 文档索引补数据级并发去重约束或锁
- [x] 其他长任务补齐 `retry_count`、`next_retry_at`、`last_error`、`recoverable` 或等价状态（资源生成任务已补齐 V11 恢复字段）
- [x] 增加后台恢复任务：启动后或定时扫描超时 `RUNNING` 任务，转为 `FAILED` 或重新入队

验收：

- [x] 重复点击生成、重复提交答案不产生重复业务结果
- [x] 重复上传不产生重复业务结果
- [x] 重复 RAG query 不产生重复业务结果
- [x] 并发重复提交答案命中唯一索引时不暴露 500
- [x] 模型或索引失败后，任务状态和错误原因可查询

### P0-4 Review Gate 强约束

- [x] 学习资源发布前需要 Critic/教师审核
- [x] 学生端资源读取已检查 `canReleaseToLearner(taskId)`
- [x] 审核约束已下沉到资源查询服务：未审核任务详情和创建响应不返回 `markdownContent`
- [x] 给资源状态补齐 `DRAFT`、`PENDING_CRITIC`、`APPROVED`、`REVISION_REQUESTED`、`REJECTED`、`PUBLISHED`
- [x] 增加审核原因、引用检查结果、安全检查结果、修订建议的结构化字段
- [x] 审核队列和审核决策接口补齐临时教师/管理员权限边界，普通学生返回 403 且不泄露 review/task/resource 详情
- [x] 审核队列和审核决策接口补齐课程范围收口：管理员全局可处理，教师只能查看和处理自己课程的 review

验收：

- [x] 学生端资源查询无法绕过审核状态
- [x] 审核日志能说明为什么通过、退回或拒绝
- [x] 普通学生不能查看或处理审核记录，`admin` 可全局执行审核操作，`teacher` 只能处理自己课程审核记录

## P1：做成真正的自适应学习平台

### P1-1 Learner Profile 更新策略

- [x] 画像来源支持 `CONVERSATION`、`ASSESSMENT`、`RESOURCE_STUDY`、`TEACHER_NOTE`
- [x] 最新画像采用合并更新，不再无序插入多份孤立记录
- [x] 明确画像维度：基础水平、学习目标、薄弱点、偏好、节奏、最近错误模式、教师标注
- [x] 增加每个画像维度的 `confidence`、`sourceType`、`lastEvidenceId`
- [x] 资源生成和路径规划显式引用画像字段，保存 `profile_snapshot`

### P1-2 Knowledge DAG 与路径规划增强

- [x] `knowledge_dependency` 已支持知识点依赖基础能力
- [x] 路径规划已考虑前置知识点顺序
- [x] 区分 `PREREQUISITE`、`RELATED`、`ADVANCED` 三类边，并在规划策略中使用
- [x] 增加掌握度阈值规则，例如低于 0.6 时优先补救前置知识
- [x] 路径节点增加推荐原因、预估时长、资源类型、测评绑定关系

### P1-3 Educational RAG Router

- [x] RAG 响应已有 `strategy`、`retrievalCount`、`NO_SOURCE` 等基础元数据
- [x] 建立显式路由器：`DIRECT_ANSWER`、`COURSE_RAG`、`RAG_WITH_PROFILE`、`RAG_WITH_HISTORY`、`NO_SOURCE_REFUSAL`
- [x] 路由判断输入包含问题复杂度、课程 KB 命中、学习画像、历史答题记录
- [x] `kb_query_log` 持久化 routing strategy、候选 chunk 数、最终引用数、是否降级

### P1-4 引用与幻觉治理

- [x] RAG 答案返回 `sources`
- [x] 无来源时已有 `NO_SOURCE` 语义基础
- [x] Orchestrator `RAG_QA` 已将 query log / citation / workflow trace 对齐到同一 traceId
- [x] 资源生成也必须保存 `source_citation`，不能只在 RAG 问答里有引用
- [x] Critic Agent 增加引用检查：引用数量、引用相关性、是否伪造来源
- [x] 对无来源生成内容强制标记 `NO_SOURCE`，并降低发布等级或进入人工复核

### P1-5 学习分析 API

- [x] 已有 `/api/analytics/overview` 和 token/model 基础统计
- [x] 增加学生端 summary：学习进度、掌握度趋势、最近错因、推荐下一步
- [x] 增加教师端 class summary：班级薄弱知识点、错因分布、资源完成率、待审核资源
- [x] 增加 admin summary：Agent 成功率、失败率、平均延迟、token 成本、RAG 命中率

## P2：论文和答辩加分项

### P2-1 Prompt Version 与实验集管理

- [x] `agent_trace` 已有 `promptVersion` 字段，数据库已有 `prompt_version` 表
- [x] 建立 `PromptVersion` 实体、Repository、管理服务和查询接口
- [x] 每次模型调用记录 prompt code、version、model、temperature、结构化输出 schema
- [x] 增加 evaluation set：RAG 问题集、评分样本集、资源生成样本集
- [x] 支持按 prompt version 对比质量指标

### P2-2 RAG 质量评估

- [x] 已有 RAG evaluation service 和 `/api/rag/evaluations` 初步接口
- [x] 固化课程 benchmark：问题、期望 chunk、标准答案、禁止回答范围
- [x] 指标补齐：`Recall@K`、`Citation Accuracy`、`Groundedness`、`No-source Refusal Rate`
- [x] 增加可重复执行评估脚本和测试，输出可归档报告（`scripts/run-rag-evaluation-archive.ps1`，已生成 `rag-quality-evaluation-report.md`；自动周期 runner 仍归入后续生产化）

### P2-3 自动批改质量评估

- [x] 已有 grading evaluation service 和 `/api/assessment/grading-evaluations` 初步接口
- [x] 建立人工评分样本集：题目、rubric、人工分、LLM/系统分、错因标签
- [x] 指标补齐：平均绝对误差、等级一致率、错因分类一致率
- [x] 增加按题型、知识点、rubric version 的分组分析

### P2-4 成本与 Token 预算治理

- [x] token 统计支持按 Agent Task 和 Model 聚合
- [x] 已有预算状态和降级策略基础字段
- [x] 增加用户、课程、Agent 类型、时间窗口维度的成本统计
- [x] 增加预算规则：超过阈值时返回 deterministic/cached response 降级建议或要求人工确认（analytics 层治理视图；调用前预算门禁仍归入 P3 生产化）
- [x] 增加高成本任务告警和异常模型调用识别

### P2-5 Agent Trace 治理看板

- [x] 已有 Agent Task、Agent Trace、Model Call Log、Token Usage Log
- [x] 增加 trace 查询过滤：用户、Agent 类型、状态、时间、失败原因
- [x] 增加工具调用记录 `agent_tool_call` 的服务层写入和接口展示
- [x] 增加 trace retention 策略，区分长期审计字段和可清理大文本字段

## P3：生产化与工程质量

### P3-1 MySQL Migration 真实验证

- [x] 使用 `backend/docker-compose.yml` 启动 MySQL 8（本机 3306 被 MySQL84 占用，验收使用 `MYSQL_PORT=3307`）
- [x] 增加 migration smoke test：从空库执行 V1-V5 全量迁移，并扩展覆盖当前 V1-V17 全链路迁移
- [x] 检查 H2 测试与 MySQL 方言差异，并记录 H2 profile Flyway disabled、MySQL-only DDL、V1 row-size 修复证据

### P3-2 RAG 索引生产化

- [x] 已有 PDF、DOCX、Markdown、TXT 轻量解析雏形
- [x] 已有基础 chunk 清洗、去重、固定切分、Markdown 章节元数据和简化页码
- [x] 补齐最小生产级 parser adapter：独立 `rag/parser` 边界、Markdown/TXT/PDF/DOCX 统一 section 输出、安全 parser 错误码、worker/manual 共用路径
- [x] 补齐复杂 PDF/DOCX、OCR fallback、真实页码和章节层级识别的最小生产化能力（P3-2-E/F/G/H/I 已完成 PDFBox/POI provider、可配置/process OCR fallback、best-effort `pageNum` / `headingPath` 入 chunk；工业级 layout/table/TOC/native/cloud OCR/真实渲染页码另列后续增强）
- [x] 补齐生产级 chunk token 切分、overlap、稳定 chunk hash 和 heading hierarchy（当前为无新依赖的 token-ish window 过渡方案；真实 tokenizer 校准留待模型接入）
- [x] 增加 embedding service 和可选 VectorDB adapter 边界（`EmbeddingService` 批量 contract + `VectorIndexAdapter` noop 实现 + 索引链路 EMBEDDING/VECTOR_UPSERT 阶段 + chunk metadata 状态字段；Spring AI embedding provider adapter 已完成；vector embedding payload contract 已完成：document/query embedding vectors 可在内存中传给 vector upsert/search request；Qdrant real VectorDB adapter minimum integration 已完成：默认 disabled/noop，显式启用 `provider=qdrant` 才装配真实 adapter，payload 只写低敏 chunk metadata，search 只取 `chunkId` 且不返回 vector；真实服务 smoke、collection dimension validation、health/ops、gRPC/Netty 风险处理仍待后续独立子任务）
- [x] 增加 hybrid retrieval、RRF、reranker timeout fallback（已完成无新增依赖的 `keyword + recency + RRF + reranker fallback`；真实 embedding/vector retrieval 仍归上一项）
- [x] 索引任务已支持 active 幂等、文档行锁、超时失败恢复和 scheduler 恢复
- [x] 索引任务补齐 worker 自动执行、进度、heartbeat、自动重试/重新入队和 task detail API

### P3-3 模型接入边界

- [x] 用 Spring AI/Spring AI Alibaba 接入真实 Chat/Embedding 模型（P3-3-C 已完成最小 Spring AI OpenAI-compatible Chat/Embedding provider adapter：`AiModelGateway` 调用 `ChatModel`，`EmbeddingService` 调用 `EmbeddingModel`，默认 `AI_MODEL_PROVIDER=none` 不外呼；DashScope / Spring AI Alibaba 专用增强和真实外部 smoke 留后续独立切片）
- [x] 所有当前模型调用通过 `AiModelGateway`，禁止业务服务直接调用模型 SDK（P3-3-A 已完成当前代码静态边界检查；真实 provider adapter 仍在上一项）
- [x] 结构化输出必须做 schema 校验和降级处理（P3-3-A 已覆盖 `agent-resource-v1` 的 `resources[]`、必填字段和 `safetyStatus` 白名单；非法输出写入 `STRUCTURED_OUTPUT_INVALID`）
- [x] 模型 provider、model name、prompt version、latency、token、error 全量落日志（P3-3-A 已补齐 model name、prompt version、latency、token、cost、safe error；P3-3-B 已通过 V18 `model_call_log.provider`、entity、recorder 和 provider 归一化完成 provider 持久化；真实 MySQL V18 smoke 受本机凭据限制待环境恢复后补验）

### P3-4 权限与安全加固

- [x] 最小高风险权限收口已完成：Profile owner、Learning Path owner、analytics overview admin-only、Health 输出收敛、RAG mixed `kbIds` strict 拒绝、`GET /api/rag/query` handler 复用 strict 查询入口
- [x] Review Gate 课程范围收口已完成：`admin` 全局可审核，`teacher` 只能 list/decision `ResourceGenerationTask.goalId -> Course.teacherId` 匹配自己的 review
- [x] 对象详情防枚举切片已完成：`GET /api/learning-paths/{pathId}`、`GET /api/resources/generation-tasks/{taskId}`、`GET /api/agent/tasks/{taskId}/trace`、`GET /api/documents/{documentId}`、`POST /api/documents/{documentId}/reindex`、`GET /api/index-tasks/{taskId}` 对非 admin 统一收敛 missing/foreign 为 `FORBIDDEN` 且无 `data`
- [x] formal OAuth2/JWK/Spring Security 最小认证边界已完成：Bearer 由 Spring Security Resource Server 处理，支持 JWK Set URI、本地 HS256 兼容、issuer/audience 校验、production-like fail-fast、统一 401/403 envelope，且 `DevAuthFilter` 只保留 dev/test 无 Bearer fallback
- [x] RAG query runtime roles-first RBAC 已完成：`/api/rag/query`、Chat/Tutor runtime、Orchestrator `RAG_QA` replay precheck 与执行路径均传递 explicit admin/teacher facts；Bearer `USER sub=admin` 不再获得 RAG runtime admin KB read 语义
- [x] KB-course binding schema/lifecycle governance 已完成：`kb_knowledge_base` 记录 `course_id / binding_status / bound_by / bound_at`；`BOUND` KB read/write 统一走 `CourseAccessService`；空 `UNBOUND` KB 首次合法 course 文档上传自动绑定；`CONFLICTED` KB 安全封锁；requestId payload conflict 保持 `409 CONFLICT`
- [x] SSE production auth strategy 已完成：Chat/Tutor SSE 在 production/staging 下固定为 Bearer/JWT fail-closed；no Bearer、invalid Bearer、header-only spoofing 在 async 与 `RagQueryService` 前被拒绝；valid Bearer 使用 JWT subject/roles 并忽略 spoofed `X-User-Id`；未新增 query token / signed stream token / cookie/session
- [x] 前端 SSE sensitive URL cleanup 最小切片已完成：production/staging 学生端 RAG 问答不再创建原生 `EventSource`，避免 `question` / `kbIds` 出现在 SSE GET URL；先改走已有 `POST /api/rag/query`，dev/test 保留 SSE 流式演示；正式 production streaming 已由 `P3-4 子任务：formal production streaming design` 补齐
- [x] 正式 production streaming 设计已完成：production/staging 学生端 RAG 使用 `POST /api/rag/query/stream` + `fetch` / `ReadableStream`，`question` / `kbIds` 保持在 POST body；后端 `text/event-stream` 输出 `status/token/done/error`，并复用 Bearer/JWT role facts、`RagQueryService` 权限与 requestId replay 语义；dev/test legacy GET SSE 保留为演示兼容
- [x] RAG KB、课程、学习路径、资源、答题记录全部补齐 MVP 生产级权限检查（RAG KB management、RAG query runtime roles-first RBAC、KB-course binding governance、Course/Knowledge Catalog 写路径、legacy subject-name 授权清理、对象详情防枚举、formal OAuth2/JWK、高价值 course/class/resource/answer penetration matrix、teacher permission residual sampling matrix、Orchestrator `ANSWER_SUBMISSION` replay scope revalidation、formal production streaming design 均已验收；持续业务矩阵抽样复核归入后续增强）
- [x] 教师端只能访问授权班级/课程数据（Review Gate 课程范围、Course/Knowledge Catalog 写路径、answer/wrong-question teacher own-course + active enrollment、review list redaction、course list spoofed-header、student summary DROPPED learner redaction、Course/Knowledge forged business-object、teacher permission residual sampling matrix 均已验收；更广 teacher-side 数据域扩展归入后续增强）
- [x] 学生端只能访问自己的画像、路径、资源和答题记录（画像、路径、资源/trace/detail 防枚举已完成；`AssessmentService` 答题记录 list/detail 目标 subject-name legacy overload 已清理；answer/wrong-question Bearer student + spoofed header owner-only 矩阵已补齐）
- [x] 增加最小权限渗透测试：owner mismatch、非 admin overview、health 敏感字段、RAG mixed `kbIds` 越权检索
- [x] 增加 Review Gate 课程范围权限测试：教师只能列出本课程 review，不能处理外部课程 review，管理员保持全局能力
- [x] 扩展完整权限渗透测试 MVP 矩阵：course/class、answer record、teacher/student/admin RBAC、对象详情防枚举、SSE production/staging auth、legacy subject-name 反射守卫、learner-resources、course-bound resource create、knowledge dependency、analytics student summary、review list redaction、agent task cancel / course create role-confusion、PromptVersion / Orchestrator / Evaluation / Assessment forged-id、dev/test legacy fallback cleanup、frontend SSE sensitive URL cleanup、teacher permission residual sampling matrix、Orchestrator `ANSWER_SUBMISSION` replay scope revalidation 均已补齐并通过 full backend `601 run, 0 failures`；更广业务矩阵持续抽样归入后续增强）

### P3-5 可观测性与运维

- [x] 增加结构化日志：traceId、userId、route、status、latency、errorCode（请求完成日志已通过 `StructuredRequestLoggingFilter` 输出白名单字段；非法 `X-Trace-Id` 会被替换，错误码由统一异常处理传递）
- [x] 增加 Micrometer 指标：请求延迟、RAG 延迟、模型延迟、失败率、token 成本（通过 `LearningOsMetrics` 集中封装；HTTP、RAG、模型网关、token/cost 指标已接入 Micrometer，`/actuator/metrics` 已暴露，tags 只使用低基数非敏感字段）
- [x] 增加健康检查深度：数据库、Redis、MinIO、模型 provider（`/api/health` 现在对数据库执行 `DataSource` 轻量探测、Redis 执行 `ping()`、MinIO 做 endpoint/client 构造检查、模型 provider 做 `none`/configured/unconfigured 状态判断；响应只返回稳定状态、布尔 metadata 和固定错误码，不泄露连接串、host、bucket、key、secret 或 raw exception）
- [x] 增加慢查询、慢模型调用、无引用回答、审核积压告警（`GET /api/analytics/ops/alerts` 已提供 admin-only 查询型告警聚合，覆盖 `SLOW_RAG_QUERY`、`SLOW_MODEL_CALL`、`RAG_NO_SOURCE`、`REVIEW_BACKLOG`，响应使用白名单 DTO，不暴露 prompt/question/raw response/errorMessage/markdownContent/review 私有字段；外部推送、通知渠道和告警持久化仍属后续生产化）

## 最小可答辩闭环

```text
学习目标
-> learner_profile 初始化 / 更新
-> learning_path 规划
-> course RAG 检索课程资料
-> resource generation 生成个性化资源
-> critic/review 审核
-> assessment 批改
-> feedback diagnosis 错因反馈
-> mastery_record 更新
-> learning_path replan
-> analytics / trace / citation 输出证据
```

最小演示必须证明：

- 个性化：资源和路径确实引用画像、掌握度和历史错误
- 可信：回答和资源有引用，无来源时不编造
- 自适应：答题后掌握度变化会触发路径调整
- 可审计：Agent Trace、Model Call、Token Usage、Review Log 都能查询
- 可评估：RAG 和自动批改至少各有一组离线评估指标

## 后续增强收口状态（不阻塞本计划）

以下项属于生产化深化或持续维护，不影响 MVP / 最小可答辩闭环计划完成。2026-06-11 已完成后续增强事实核验、专家 subagent 并行评审、opt-in smoke 实测化和收口证据；仍保留的未勾选项需作为后续独立任务继续推进。

### P3-2 工业级文档解析

- [x] 工业级 PDF layout/table/TOC/reading-order、native/cloud OCR、OCR confidence 与真实渲染页码增强（PDF `RENDERED_PAGE` / `TABLE` / `TOC` 启发式、`layoutConfidence`、OCR fallback confidence；完整 native/cloud OCR 流水线仍可按环境 opt-in 扩展）

### P3-2 VectorDB 运维

- [x] Qdrant 真实服务 smoke（`@Tag("external-smoke")` + `QDRANT_SMOKE_ENABLED=true` opt-in）、collection dimension validation（`expected-dimension`）、health/ops（`/api/health` vector 组件 + `QdrantVectorHealthProbe`）、gRPC/Netty 风险处理（probe 超时 + health metadata）

### P3-3 模型 provider 深化

- [x] 多 Provider 注册后端：Admin CRUD、API Key 加密、`AiModelGateway` registry 优先路由（DeepSeek / MiMo / DashScope / OpenAI-compatible）
- [x] Admin 前端供应商配置页（`/admin/model-providers`，对齐截图字段：名称/备注/官网/API Key/Base URL/Chat/Embedding）
- [x] Embedding 路径读取 registry provider（`ModelProviderService.resolveDefaultEmbeddingProvider` + `OpenAiCompatibleEmbeddingModelFactory`）
- [x] DashScope / Spring AI Alibaba 专用 provider 增强（OpenAI-compatible DashScope preset + 前端 preset）
- [x] 真实外部 provider smoke、MySQL V18 provider 字段 smoke（`@Tag("external-smoke")` + `MODEL_PROVIDER_SMOKE_ENABLED=true` opt-in；V18 provider 字段已有单测覆盖）

### P3-4 权限持续维护

- [x] 更广 teacher-side 数据域权限扩展（`BusinessPermissionMatrixRegressionTest` 回归：student/teacher/admin 对 model-providers、token-budget、ops-alerts 边界）
- [x] 持续业务矩阵抽样复核（回归门禁，不作为单次计划阻塞项）

### P3-5 / P2-4 运维与成本深化

- [x] 告警外部推送、通知渠道、告警持久化（`ops_alert_record` + webhook opt-in + `/api/analytics/ops/alerts/persisted` + acknowledge API + Admin Operations 真实告警区）
- [x] 调用前 token 预算门禁（`TokenBudgetGateService` + `AiModelGateway` deterministic fallback when over budget）
