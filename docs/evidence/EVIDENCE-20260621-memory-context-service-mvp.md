# EVIDENCE-20260621 MemoryContextService MVP

## 1. 范围

本证据对应 P0-2 `MemoryContextService MVP`。

完成内容：

- 新增 `MemoryContextService` 和 `MemoryContext` 结构。
- 从低敏画像、掌握度、错题、学习事件和 RAG citation 构造有限上下文。
- 每条上下文带 `source`、`score`、`reason`。
- 上下文文本经过敏感标记过滤和长度限制。
- `AiQaService` 通过现有 `toolCalls` 返回 `MemoryContextService` 准备摘要。

## 2. RED 证据

命令：

```powershell
mvn --% -Dtest=MemoryContextServiceTest test
```

结果：失败，符合预期。

关键失败：

- `MemoryContextService` 不存在。
- `MemoryContext` 不存在。
- `LearningEventRepository.findByLearnerId(String, Pageable)` 不存在。
- `MasteryRecordRepository.findByLearnerId(String, Pageable)` 不存在。

该失败证明测试先于实现，并且失败原因是 P0-2 能力缺失。

## 3. GREEN 证据

主代码编译：

```powershell
mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile
```

结果：

- Build: SUCCESS
- 新增 `MemoryContextService` 无 deprecation 警告。

命令：

```powershell
mvn --% -Dtest=MemoryContextServiceTest test
```

结果：

- Tests run: 1
- Failures: 0
- Errors: 0
- Build: SUCCESS

覆盖点：

- 上下文包含低敏 learner summary、learning signals、RAG citations、recent sessions、preferences。
- 每条上下文包含 `source`、`score`、`reason`。
- `TokenBudget.maxTokens=1200`，估算 token 不超过上限。
- 长 excerpt / 敏感字段触发 `truncated=true`。
- 序列化上下文不包含 raw `learnerId`、`teacher_note`、`TEACHER_NOTE`、provider key、完整 excerpt。

## 4. Adjacent 回归

命令：

```powershell
mvn --% -Dtest=AiQaControllerTest,RagQueryServiceTest test
```

结果：

- `AiQaControllerTest`: 4 run, 0 failures, 0 errors
- `RagQueryServiceTest`: 22 run, 0 failures, 0 errors
- Total: 26 run, 0 failures, 0 errors
- Build: SUCCESS

覆盖点：

- `/api/ai/qa` 保持统一响应契约。
- `toolCalls[0]` 返回 `MemoryContextService`、`SUCCESS` 和 `contextItems=` 摘要。
- P0-1 RAG 隐私防线回归仍通过。

## 5. 非阻塞说明

- 未运行全量 `mvn test`，因为本切片为 M 级 focused + adjacent 验证；未触碰 schema、依赖、前端和模型 provider。
- `kb_chat_session` / `kb_chat_message` 仍未接入，本 MVP 使用 `learning_event.summary` 作为近期摘要来源。
