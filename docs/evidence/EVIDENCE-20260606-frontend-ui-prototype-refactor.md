# 前端中低保真 UI 原型重构证据

## 范围

本次只修改前端页面、全局样式、前端测试和交付文档。未修改后端、数据库 migration、API client、路由、类型定义或依赖声明。

## 测试证据

### Focused RED

命令：

```bash
cd frontend
npm test -- --run src/App.spec.ts -t "Chinese medium-fidelity|role-aware shell context|renders the student learning loop"
```

结果：失败，原因符合预期：

- 缺少中文 `学生端 Learning Loop 工作台`。
- 缺少 `data-test="no-source-card"`。
- Shell context 仍是英文 `Active learning loop`。

### Focused GREEN

命令：

```bash
cd frontend
npm test -- --run src/App.spec.ts -t "Chinese medium-fidelity|role-aware shell context|renders the student learning loop|teacher review details|admin dependency health|admin copy|routes|prevents a second RAG stream"
```

结果：通过。

```text
Test Files  1 passed (1)
Tests  8 passed | 20 skipped (28)
```

### Full Frontend Test

命令：

```bash
cd frontend
npm test -- --run
```

结果：通过。本轮 SDD 收口后重新执行，exit 0。

```text
Test Files  1 passed (1)
Tests  28 passed (28)
Duration  5.30s
```

### Production Build

命令：

```bash
cd frontend
npm run build
```

结果：通过。本轮 SDD 收口后重新执行，exit 0。

```text
vue-tsc -b && vite build
✓ built
dist/assets/index-DwKfCmB2.css   23.38 kB │ gzip:  5.01 kB
dist/assets/index-DMg7pop5.js   144.35 kB │ gzip: 51.24 kB
```

## 安全和架构检查

命令：

```powershell
Select-String -Path 'D:\多元agent\frontend\src\**\*.*' -Pattern 'api.openai|openai.com|anthropic|dashscope|llm|VITE_.*KEY|API_KEY' -CaseSensitive:$false
```

结果：无匹配输出。

结论：

- 前端未直连 LLM provider。
- 前端未新增 API key 或密钥。
- API 调用仍通过现有 `frontend/src/api/client.ts`。
- Context Pack 明确禁止修改 `backend/**`、`frontend/src/api/**`、`frontend/src/router.ts`、`frontend/src/types/api.ts`、依赖声明和 `docs/planning/backend-architecture-todolist.md`。

## 浏览器视觉检查

使用本机 Chrome headless 对当前 Vite 前端进行截图检查。

命令摘要：

```powershell
chrome.exe --headless --window-size=1440,900 --screenshot=student-desktop.png http://127.0.0.1:5173/
chrome.exe --headless --window-size=1440,900 --screenshot=teacher-desktop.png http://127.0.0.1:5173/teacher/reviews
chrome.exe --headless --window-size=1440,900 --screenshot=admin-desktop.png http://127.0.0.1:5173/admin/operations
chrome.exe --headless --window-size=375,844 --screenshot=student-mobile-fixed.png http://127.0.0.1:5173/
```

截图文件：

- `frontend/target-ui-check/student-desktop.png`
- `frontend/target-ui-check/teacher-desktop.png`
- `frontend/target-ui-check/admin-desktop.png`
- `frontend/target-ui-check/student-mobile-fixed.png`

检查结论：

- 学生端桌面：Learning Loop、RAG 问答、Citation、no source、学习路径首屏可读。
- 教师端桌面：Review Queue、空队列、审核检查面板、禁用 Reject 目标态按钮可读。
- 管理员端桌面：KPI、服务状态、图表占位、异常告警、接口来源可读；后端未运行时进入 failed/loading 兜底状态，没有伪造数据。
- 学生端移动：Header 和主按钮已调整为移动短文案，无明显按钮文字截断。

## 已知说明

- `frontend/target-ui-check/` 为本次视觉检查输出目录，不是业务源码。
- 管理员运行时截图显示 health/analytics 失败或 loading，是因为只运行了前端 Vite，后端 `localhost:8080` 未提供真实响应；这是符合边界的失败态展示。
