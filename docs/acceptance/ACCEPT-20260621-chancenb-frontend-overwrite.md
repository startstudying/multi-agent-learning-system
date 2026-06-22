# ACCEPT：覆盖迁移 chanceNB/mu 前端

## 验收结论

Verdict：PASS WITH KNOWN RISKS。

本次已完成用户确认的前端覆盖迁移，前端测试、生产构建和本地预览 HTTP 检查均通过。由于未安装 Playwright 浏览器二进制，本次没有截图级视觉证据；该限制已记录在 Evidence。

## 验收项

| 验收项 | 结果 | 证据 |
|---|---|---|
| 使用 `chanceNB/main` 前端覆盖当前项目 | PASS | `git checkout chanceNB/main -- frontend` 已执行 |
| 清理旧前端残留源码 | PASS | `frontend/src` 迁移源之外残留数为 `0` |
| 不修改后端 / DB / API 合同 | PASS | 本任务未执行后端改动；当前后端已有的脏改动为任务前存在 |
| 前端测试通过 | PASS | `pnpm test -- --run`：34 passed |
| 前端构建通过 | PASS | `pnpm build`：exit 0 |
| 本地预览可访问 | PASS | `http://127.0.0.1:4173/` 返回 200 |
| 浏览器截图视觉验证 | PARTIAL | Playwright 浏览器二进制缺失，未下载 |
| 无前端真实密钥泄漏 | PASS | 未匹配到常见真实 API key 形态 |

## 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 未修改后端。 |
| Frontend rules | PASS | API 调用仍通过 `frontend/src/api/*` 和共享请求封装；未新增前端 LLM provider 直连。 |
| Agent / RAG rules | PASS | 未修改 Agent/RAG 后端执行链。 |
| Security | PASS | 未新增依赖，未写入真实密钥。 |
| API / Database | PASS | 未修改 API 合同或数据库 schema。 |

## 剩余风险

- `chanceNB/main` 前端可能需要其对应后端的聊天会话接口，当前任务未迁移后端。
- 当前项目之前的学生 workspace / AI QA 前端功能已被覆盖，若需要恢复能力，需要以后续任务重新接入。
- 视觉截图验证可在安装 Playwright 浏览器后补跑。
