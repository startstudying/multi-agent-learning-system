# PRD：覆盖迁移 chanceNB/mu 前端

## 背景

用户希望当前项目采用 `chanceNB/mu.git` 的前端，并已明确允许覆盖当前前端实现。当前仓库已有学生、教师、管理员三端工作区和 AI QA 前端改造，但本次目标不是继续融合现有视觉，而是以 `chanceNB/main` 的前端为准完成覆盖迁移。

## 目标

- 将 `chanceNB/main` 中的 `frontend` 前端体验迁移到当前项目。
- 保留当前项目后端、数据库迁移、API 服务和 AI/RAG/Agent 架构不变。
- 使用 `chanceNB/main` 的应用壳层、路由、页面、组件、样式和前端测试作为迁移基准。
- 覆盖后前端应能通过现有前端测试与构建命令，或明确记录失败原因。

## 非目标

- 不迁移 `chanceNB/main` 的后端 RAG 会话绑定代码。
- 不修改后端 REST API、DTO、数据库 schema 或 Spring Boot 配置。
- 不新增 npm 依赖。
- 不处理 Clash for Windows 本地代理配置；截图仅说明当前网络环境可能影响 GitHub fetch。
- 不保留当前本地未提交的学生工作区、AI QA 前端扩展或三端 workspace 分离逻辑，除非 `chanceNB/main` 自身包含。

## 用户价值

- 快速把目标仓库的前端形态落入当前项目，避免继续在当前前端分支上做局部修补。
- 保持后端能力不被前端迁移扰动，降低数据库/API 风险。
- 覆盖迁移后形成清晰证据，便于后续再决定是否把当前项目的 AI QA 能力重新接回新前端。

## 验收口径

- 当前 `frontend/src` 主要实现与 `chanceNB/main` 的前端一致。
- `frontend/package.json`、`frontend/vite.config.ts` 等前端入口配置按 `chanceNB/main` 覆盖。
- 前端测试和构建已运行并记录结果。
- 文档记录本次覆盖带来的功能取舍和后续风险。
