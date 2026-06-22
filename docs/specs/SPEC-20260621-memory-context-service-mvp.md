# SPEC-20260621 MemoryContextService MVP

## 1. 背景

`PLAN-20260621-memory-answer-quality-execution-readable.md` 已完成 P0-1 Memory/RAG Privacy Guard。当前 `/api/ai/qa` 仍主要由 `AiQaService` 包装 `RagQueryService`，缺少统一的、低敏的、可解释的上下文构造层。

P0-2 目标是在后端新增 `MemoryContextService` MVP，为后续 `QaRuntime`、`ContextOrchestrator`、`AnswerVerifier` 提供稳定输入。

## 2. 范围

本切片只实现后端服务层能力：

- 新增 `MemoryContextService`，构造统一 `MemoryContext`。
- 上下文来源包括低敏画像摘要、学习状态信号、RAG citation、近期学习事件摘要、偏好记忆。
- 每条可注入上下文必须包含 `source`、`score`、`reason`。
- 添加 token budget 和列表上限，禁止无限填充历史。
- 接入 `AiQaService`，通过现有 `toolCalls` 返回上下文准备摘要，不修改请求 DTO。

本切片不实现：

- 不新增 DB 表或迁移。
- 不新增依赖。
- 不改变 `/api/ai/qa` 请求字段。
- 不实现完整 `QaRuntime`、`IntentRouter`、`ContextOrchestrator`、`AnswerVerifier`。
- 不把模型调用或前端改造纳入范围。

## 3. 设计

新增结构：

```java
record MemoryContext(
    LearnerSummary learner,
    List<LearningSignal> learningSignals,
    List<RagCitationContext> citations,
    List<RecentSessionSummary> recentSessions,
    List<PreferenceMemory> preferences,
    List<ContextInjectionReason> injectionReasons,
    TokenBudget budget
) {}
```

服务入口：

```java
MemoryContext build(String learnerId, String courseId, RagQueryResponse ragResponse, String answerMode)
```

构造规则：

- `learner` 使用 `MemoryPrivacyPolicy.profileRef(profileId)`，不暴露 raw `learnerId`。
- profile 只提取低敏字段，例如目标、弱点、资源偏好、学习节奏；跳过 `teacher_note` / `TEACHER_NOTE`。
- 学习状态从 `mastery_record` 和 `wrong_question` 取最近少量摘要。
- 近期摘要从 `learning_event.summary` 取最近少量记录；`kb_chat_session` / `kb_chat_message` 仍是未来项。
- RAG citation 只保留短摘要和引用元数据，不保留完整 excerpt。
- 所有文本进入上下文前做长度限制和敏感标记过滤。
- token budget 采用确定性估算，超过预算时截断并标记 `truncated=true`。

## 4. 安全与治理

- 不保存或暴露 raw prompt、provider key、teacher note、完整 citation excerpt。
- 不让前端直接访问 LLM API。
- 不让 Agent/Tool 绕过 Service 层访问 Repository。
- P0-2 只构造上下文，不直接执行模型调用，不新增 agent loop。

## 5. 验收标准

- `MemoryContextService` 单测证明每条上下文都有来源、分数和注入原因。
- 单测证明 token budget 有上限，长文本/长列表会截断。
- 单测证明序列化后的上下文不包含 raw prompt、provider key、`teacher_note`、完整 excerpt。
- `/api/ai/qa` 响应的 `toolCalls` 包含 `MemoryContextService` 上下文准备摘要。
- 相关 RAG/AI QA 回归测试通过。
