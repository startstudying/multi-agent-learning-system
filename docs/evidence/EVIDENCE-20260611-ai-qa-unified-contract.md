# EVIDENCE - AI QA Slice 1 统一问答合同

## 1. Scope

本切片实现 V1-V3：统一非流式问答 API、三档模式策略、安全 `reasoningSummary`、学生端模式选择与摘要展示。

## 2. Verification

### Backend RED

```powershell
cd backend; mvn test '-Dtest=QaModePolicyTest,AiQaControllerTest'
```

Observed expected RED:

- 首次失败于 `QaModePolicy` 不存在。
- 后续无效模式用例失败于未知 `answerMode` 未被拒绝。

### Backend Focused GREEN

```powershell
cd backend; mvn test '-Dtest=QaModePolicyTest,AiQaControllerTest'
```

Result:

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Backend Adjacent

```powershell
cd backend; mvn test '-Dtest=ChatControllerTest,AiModelGatewayTest'
```

Result:

```text
Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Frontend

```powershell
cd frontend; pnpm test -- App.spec.ts
cd frontend; pnpm build
```

Result:

```text
App.spec.ts: 31 passed
vue-tsc -b && vite build: passed
```

## 3. Acceptance Evidence

| Criteria | Verdict | Evidence |
|---|---|---|
| `POST /api/ai/qa` exists | PASS | `AiQaControllerTest` |
| Mode mapping `FAST/THINKING/EXPERT -> low/medium/high` | PASS | `QaModePolicyTest` |
| Default mode `THINKING` | PASS | `QaModePolicyTest`, `AiQaControllerTest` |
| Unknown mode rejected | PASS | `AiQaControllerTest` returns `VALIDATION_ERROR` |
| Safe `reasoningSummary` | PASS | `QaModePolicyTest`, API response assertions |
| Frontend sends selected `answerMode` | PASS | `App.spec.ts` |
| Frontend displays `reasoningSummary`, `traceId`, citations | PASS | `App.spec.ts` |
| No dependency / DB schema change | PASS | Diff review |

## 4. Limitations

- 本切片先实现后端产品策略合同和 UI 行为，不实现原生 OpenAI Responses API `reasoning.effort` 调用。
- 统一流式问答仍属于 Slice 2。
- 真实工具调用和多 Agent verifier 属于后续切片。
