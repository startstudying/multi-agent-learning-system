# Context Pack: AI QA 无资料降级回答

## 相关记忆和文档

- `docs/memory/PROJECT_MEMORY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/specs/SPEC-20260611-ai-qa-unified-contract.md`
- `docs/specs/SPEC-20260611-ai-qa-no-source-fallback.md`

## 当前事实

- `/api/ai/qa` 由 `AiQaService` 包装 `RagQueryService`。
- `RagQueryService` no-source 时返回 `retrieval.noSource=true` 和 no-source answer。
- 学生端当前固定展示 no-source 安全拒答卡片，容易让降级回答看起来像失败。

## 选定技能

- feature-development-workflow
- educational-rag-pipeline
- model-gateway-boundary
- api-contract-design
- vue3-component-design
- test-generator

## Subagent Plan

不使用 subagent。该任务为 M 级但修改面集中，单 Codex 执行。

## 允许修改文件

见 `docs/tasks/TASK-20260611-ai-qa-no-source-fallback.md`。

## 禁止修改文件

见 `docs/tasks/TASK-20260611-ai-qa-no-source-fallback.md`。

## 测试命令

```bash
cd backend && mvn test -Dtest=AiQaControllerTest,RagQueryServiceTest
cd frontend && pnpm test -- --run
cd frontend && pnpm build
```

## 当前边界

只实现 AI QA 层的 no-source 降级和学生端展示。真实通用 LLM 调用、SSE 统一 QA、多 Agent 编排留给后续 roadmap slice。

验证阶段发现 `frontend/src/App.vue` 的 lucide 图标类型写法阻塞 `pnpm build`，允许做最小类型修复：使用 Vue `Component` 类型描述 shell 图标。
