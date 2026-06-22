# ACCEPT-20260621 QA streaming and trace workbench MVP

## 1. 验收结论

Verdict: PASS

P1 `QA streaming and trace workbench MVP` 已完成。后端提供 `/api/ai/qa/stream`，前端生产/预发安全流式通道消费 AI QA stream，并在学生工作台展示 QA verification / quality 信息。

## 2. 验收项

| 验收项 | 结论 | 证据 |
|---|---|---|
| `/api/ai/qa/stream` 返回 SSE | PASS | `AiQaControllerTest`: 5 run, 0 failures |
| stream 包含 `status` / `token` / `done` | PASS | SSE content 断言通过 |
| stream `done` 包含 `verification` 和 gate policy | PASS | content 包含 `verification` 与 `BASIC_QA_VERIFIER_V1` |
| 前端 production/staging 使用 `/api/ai/qa/stream` | PASS | `App.spec.ts`: 34 passed |
| question/kbIds 不进入 stream URL | PASS | URL 断言不包含 `question=` / `kbIds=` |
| 学生工作台展示 QA quality summary | PASS | `qa-quality-summary` 断言包含 `PASS`、`BASIC_QA_VERIFIER_V1`、`STRUCTURED_SCHEMA_V1` |
| 后端 compile 通过 | PASS | Build SUCCESS |
| 前端 build 通过 | PASS | `vue-tsc -b && vite build` passed |

## 3. 剩余风险

- 本 MVP 是 transport streaming，`token` 事件仍可一次发送完整答案；真实 provider token streaming 需要后续单独切片。
- 教师/管理员独立质量工作台尚未实现；当前质量展示在学生工作台主回答卡与右侧思考流中。
- QA stream 复用现有 `QaRuntime`，尚未为每个 QA runtime 步骤写入独立 durable `agent_trace`。
- P2 memory lifecycle governance 仍未完成。

## 4. 下一步

继续 P2 `Memory lifecycle governance`：补最小可治理的记忆生命周期能力，覆盖 salience、decay、用户可编辑/删除、session 表使用边界。
