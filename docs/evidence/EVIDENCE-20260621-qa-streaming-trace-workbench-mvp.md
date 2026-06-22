# EVIDENCE-20260621 QA streaming and trace workbench MVP

## 1. 范围

本证据对应 P1 `QA streaming and trace workbench MVP`。

完成内容：

- 新增后端 `POST /api/ai/qa/stream`，使用 `text/event-stream` 输出 `status` / `token` / `done` / `error`。
- QA stream 复用 `AiQaService.answer(...)` 与 P0 `QaRuntime` / `AnswerVerifier`。
- 新增前端 `frontend/src/api/aiQa.ts`，通过 shared `streamRequest` 调用 `/api/ai/qa/stream`。
- 学生工作台 production/staging 安全流式通道切换到 AI QA stream，请求体携带 question/kbIds，不通过 URL 泄露。
- AI 回答卡片展示 QA quality summary：verification verdict、gate policy、source policy、uncertainty、mode/reasoning effort、tool call count、quality flags。

## 2. RED 证据

先写入 P1 后端和前端测试后执行：

```powershell
mvn --% -Dtest=AiQaControllerTest test
```

结果：失败，符合预期。

关键失败：

- `POST /api/ai/qa/stream` 尚不存在。
- MockMvc 命中静态资源处理器，返回 500 / `NoResourceFoundException`。

前端 RED：

```powershell
pnpm test -- --run App.spec.ts
```

结果：失败，符合预期。

关键失败：

- production/staging 仍调用 `/api/rag/query/stream`。
- 未调用 `/api/ai/qa/stream`。
- 未展示 `qa-quality-summary`。

## 3. GREEN 证据

后端 focused：

```powershell
mvn --% -Dtest=AiQaControllerTest test
```

结果：

- `AiQaControllerTest`: 5 run, 0 failures, 0 errors
- Build: SUCCESS

覆盖点：

- `/api/ai/qa` REST 合同仍通过。
- `/api/ai/qa/stream` 输出 `status`、`token`、`done`。
- stream 内容包含 `INTENT_ROUTING`、`VERIFYING`、answer、traceId、`verification`、`BASIC_QA_VERIFIER_V1`。
- stream 内容不包含 raw `chain-of-thought`。

前端 focused：

```powershell
pnpm test -- --run App.spec.ts
```

结果：

- `App.spec.ts`: 34 passed

覆盖点：

- production/staging 调用 `/api/ai/qa/stream`。
- stream URL 不包含 `question=` 或 `kbIds=`。
- 不回退到 legacy EventSource。
- UI 展示 QA answer、traceId、citation、verification verdict、gate policy 和 quality flags。

## 4. Build / Compile

后端 compile：

```powershell
mvn --% -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true compile
```

结果：

- Build: SUCCESS

前端 build：

```powershell
pnpm build
```

结果：

- `vue-tsc -b && vite build` 通过。
- Vite build 成功输出 `dist`。

## 5. 安全检查

命令：

```powershell
rg -n "apiKey|api_key|provider key|provider_key|sk-|chain-of-thought|rawPrompt|profileSnapshot|question=.*kbIds|/api/ai/qa/stream\\?" frontend/src backend/src/main/java/com/learningos/aiqa
```

结果说明：

- AI QA 生产代码中敏感词命中仅为 P0 memory/verifier 过滤规则。
- 前端命中 `apiKey` 的位置属于既有 Admin Model Provider 配置页，不是 P1 学生 QA stream。
- 未发现 `/api/ai/qa/stream?` 或 question/kbIds URL 拼接。

## 6. 非阻塞说明

- 本切片实现 transport streaming，不是真实模型 token streaming。
- 未新增 DB schema、依赖、真实 model reviewer、批量 eval runner、教师/管理员独立质量看板或 P2 memory lifecycle。
- Mockito dynamic agent warning 为现有测试运行环境提示，不影响测试结论。
