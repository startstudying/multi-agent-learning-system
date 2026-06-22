# TASK-20260621 QA streaming and trace workbench MVP

## 1. 目标

完成 P1 最小切片：新增 `/api/ai/qa/stream`，并让学生工作台生产/预发安全通道消费 AI QA stream 和展示 answer quality / verification 信息。

## 2. 范围

包含：

- `/api/ai/qa/stream`
- frontend `streamAiQa`
- 学生工作台 QA verification 展示
- focused backend/frontend tests
- evidence / acceptance / memory / changelog

不包含：

- DB schema
- 新依赖
- 真实模型 token streaming
- 教师/管理员独立质量看板
- 批量 eval runner
- P2 memory lifecycle

## 3. 允许修改文件

- `backend/src/main/java/com/learningos/aiqa/api/AiQaController.java`
- `backend/src/test/java/com/learningos/aiqa/api/AiQaControllerTest.java`
- `frontend/src/api/aiQa.ts`
- `frontend/src/types/api.ts`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/components/workspace/WorkspaceStream.vue`
- `frontend/src/components/learning/AiMessageBlock.vue`
- `frontend/src/App.spec.ts`
- P1 workflow docs / evidence / acceptance
- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`

## 4. 禁止修改文件

- `backend/pom.xml`
- `frontend/package.json`
- `backend/src/main/resources/db/**`
- AI provider config / secrets
- RAG parser/vector/index worker
- ResourceGeneration / ReviewGate 生产代码
- P2 memory lifecycle 生产模块

## 5. 验收标准

- `AiQaControllerTest` 覆盖 `/api/ai/qa/stream` status/token/done SSE。
- stream endpoint 使用当前用户 role facts。
- 前端 production/staging 请求 `/api/ai/qa/stream`，不是 `/api/rag/query/stream`。
- stream URL 不包含 question/kbIds。
- 学生工作台展示 `verification.verdict`、`gatePolicy` 和 QA quality flags。
- focused tests 和 build/compile 通过。

## 6. Evidence

已创建：

- `docs/evidence/EVIDENCE-20260621-qa-streaming-trace-workbench-mvp.md`

验证结果：

- RED backend：`mvn --% -Dtest=AiQaControllerTest test` 首次失败于 `/api/ai/qa/stream` 缺失。
- RED frontend：`pnpm test -- --run App.spec.ts` 首次失败于 production/staging 仍调用 `/api/rag/query/stream` 且没有 `qa-quality-summary`。
- GREEN backend：`mvn --% -Dtest=AiQaControllerTest test`，5 run, 0 failures, 0 errors。
- GREEN frontend：`pnpm test -- --run App.spec.ts`，34 passed。
- Compile：`mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile`，Build SUCCESS。
- Build：`pnpm build`，`vue-tsc -b && vite build` passed。
- Safety grep：未发现 `/api/ai/qa/stream?` 或 question/kbIds URL 拼接；敏感词命中仅为既有过滤规则或 Admin Provider 配置页。

## 7. Acceptance Verdict

已创建：

- `docs/acceptance/ACCEPT-20260621-qa-streaming-trace-workbench-mvp.md`

Verdict: PASS

P1 MVP 已完成：后端提供 AI QA stream，前端 production/staging 使用 AI QA stream，并展示 verification / quality summary。本任务未实现真实 provider token streaming、独立教师/管理员质量看板、批量 eval runner 或 P2 memory lifecycle。
