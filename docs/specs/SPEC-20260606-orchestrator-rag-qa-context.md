# Orchestrator RAG_QA 上下文收敛规格

## Payload

`CreateWorkflowRequest.payloadJson`：

```json
{
  "kbIds": ["kb_sql"],
  "question": "Why does SQL JOIN duplicate rows?",
  "topK": 5
}
```

校验规则：

- `kbIds` 必须是非空数组。
- `kbIds` 中空字符串会被过滤；过滤后为空则报 `VALIDATION_ERROR`。
- `question` 必须非空。
- `topK` 可选。

## Orchestrator 行为

1. 解析并校验 payload。
2. 创建 workflow envelope。
3. `AgentRunRecorder.startRun(...)` 创建 `agent_task`，初始状态 `RUNNING`。
4. 写入 `workflow_start` trace step。
5. 调用 `RagQueryService.queryWithTraceId(...)`，传入 Orchestrator traceId。
6. 根据 RAG 响应追加 trace steps：
   - `step_rag_safety`
   - `step_rag_retrieval`
   - `step_rag_answer`
7. 转换 Agent task 到 `DONE`。
8. 返回 workflow status context。

## RAG Service 行为

新增：

```java
RagQueryResponse queryWithTraceId(
    String userId,
    List<String> requestedKbIds,
    String question,
    Integer topK,
    String traceId
)
```

直接 `query(...)` 继续保留，内部仍从 `TraceContext` 或随机 id 获取 trace。

## Trace 规则

- `workflow_start.sequenceNo = 1`。
- RAG 子步骤从 2 开始追加。
- `kb_query_log.traceId`、`source_citation.traceId`、workflow response `traceId` 三者一致。
- no-source 是成功降级，不是失败。

## 失败规则

本轮最小切片先覆盖 payload validation 和成功/无来源路径。运行期权限或安全失败的 durable failed workflow 证据留给后续 retry/recovery 策略切片，与 P0-1 的失败策略细化一起处理。
