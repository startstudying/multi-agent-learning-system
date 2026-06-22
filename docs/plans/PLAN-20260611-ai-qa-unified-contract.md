# PLAN - AI QA Slice 1 统一问答合同

## 1. Current State

- 已有 `POST /api/rag/query` 和 `POST /api/rag/query/stream`。
- 已有前端学生端 RAG 提问入口。
- 已有 `AiModelGateway` 和模型 provider registry，但当前 RAG 查询主要是检索式回答，不直接暴露统一 QA 模式。
- 父路线要求先做 V1-V3：普通问答 + `FAST/THINKING/EXPERT` + safe `reasoningSummary`。

## 2. Implementation Steps

1. RED：新增后端 `QaModePolicyTest`，固定 `answerMode -> reasoningEffort` 和默认模式。
2. RED：新增后端 `AiQaControllerTest`，固定 `POST /api/ai/qa` 响应合同、RAG service 复用、Bearer role facts、不暴露内部字段。
3. GREEN：新增 `aiqa` 包下 DTO、Policy、Service、Controller。
4. RED：新增/调整前端测试，固定模式选择发送 `answerMode`，响应展示 `reasoningSummary`。
5. GREEN：新增 `frontend/src/api/aiQa.ts`，修改 `StudentDashboard.vue` 和类型。
6. 运行 focused backend/frontend tests。
7. 运行 adjacent tests：RAG controller tests、frontend App tests。
8. 更新 Evidence / Acceptance / Changelog / Memory。

## 3. Risk Controls

- 不新增依赖。
- 不新增 DB migration。
- 不把 `gpt-5.5` 模型 slug、provider 参数或 `reasoning.effort` 作为前端可任意控制字段。
- 不返回原始 reasoning tokens。
- 不把 `courseId` 当成权限事实；权限仍由 RAG service 的 KB/course scope 检查执行。

## 4. Test Strategy

Backend focused:

```powershell
cd backend; mvn test -Dtest=QaModePolicyTest,AiQaControllerTest
```

Backend adjacent:

```powershell
cd backend; mvn test -Dtest=ChatControllerTest,AiModelGatewayTest
```

Frontend focused/adjacent:

```powershell
cd frontend; pnpm test -- App.spec.ts
cd frontend; pnpm build
```

Full backend/frontend tests can be run if focused tests reveal cross-module risk or after implementation stabilizes.

## 5. Integration Review

Expected integrated result:

- `/api/ai/qa` wraps existing RAG answer path.
- Frontend can select mode and see summary without knowing model internals.
- Existing RAG stream remains unchanged for Slice 2.
