# RUN - AI QA Slice 1 统一问答合同专家分析

## 1. Backend Expert

- 建议新增独立 `com.learningos.aiqa` 包，避免污染 `rag` 模块。
- `AiQaController` 只处理 HTTP、当前用户和 response envelope。
- `AiQaService` 复用 `RagQueryService`，不直接访问 Repository。
- `QaModePolicy` 独立测试，固定 `answerMode -> reasoningEffort`。

## 2. Frontend Expert

- 新增 `frontend/src/api/aiQa.ts`，保持 API wrapper 模式。
- 学生问答区增加 segmented/radio 风格模式选择。
- 响应后展示 `reasoningSummary`，保留 citations 和 traceId。
- 不改整体 cockpit 结构，避免扩大 UI 风险。

## 3. Security & Quality

- 禁止前端发送模型 slug、provider、API key、prompt、raw effort。
- `reasoningSummary` 使用确定性安全摘要，不来自 raw chain-of-thought。
- `EXPERT` 当前只代表策略合同，不启动额外无限自检。
- 不新增 DB / dependency，降低回归面。

## 4. Integration Reviewer

结论：可执行。实现边界清晰，符合父路线 Slice 1。若后续需要 Responses API 原生 effort/summary，应单开模型网关增强任务。
