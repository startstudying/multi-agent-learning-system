# AI QA 无资料降级回答任务

## 目标

让课程问答遵循“有 RAG 资料就按 RAG 工作流，没有可靠资料就按普通 ChatGPT Web 风格回答”的逻辑，并在 UI 中标明来源状态。

## 任务类型

AI QA / RAG 问答降级行为修复。

## 允许修改文件

- `backend/src/main/java/com/learningos/aiqa/api/dto/AiQaDtos.java`
- `backend/src/main/java/com/learningos/aiqa/application/AiQaService.java`
- `backend/src/main/java/com/learningos/aiqa/application/QaModePolicy.java`
- `backend/src/test/java/com/learningos/aiqa/api/AiQaControllerTest.java`
- `frontend/src/types/api.ts`
- `frontend/src/App.vue`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/App.spec.ts`
- `docs/product/PRD-20260611-ai-qa-no-source-fallback.md`
- `docs/requirements/REQ-20260611-ai-qa-no-source-fallback.md`
- `docs/specs/SPEC-20260611-ai-qa-no-source-fallback.md`
- `docs/plans/PLAN-20260611-ai-qa-no-source-fallback.md`
- `docs/tasks/TASK-20260611-ai-qa-no-source-fallback.md`
- `docs/context/CONTEXT-20260611-ai-qa-no-source-fallback.md`
- `docs/evidence/EVIDENCE-20260611-ai-qa-no-source-fallback.md`
- `docs/acceptance/ACCEPT-20260611-ai-qa-no-source-fallback.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`

## 禁止修改文件

- RAG 检索、chunk、reranker、permission service。
- 数据库 migration。
- 依赖配置文件。
- 纯 RAG API 合同。

## 测试命令

```bash
cd backend && mvn test -Dtest=AiQaControllerTest,RagQueryServiceTest
cd frontend && pnpm test -- --run
cd frontend && pnpm build
```

## 验收标准

- AI QA no-source 响应返回 `GENERAL_FALLBACK` 和 `NO_COURSE_SOURCE_FALLBACK`。
- AI QA no-source 响应不展示为 ERROR，不展示安全拒答。
- AI QA 有来源响应仍返回 `COURSE_GROUNDED` 和引用。
- RAG no-source 测试仍保持 `NO_SOURCE_REFUSAL`。
- 前端生产构建通过；如遇现存类型门禁，只允许做与构建阻塞直接相关的最小修复。
