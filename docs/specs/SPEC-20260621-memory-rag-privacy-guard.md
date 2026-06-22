# SPEC-20260621 Memory/RAG Privacy Guard

## 1. 背景

`PLAN-20260621-memory-answer-quality-execution-readable.md` 要求先治理记忆与 RAG 写入边界，再扩展长期记忆和 QA runtime。当前代码中有三个直接风险点：

- `RagQueryService` 会把 `question` 写入 `kb_query_log.question`，并在 requestId replay 场景把完整 `RagQueryResponse` 写入 `responseJson`。
- `SourceCitationRecord.excerpt` 会持久化 citation excerpt。
- `LearningWorkflowService` 与 `ResourceGenerationService` 的 `profileSnapshot` 包含 `learnerId` 和 `teacher_note`，其中资源生成还会把该快照传入 `ResourceAgent` 模型上下文。

## 2. 目标

本切片只实现 P0-1 最小隐私守门：

1. 新增统一隐私策略服务，供 RAG、学习路径、资源生成写入前调用。
2. RAG query log 不再保存完整原始问题，只保存 hash、长度和低敏元数据。
3. 持久化 citation record 不再保存完整 excerpt，只保存 citation reference、hash 和长度。
4. replay snapshot 不再保存完整 answer/source excerpt；replay 命中时返回降级响应。
5. `profileSnapshot` 改为 `profileRef + 必要低敏字段`，不再包含 raw `learnerId` 和 `teacher_note`。
6. `ResourceAgent` 模型上下文中的 `profileSnapshot` 不包含 `teacher_note`。

## 3. 非目标

- 不新增数据库迁移，不删除既有列。
- 不改变 REST request/response DTO 字段名。
- 不新增外部依赖。
- 不实现 P0-2 `MemoryContextService`、P0-3 `QaRuntime`、P0-4 verifier/eval gate。
- 不清洗历史已写入数据；本切片只约束新写入数据。

## 4. 设计

### 4.1 `MemoryPrivacyPolicy`

新增 `com.learningos.common.privacy.MemoryPrivacyPolicy`：

- `questionLogValue(rawQuestion)`：输出 `questionHash`、`questionLength`、`policy`，不返回 raw question。
- `citationExcerptValue(rawExcerpt, citationRef)`：输出 citation reference、excerpt hash、excerpt length、policy，不返回完整 excerpt。
- RAG replay snapshot 由 `RagQueryService` 组装，策略服务提供固定降级 answer 和 excerpt 元数据格式；快照保留 `traceId`、retrieval metadata 和 citation identity，隐藏 answer 与 source excerpt。
- `profileRef(profileId)`：输出可引用的 profile reference；无 profile 时返回固定缺省 reference。

### 4.2 RAG 持久化策略

- `kb_query_log.question` 保存 `questionHash=...;questionLength=...;policy=hash_only_v1`。
- `kb_query_log.response_json` 保存降级 `RagQueryResponse`，answer 使用固定降级说明，sources 的 excerpt 使用 citation reference/hash/length。
- `source_citation.excerpt` 保存 citation reference/hash/length。
- API 现场响应仍可返回当前用户可访问的 citation excerpt；只有 durable replay snapshot 降级。

### 4.3 Profile Snapshot 策略

- `learning_path.profile_snapshot` 与 `resource_generation_task.profile_snapshot` 保留：
  - `profileRef`
  - `target`
  - `baseline_level`
  - `learning_goal`
  - `weak_point`
  - `preference`
  - `pace_and_feedback`
  - `recent_error_pattern`
  - `sources`
- 不再写入 `learnerId`。
- 不再写入 `teacher_note`。
- 资源生成传给模型的 `profileSnapshot` 复用同一份低敏快照。

## 5. Replay 降级策略

requestId replay 命中时，返回第一次请求的 `traceId` 和 retrieval metadata，但 answer/source excerpt 来自降级快照。客户端如需要完整回答，应重新提交原始问题并生成新的 requestId。这样避免为了幂等 replay 长期保存完整原始问题、完整回答或完整 excerpt。

## 6. 架构漂移检查

| 检查项 | 结论 |
|---|---|
| Controller 仅委托 Service | 不改 Controller，保持 PASS |
| Service 层执行业务与隐私策略 | PASS，策略服务由 application service 调用 |
| RAG 权限过滤 | 不改 `PermissionService.requireReadableKbIds(...)` 调用顺序 |
| RAG answer citation | 现场响应仍包含 sources；持久化 replay snapshot 明确降级 |
| 敏感字段不进 prompt | PASS，资源生成模型上下文不再包含 `teacher_note` |
| API/DB 合同 | 不改 DTO 和 schema；仅改变字段内容策略 |
| 新依赖 | 无 |

## 7. 验收标准

- 新写入 `kb_query_log.question` 不包含原始问题全文，并包含 hash/length。
- 新写入 `kb_query_log.responseJson` 不包含原始问题全文或完整 source excerpt。
- 新写入 `source_citation.excerpt` 不包含完整 excerpt。
- 新写入 `profileSnapshot` 不包含 `learnerId` 或 `teacher_note`，并包含 `profileRef`。
- `ResourceAgent` 模型请求参数中的 `profileSnapshot` 不包含 `teacher_note`。
- focused/adjacent backend tests 通过，或记录无法执行原因。
