# PRD-20260621 Memory lifecycle governance MVP

## 1. 背景

P0/P1 已经把 AI QA 的隐私、上下文、结构化回答、verifier、stream 展示串起来，但记忆仍缺少生命周期治理。现有 `kb_chat_session` / `kb_chat_message` 只有 `id`，不能表达 salience、decay、可编辑、可删除，也不能承接 ChatGPT-like 会话记忆。

## 2. 目标

提供 P2 最小可治理版本：

- 扩展 `kb_chat_session` / `kb_chat_message`，支持 learner/course/session/message 元数据。
- QA 完成后写入低敏 session memory summary。
- 计算 salience 和 decay，避免所有记忆永久有效。
- 提供用户可编辑/删除的 memory message API。
- `MemoryContextService` 优先读取未删除、未过期的 session summary，再回退学习事件。

## 3. 用户价值

- 学习者的会话记忆可被治理、纠错、删除。
- 系统不会默认长期保留 raw question、完整 answer 或 hidden context。
- 后续前端可接入记忆列表和纠错 UI。

## 4. 非目标

- 不保存 raw question 或完整 answer。
- 不实现复杂冲突合并、跨设备同步 UI、教师批量治理后台。
- 不实现向量化长期记忆。
- 不新增依赖。
- 不改变 AI QA 请求前端合同。

## 5. 成功标准

- 新 QA 轮次产生低敏 `kb_chat_session` / `kb_chat_message` 记录。
- message summary 只包含 hash/length/source/verification 等低敏字段。
- message 有 salienceScore、decayAt、editable、deletedAt。
- 用户只能编辑/删除自己的 memory message。
- MemoryContext 不注入已删除或已过期 session memory。
