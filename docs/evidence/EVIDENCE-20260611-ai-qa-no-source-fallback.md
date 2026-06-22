# AI QA 无资料降级回答证据

## 任务

`TASK-20260611-ai-qa-no-source-fallback.md`

## 变更摘要

- `/api/ai/qa` 响应新增 `sourceStatus` 和 `sourcePolicy`。
- AI QA 层先执行 RAG；当 RAG 无可靠来源时，返回 `GENERAL_FALLBACK / NO_COURSE_SOURCE_FALLBACK` 和通用回答，不再把严格 RAG 拒答直接展示成学生答案。
- 有课程引用时返回 `COURSE_GROUNDED / COURSE_RAG`，保留 sources 和 traceId。
- 学生端读取来源状态并展示课程资料回答或通用回答。
- 纯 RAG no-source 规则保持不变。

## 验证命令

```bash
cd backend && mvn test "-Dtest=AiQaControllerTest,QaModePolicyTest,RagQueryServiceTest"
```

结果：通过，`Tests run: 29, Failures: 0, Errors: 0, Skipped: 0`。

```bash
cd frontend && pnpm test -- --run
```

结果：通过，`33 passed`。

```bash
cd frontend && pnpm build
```

结果：通过，`vue-tsc -b && vite build` 成功。

## 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 仍委托 `AiQaService`；降级策略在 Service 层。 |
| Frontend rules | PASS | 前端仍通过 shared API client 调后端，无 LLM 直连。 |
| Agent / RAG rules | PASS | 纯 RAG no-source 不变；AI QA 降级明确标识无课程来源且不伪造引用。 |
| Security | PASS | 无新依赖、无密钥、无 prompt 权限控制。 |
| API / Database | PASS | 仅新增兼容响应字段，无 DB 变更。 |

## 注意事项

- 通用回答目前是后端规则化占位策略，后续可接入 model gateway 的真实普通问答能力。
- 工作区存在本任务开始前的其他未提交改动，本证据只覆盖本任务相关验证。
