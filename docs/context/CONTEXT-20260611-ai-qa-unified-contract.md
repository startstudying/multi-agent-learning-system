# CONTEXT - AI QA Slice 1 统一问答合同

## 1. Boundary

本上下文包只允许实现 V1-V3 统一非流式问答合同。

当前实现策略：

- 后端新增 `/api/ai/qa`。
- 复用现有 `RagQueryService`。
- 不新增模型 SDK、DB schema、外部工具或多 Agent runtime。
- 前端学生问答区从 RAG 专用调用迁移到统一 QA 调用，保留现有 stream 路线给 Slice 2。

## 2. Backend Contract

`AiQaRequest`:

- `question`
- `answerMode`
- `kbIds`
- `courseId`
- `topK`
- `requestId`

`AiQaResponse`:

- `answer`
- `answerMode`
- `reasoningEffort`
- `reasoningSummary`
- `sources`
- `traceId`
- `workflowId`
- `toolCalls`

## 3. Mode Policy

- `FAST -> low`
- `THINKING -> medium`
- `EXPERT -> high`
- blank/null -> `THINKING`
- invalid enum -> validation error

## 4. Security Notes

- `reasoningSummary` is a safe product summary, not raw reasoning.
- Frontend cannot pass provider/model/reasoning internals.
- `courseId` cannot bypass KB/course permission.
- No secrets or raw provider errors in docs/memory.

## 5. Allowed Files

See `TASK-20260611-ai-qa-unified-contract.md`.

## 6. Verification

Focused backend:

```powershell
cd backend; mvn test -Dtest=QaModePolicyTest,AiQaControllerTest
```

Adjacent backend:

```powershell
cd backend; mvn test -Dtest=ChatControllerTest,AiModelGatewayTest
```

Frontend:

```powershell
cd frontend; pnpm test -- App.spec.ts
cd frontend; pnpm build
```
