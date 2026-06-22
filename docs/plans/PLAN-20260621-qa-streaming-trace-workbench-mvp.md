# PLAN-20260621 QA streaming and trace workbench MVP

## 1. 执行顺序

1. 写后端 RED：`AiQaControllerTest` 覆盖 `/api/ai/qa/stream`。
2. 写前端 RED：`App.spec.ts` 覆盖 production/staging 使用 QA stream 并展示 verification。
3. 后端实现：
   - `AiQaController` 增加 stream endpoint。
   - 复用 `AiQaService.answer(...)`。
   - 发送 status/token/done/error SSE。
4. 前端实现：
   - 新增 `api/aiQa.ts`。
   - 扩展 `types/api.ts`。
   - `StudentDashboard` production/staging 改用 `streamAiQa`。
   - `WorkspaceStream` / `AiMessageBlock` 展示 QA quality summary。
5. 验证：
   - backend focused test
   - frontend focused test
   - backend compile
   - frontend build
6. 写 Evidence / Acceptance。
7. 更新总计划、changelog、memory。

## 2. 风险与处理

| 风险 | 处理 |
|---|---|
| 把 true model token streaming 做进本切片 | 明确限制为 transport streaming，后续单独做 model gateway 流式 |
| 前端泄露 question/kbIds 到 URL | production/staging 使用 POST stream request body |
| QA stream error 泄露异常 | 后端固定 `AI_QA_STREAM_FAILED` |
| 现有 RAG dev tests 被破坏 | dev legacy EventSource/RAG REST fallback 保留 |
| UI 信息过密 | 只展示 verdict / gate policy / source policy / uncertainty / flags |

## 3. 测试命令

```powershell
cd backend
mvn --% -Dtest=AiQaControllerTest test
mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile
```

```powershell
cd frontend
pnpm test -- --run App.spec.ts
pnpm build
```

## 4. 完成判定

- 后端 stream endpoint 可用且测试通过。
- 前端 production/staging 使用 QA stream，URL 不含 question/kbIds。
- UI 展示 verification。
- 证据、验收、memory、changelog 已更新。
