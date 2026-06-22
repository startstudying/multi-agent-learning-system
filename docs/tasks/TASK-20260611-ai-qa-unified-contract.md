# TASK - AI QA Slice 1 统一问答合同

## 1. Goal

实现 V1-V3 第一刀：统一问答 API、三档模式策略、安全 `reasoningSummary`，并在学生端问答区展示模式和摘要。

## 2. Scope

In scope:

- 新增 `POST /api/ai/qa`。
- 新增 `FAST` / `THINKING` / `EXPERT` 后端策略映射。
- 响应包含 `reasoningEffort` 和安全 `reasoningSummary`。
- 前端学生问答区可选择模式并调用统一 QA API。
- focused / adjacent tests。

Out of scope:

- 不实现统一 stream endpoint。
- 不新增 WebSearch / FileSearch / CodeAnalysis Tool。
- 不新增多 Agent 编排。
- 不新增 DB schema / dependency。
- 不实现真实 Responses API SDK 迁移。

## 3. Context Pack

### Related Memory and Docs

- `docs/memory/PROJECT_MEMORY.md`
- `docs/product/PRD-20260611-ai-qa-evolution-roadmap.md`
- `docs/specs/SPEC-20260611-ai-qa-evolution-roadmap.md`
- `docs/plans/PLAN-20260611-ai-qa-evolution-roadmap.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`

### Selected Skills

- feature-development-workflow
- spring-boot-architecture
- api-contract-design
- model-gateway-boundary
- agent-trace-governance
- security-review
- vue3-component-design
- ai-streaming-ui
- test-generator

### Subagent Plan

文档化 L1/L2 分析，报告：

- `docs/subagents/runs/RUN-20260611-ai-qa-unified-contract.md`

代码实现由单 Codex 完成。

### Files Allowed To Modify

- `backend/src/main/java/com/learningos/aiqa/api/AiQaController.java`
- `backend/src/main/java/com/learningos/aiqa/api/dto/AiQaDtos.java`
- `backend/src/main/java/com/learningos/aiqa/application/AiQaService.java`
- `backend/src/main/java/com/learningos/aiqa/application/QaModePolicy.java`
- `backend/src/test/java/com/learningos/aiqa/api/AiQaControllerTest.java`
- `backend/src/test/java/com/learningos/aiqa/application/QaModePolicyTest.java`
- `frontend/src/api/aiQa.ts`
- `frontend/src/types/api.ts`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/App.spec.ts`
- `docs/evidence/EVIDENCE-20260611-ai-qa-unified-contract.md`
- `docs/acceptance/ACCEPT-20260611-ai-qa-unified-contract.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`

### Files Not Allowed To Modify

- `backend/pom.xml`
- `frontend/package.json`
- `backend/src/main/resources/db/migration/**`
- `docs/superpowers/**`
- unrelated backend/frontend modules

### Test Commands

```powershell
cd backend; mvn test -Dtest=QaModePolicyTest,AiQaControllerTest
cd backend; mvn test -Dtest=ChatControllerTest,AiModelGatewayTest
cd frontend; pnpm test -- App.spec.ts
cd frontend; pnpm build
```

## 4. Acceptance Criteria

- [x] `POST /api/ai/qa` 存在并返回 `ApiResponse<AiQaResponse>`。
- [x] `FAST` / `THINKING` / `EXPERT` 映射为 `low` / `medium` / `high`。
- [x] 缺省模式为 `THINKING`。
- [x] 响应包含安全 `reasoningSummary`，不包含原始 CoT、prompt、key 或 provider 内部配置。
- [x] 前端能选择模式并把 `answerMode` 发送到统一 QA API。
- [x] 前端展示 `reasoningSummary`、`traceId` 和 citations。
- [x] Focused backend/frontend tests 通过，或限制被记录。
- [x] Changelog、Project Memory、Evidence、Acceptance 已更新。
