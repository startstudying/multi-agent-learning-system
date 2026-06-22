# ACCEPT-20260621 MemoryContextService MVP

## 1. 验收结论

Verdict: PASS

P0-2 `MemoryContextService MVP` 已完成。实现保持在后端 Service 层，不新增 DB schema、依赖或前端改动。

## 2. 验收项

| 验收项 | 结论 | 证据 |
|---|---|---|
| 每条上下文都有来源、分数、注入原因 | PASS | `MemoryContextServiceTest` 断言 `source/score/reason` |
| token budget 有上限 | PASS | `TokenBudget.maxTokens=1200`，estimated 不超过上限 |
| 上下文不包含 raw prompt / provider key / teacher note | PASS | 序列化断言不包含 `teacher_note`、`TEACHER_NOTE`、provider key |
| citation 不保留完整 excerpt | PASS | 测试使用长 excerpt，并断言完整文本未进入序列化上下文 |
| AI QA 响应暴露上下文准备摘要 | PASS | `AiQaControllerTest` 断言 `toolCalls[0].name=MemoryContextService` |
| P0-1 RAG 隐私回归未破坏 | PASS | `RagQueryServiceTest` 22 run, 0 failures |

## 3. 剩余风险

- 近期会话摘要暂用 `learning_event.summary`；真实 chat session 生命周期属于 P1/P2 后续。
- `MemoryContext` 当前只准备上下文，不负责 prompt 编排、模型网关策略、结构化回答或 verifier gate。
- 未运行全量后端测试；本轮执行 focused + adjacent 测试，符合 M 级切片风险范围。

## 4. 下一步

继续 P0-3 `QaRuntime structured answer MVP`：把 `/api/ai/qa` 从 RAG wrapper 升级为运行时入口，串接 intent、memory context、RAG/tool、model gateway、structured answer 和 verifier 前置接口。
