# EVIDENCE：覆盖迁移 chanceNB/mu 前端

## 执行摘要

本次已按用户确认的“允许覆盖”策略，将 `chanceNB/main` 的前端覆盖迁移到当前项目。迁移限制在 `frontend/` 与本任务文档范围内，未修改后端、数据库或 API 合同。

## 迁移源

- 仓库：`https://github.com/chanceNB/mu.git`
- 本地引用：`chanceNB/main`
- 提交：`20bc8aacb0d8e368df97c8d251adc704713bb0fe`

## 实施证据

- 执行了 `git checkout chanceNB/main -- frontend`，覆盖当前项目 `frontend`。
- 清理了 `frontend/src` 中不属于 `chanceNB/main` 的 13 个旧前端残留文件。
- 清理后校验：`frontend/src` 中迁移源之外的源码残留数为 `0`。
- `frontend/package.json` 回到迁移源脚本集合，不再保留当前工作区的 `dev:student` / `dev:teacher` / `dev:admin` 三端脚本。

## 测试证据

命令：

```powershell
cd frontend
pnpm test -- --run
```

结果：

```text
Test Files  1 passed (1)
Tests       34 passed (34)
Exit code   0
```

## 构建证据

命令：

```powershell
cd frontend
pnpm build
```

结果：

```text
vue-tsc -b && vite build
vite v8.0.16 building client environment for production...
1833 modules transformed.
dist/index.html                 0.48 kB
dist/assets/index-Be1_MFRx.css  88.17 kB
dist/assets/index-BF2X9Y6I.js   189.81 kB
Exit code 0
```

## 本地预览证据

- 启动了 `pnpm preview -- --host 127.0.0.1 --port 4173 --strictPort`。
- 本地 URL：`http://127.0.0.1:4173/`
- HTTP 检查：`StatusCode 200`，`RawContentLength 485`。
- 监听进程：`node.exe`，PID `22004`。

## 视觉检查限制

尝试用 Playwright 打开本地预览时，Playwright 包存在但浏览器二进制缺失：

```text
Executable doesn't exist at ... chromium_headless_shell...
Please run: npx playwright install
```

本任务未临时下载浏览器二进制，因此没有生成浏览器截图证据。当前视觉/运行证据以测试、构建和本地预览 HTTP 200 为准。

## 安全与架构检查

- 未新增 npm 依赖。
- 未修改 `backend/**`。
- `frontend/src` 中未匹配到真实 API key 形态，例如 `sk-*`、`AIza*`、Slack token 等。
- `frontend/src/api/modelProviders.ts` 包含 OpenAI / DashScope 等 provider preset URL，但这些 URL 通过 `/api/admin/model-providers` 后端管理接口提交，不是前端直接调用模型服务。
- 前端 API 调用仍集中在 `frontend/src/api/*`，共享 `client.ts` 负责请求封装和 SSE。

## 已知风险

- 当前项目原有三端 workspace 分离和 AI QA 前端整合被前端覆盖取代。
- 新前端包含 `/api/chat/sessions/{sessionId}/stream` 等聊天会话入口；本任务未迁移 `chanceNB/main` 后端会话接口，后续联调时可能需要补后端。
- 工作区中仍存在本任务之前的非前端未提交改动，不属于本次迁移清理范围。
