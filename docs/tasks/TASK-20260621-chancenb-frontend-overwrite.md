# TASK：覆盖迁移 chanceNB/mu 前端

## 目标

把 `chanceNB/main` 的前端覆盖迁移到当前项目，保留当前后端和架构边界不变。

## 当前状态

- 已抓取 `chanceNB/main`，提交为 `20bc8aacb0d8e368df97c8d251adc704713bb0fe`。
- 当前工作区有大量未提交改动。
- 用户已明确允许覆盖前端。

## 范围

包括：

- `frontend/src`
- `frontend/package.json`
- `frontend/vite.config.ts`
- 前端测试与类型文件
- 本次任务文档、证据、验收、memory、changelog

不包括：

- 后端 RAG 会话 API 迁移
- 数据库迁移
- 新 npm 依赖
- 当前非前端未提交改动清理

## Context Pack

相关记忆和文档：

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`

选用技能：

- `feature-development-workflow`
- `frontend-design`
- `Confidence Check`
- `using-git-worktrees`
- `test-driven-development`
- `verification-before-completion`

Subagent plan：

- 不启用实际子代理；本次仅前端模块覆盖，单 Codex 执行并记录集成审查。

允许修改文件：

- `frontend/**`
- 本任务相关 `docs/product|requirements|specs|plans|tasks|context|evidence|acceptance/**`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`

禁止修改文件：

- `backend/**`
- `docs/api/**`
- `docs/data/**`
- `docs/architecture/**`
- secrets / credential / private key files

测试命令：

- `cd frontend && pnpm test -- --run`
- `cd frontend && pnpm build`

任务边界：

- 本任务只完成前端覆盖迁移和验证记录。
- 如果覆盖后的 UI 调用了当前后端不存在的接口，只记录为后续集成风险，不在本任务中改后端。

## 验收标准

- 前端文件已按 `chanceNB/main` 覆盖。
- 旧前端残留源文件已清理到不影响迁移源前端。
- 测试和构建已运行并记录。
- Evidence 和 Acceptance 已创建。

## Evidence

- 覆盖源：`chanceNB/main`，提交 `20bc8aacb0d8e368df97c8d251adc704713bb0fe`。
- 已执行 `git checkout chanceNB/main -- frontend`。
- 已删除 `frontend/src` 内 13 个不属于迁移源的旧前端残留文件。
- 校验结果：`frontend/src` 迁移源之外残留数为 `0`。
- 测试：`cd frontend && pnpm test -- --run`，`34 passed`，exit code `0`。
- 构建：`cd frontend && pnpm build`，exit code `0`。
- 预览：`http://127.0.0.1:4173/` 返回 HTTP `200`。
- Playwright 视觉检查受限：本地缺少 chromium headless shell，未临时下载浏览器二进制。

## Acceptance Verdict

PASS WITH KNOWN RISKS。

已完成用户确认的前端覆盖迁移。剩余风险是 `chanceNB/main` 前端可能引用当前后端尚未迁移的聊天会话接口，以及截图级视觉验证尚未补跑。
