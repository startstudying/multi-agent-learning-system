# SPEC-20260621 记忆系统与回答质量架构规格

## 1. 当前系统证据

已具备的基础：

- `AiModelGateway`：模型网关、结构化输出、provider 观测和错误脱敏基础。
- `RagQueryService`：RAG 检索、权限过滤、query log、citation 基础。
- `LearningWorkflowService`：学习者画像、路径、资源生成上下文基础。
- `OrchestratorWorkflowService`：Agent task/trace 编排基础。
- Prompt Version、Evaluation Set/Run、Agent Trace、Model Call Log、Token Usage 已有基础表和服务。

关键缺口：

- `AiQaService` 仍主要包装 RAG，不是统一 `QaRuntime`。
- `answerMode` 只映射到 `low/medium/high` 字符串，未改变真实检索、工具、reviewer、模型策略。
- QA 未系统接入长期记忆、近期会话摘要、Verifier、Eval gate。
- `kb_chat_session` / `kb_chat_message` 当前只有空壳字段，不能支撑高质量多轮状态。
- RAG no-source fallback 仍偏模板化，不是真正综合回答链路。

## 2. 目标架构

```text
Vue Frontend
  -> Spring Boot Controller
  -> QaRuntime
       -> IntentRouter
       -> MemoryContextService
            -> LearnerProfileSummary
            -> LearningEvent / Mastery / WrongQuestion
            -> RecentSessionSummary
            -> RAG Retrieval / Citation
            -> Preference / Project Memory
       -> ContextOrchestrator
       -> ToolExecutor
       -> AiModelGateway
       -> AnswerVerifier
       -> FinalComposer
       -> Eval / Trace / Feedback Log
```

## 3. 核心组件

### 3.1 IntentRouter

输入：用户问题、mode、课程/KB/会话上下文。

输出：

```json
{
  "taskType": "QA",
  "complexity": "medium",
  "needsRag": true,
  "needsMemory": true,
  "needsClarification": false,
  "riskLevel": "medium",
  "qualityPolicy": "source_required"
}
```

### 3.2 MemoryContextService

职责：

- 聚合业务记忆与 RAG 上下文。
- 按优先级和 token budget 裁剪。
- 记录每条 memory/citation 被注入的原因和分数。
- 输出模型可消费但不泄露敏感原文的 `MemoryContext`。

优先级：

1. P0 系统策略、安全规则、权限边界。
2. P1 当前用户显式请求。
3. P2 当前任务约束、课程/KB/模式。
4. P3 RAG 证据、学习状态、近期会话摘要。
5. P4 项目/课程长期知识。
6. P5 用户偏好与历史经验。

### 3.3 MemoryRouter

根据任务生成 read/write plan：

- 普通问答：少量 preference + RAG + recent summary。
- 学习诊断：profile summary + mastery + wrong-question + RAG。
- 资源生成：profile 最小字段 + source citation + review policy。
- 长任务继续：task buffer + recent summary + trace summary。

### 3.4 PrivacyPolicy / MemoryWritePipeline

写入前必须执行：

`CandidateExtraction -> Classification -> Dedup -> ConflictDetection -> ImportanceScore -> SensitivityScore -> PolicyCheck -> Commit`

默认策略：

- `teacher_note` 不进入 prompt 和长期记忆。
- 原始 question/answer/excerpt 默认不进入长期记忆。
- RAG replay snapshot 只保留必要低敏字段、hash、长度和 citation id。
- 长期记忆必须支持用户查看、修改、删除或管理员治理。

### 3.5 QaRuntime

推荐接口：

```java
QaAnswer run(QaRequest request, UserContext userContext);
```

主要步骤：

1. `IntentRouter.classify(...)`
2. `MemoryContextService.build(...)`
3. `RagQueryService.retrieve(...)` 或未来 `ToolExecutor.retrieveCourseChunks(...)`
4. `ContextOrchestrator.buildPromptInput(...)`
5. `AiModelGateway.generateStructured(...)`
6. `AnswerVerifier.verify(...)`
7. `FinalComposer.compose(...)`
8. `Trace/Eval/FeedbackLog.persist(...)`

### 3.6 AnswerVerifier

MVP 先做规则 + 轻量 LLM reviewer 可选：

- schema 合规。
- citation 必填策略。
- no-source 策略。
- 隐私泄露关键词/字段检查。
- prompt injection 响应检查。
- 答案完整性和下一步建议检查。

### 3.7 EvalStore

Eval 数据集至少标记：

- `SOURCE_REQUIRED`
- `NO_SOURCE`
- `PROMPT_INJECTION`
- `PRIVACY_LEAK`
- `CROSS_USER_MEMORY_LEAK`
- `PERSONALIZED_TUTORING`
- `TOOL_USE`
- `MULTI_TURN_MEMORY_DRIFT`

每次策略变更需要记录 baseline vs candidate。

## 4. 数据模型方向

后续实现可优先采用 MySQL 兼容 MVP，而不是立即切 PostgreSQL + pgvector：

- 复用现有 MySQL 业务表承载 profile、trace、evaluation、RAG citation。
- 新增 memory MVP 时可先使用 MySQL `memory_item` + keyword/hybrid search，向量存储沿用现有 VectorIndexAdapter/Qdrant 边界。
- 如果未来确定 PostgreSQL + pgvector，需要另行 ADR 和迁移评审。

建议 `memory_item` 概念字段：

- `id`
- `tenantId`
- `userId`
- `courseId`
- `taskId`
- `scope`
- `type`
- `summary`
- `contentRef`
- `metadataJson`
- `confidence`
- `importance`
- `sensitivity`
- `source`
- `sourceRef`
- `expiresAt`
- `deletedAt`

## 5. 架构边界

- Controller 不拼复杂 prompt。
- Tool 只能调用 Service 层，不能直接访问 Mapper/Repository。
- Prompt 不能替代权限控制。
- Agent loop 必须有最大轮数。
- RAG 答案必须可追溯来源。
- AI 生成资源默认进入 review workflow。
- 重要操作必须有 `traceId`。

## 6. 不立即实现的内容

- GraphMemory / Neo4j。
- 多 Agent 共享组织记忆。
- 自动复杂冲突推理。
- 长期记忆自我进化。
- 无评审的新运行时依赖。
