# SPEC - AI QA Slice 1 统一问答合同

## 1. Architecture

目标结构：

```text
StudentDashboard
-> frontend/src/api/aiQa.ts
-> POST /api/ai/qa
-> AiQaController
-> AiQaService
-> QaModePolicy
-> RagQueryService
-> ApiResponse<AiQaResponse>
```

本切片不让前端直接访问 LLM、provider、API key、prompt 或工具。后端复用现有 `RagQueryService` 获取答案、引用和 `traceId`。

## 2. API Contract

### Endpoint

```text
POST /api/ai/qa
Content-Type: application/json
```

### Request

```json
{
  "question": "Why does SQL JOIN duplicate rows?",
  "answerMode": "THINKING",
  "kbIds": ["kb_java_backend"],
  "courseId": "course_java_backend",
  "topK": 5,
  "requestId": "req_ai_qa_001"
}
```

字段规则：

| Field | Rule |
|---|---|
| `question` | 必填，非空。 |
| `answerMode` | 可选，`FAST` / `THINKING` / `EXPERT`，默认 `THINKING`。 |
| `kbIds` | 当前 Slice 必填且非空，用于复用课程 RAG。 |
| `courseId` | 可选，当前只透传为未来审计字段，不参与权限绕过。 |
| `topK` | 可选，沿用 RAG 查询限制。 |
| `requestId` | 可选，存在时复用 RAG query replay。 |

### Response

```json
{
  "answer": "JOIN duplicates come from one-to-many matches...",
  "answerMode": "THINKING",
  "reasoningEffort": "medium",
  "reasoningSummary": "我先按课程资料检索 JOIN 基数问题，再基于引用整理答案。",
  "sources": [],
  "traceId": "trc_qa_001",
  "workflowId": null,
  "toolCalls": []
}
```

## 3. Mode Policy

`QaModePolicy` 负责模式映射：

| answerMode | reasoningEffort | summary behavior | current execution |
|---|---|---|---|
| `FAST` | `low` | 简短说明“快速检索并回答” | RAG topK 可较小，若调用方传 topK 则尊重现有 RAG 限制。 |
| `THINKING` | `medium` | 说明检索、组织和来源检查 | 默认模式。 |
| `EXPERT` | `high` | 说明更完整检索和引用核对 | 当前不新增自检模型调用；为后续 verifier 预留合同。 |

`xhigh` 不作为前端枚举。后续如启用，只能经后台配置和成本门禁。

## 4. Reasoning Summary Safety

`reasoningSummary` 必须由后端确定性生成或从受控模型摘要二次过滤。本切片先用确定性安全摘要：

- 不展示原始 chain-of-thought。
- 不展示 system/developer prompt。
- 不展示工具原始输入/输出。
- 不展示私有文档全文。
- 不展示密钥、provider 配置、内部评分细节。

## 5. Backend Files

允许新增：

- `backend/src/main/java/com/learningos/aiqa/api/AiQaController.java`
- `backend/src/main/java/com/learningos/aiqa/api/dto/AiQaDtos.java`
- `backend/src/main/java/com/learningos/aiqa/application/AiQaService.java`
- `backend/src/main/java/com/learningos/aiqa/application/QaModePolicy.java`
- `backend/src/test/java/com/learningos/aiqa/api/AiQaControllerTest.java`
- `backend/src/test/java/com/learningos/aiqa/application/QaModePolicyTest.java`

允许修改：

- 如测试配置需要，可最小调整现有测试辅助代码。

## 6. Frontend Files

允许新增：

- `frontend/src/api/aiQa.ts`

允许修改：

- `frontend/src/types/api.ts`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/App.spec.ts`

## 7. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS planned | Controller -> Service -> RagQueryService。 |
| Frontend rules | PASS planned | 只通过 API wrapper 调后端。 |
| Agent / RAG rules | PASS planned | 复用 RAG citations 和 permission filtering。 |
| Security | PASS planned | 不暴露 CoT、prompt、key、任意模型参数。 |
| API / Database | PASS planned | 新增 API 合同，无 DB schema。 |
