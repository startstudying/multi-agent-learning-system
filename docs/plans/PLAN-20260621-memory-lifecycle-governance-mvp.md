# PLAN-20260621 Memory lifecycle governance MVP

## 1. 执行顺序

1. 写 RED tests：
   - `MemoryLifecycleServiceTest`
   - `MemoryContextServiceTest`
   - `SchemaConvergenceMigrationTest` V23
2. 创建 V23 migration。
3. 扩展 `KbChatSession` / `KbChatMessage` entity。
4. 新增 `KbChatMessageRepository`，扩展 `KbChatSessionRepository`。
5. 新增 `MemoryLifecycleService`。
6. `AiQaService` 成功回答后写低敏 session memory。
7. 新增 `AiQaMemoryController`。
8. `MemoryContextService` 优先读取 active session message。
9. 跑 focused / adjacent / compile。
10. 写 Evidence / Acceptance，更新总计划、memory、changelog。

## 2. 风险与处理

| 风险 | 处理 |
|---|---|
| Raw question / full answer 被持久化 | 只用 `MemoryPrivacyPolicy.questionLogValue`、answerLength、sourcePolicy、verification |
| 用户编辑写入敏感内容 | update summary 走敏感标记过滤 |
| DB migration 影响 MySQL smoke | 更新 migration smoke version/count 和 schema convergence test |
| MemoryContext 注入过期/删除记忆 | Repository 查询和测试覆盖 |
| 过度实现长期记忆系统 | 只做 session summary、salience、decay、edit/delete MVP |

## 3. 测试命令

```powershell
cd backend
mvn --% -Dtest=MemoryLifecycleServiceTest,MemoryContextServiceTest,SchemaConvergenceMigrationTest test
mvn --% -Dtest=AiQaControllerTest,QaRuntimeTest test
mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile
```

## 4. 完成判定

- V23 schema 文本测试通过。
- memory lifecycle service 测试通过。
- memory context 排除 deleted/expired 测试通过。
- AI QA 回归通过。
- Evidence / Acceptance / Memory / Changelog 更新完成。

## 5. 执行结果

状态：已完成。

交付：

- 新增 `V23__memory_lifecycle_governance.sql`，扩展 `kb_chat_session` / `kb_chat_message` lifecycle 字段和 `idx_kb_chat_message_lifecycle`。
- 扩展 `KbChatSession` / `KbChatMessage` entity，新增 `KbChatMessageRepository`，扩展 `KbChatSessionRepository`。
- 新增 `MemoryLifecycleService`，负责低敏 QA memory summary、salience/decay、列表、owner-only edit/delete。
- 新增 `AiQaMemoryController`：`GET /api/ai/memory/sessions`、`PATCH /api/ai/memory/messages/{messageId}`、`DELETE /api/ai/memory/messages/{messageId}`。
- `AiQaService` 在成功回答后写入低敏 session memory。
- `MemoryContextService` 优先读取 active session memory，无 active memory 时回退 learning event。

验证：

- Focused：`mvn --% -Dtest=MemoryLifecycleServiceTest,MemoryContextServiceTest,SchemaConvergenceMigrationTest test`，26 run, 0 failures, 0 errors。
- Adjacent：`mvn --% -Dtest=AiQaControllerTest,QaRuntimeTest test`，7 run, 0 failures, 0 errors。
- Compile：`mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile`，Build SUCCESS。

说明：

- 真实 MySQL smoke 未运行；该测试是 opt-in，需要可用 MySQL 8 环境。
