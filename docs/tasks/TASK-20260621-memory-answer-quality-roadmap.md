# TASK-20260621 记忆系统与 ChatGPT 级回答质量路线图

## 1. Goal

基于用户给出的 Agent 记忆系统框架与 Answer Quality System 框架，结合当前项目代码和已有能力，产出一套可落地的优化路线图，明确优先级、架构缺口、风险和下一步实施切片。

## 2. Scope

本任务只做调研、综合设计和文档沉淀。

包括：

- 阅读项目记忆、架构规则、相关代码和 subagent 报告。
- 归纳当前 AI QA / RAG / Trace / Model Gateway / Evaluation 能力。
- 生成 L-size workflow 文档。
- 更新 changelog 与 memory。

不包括：

- 修改后端/前端生产代码。
- 新增数据库迁移。
- 新增依赖。
- 启动服务或执行完整测试。

## 3. Allowed Files

- `docs/product/PRD-20260621-memory-answer-quality-roadmap.md`
- `docs/requirements/REQ-20260621-memory-answer-quality-roadmap.md`
- `docs/specs/SPEC-20260621-memory-answer-quality-roadmap.md`
- `docs/plans/PLAN-20260621-memory-answer-quality-roadmap.md`
- `docs/tasks/TASK-20260621-memory-answer-quality-roadmap.md`
- `docs/context/CONTEXT-20260621-memory-answer-quality-roadmap.md`
- `docs/evidence/EVIDENCE-20260621-memory-answer-quality-roadmap.md`
- `docs/acceptance/ACCEPT-20260621-memory-answer-quality-roadmap.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/DECISION_MEMORY.md`

## 4. Disallowed Files

- `backend/src/**`
- `frontend/src/**`
- `backend/src/main/resources/db/migration/**`
- `frontend/package.json`
- 任何配置密钥、环境变量或依赖锁文件。

## 5. Acceptance Criteria

- L-size 文档全部存在且为中文正文。
- 明确当前系统缺口：缺少统一 `QaRuntime`、`MemoryContextService`、`AnswerVerifier`、Eval gate。
- 明确 P0 优先级：先隐私治理，再上下文，再 QA runtime，再 verifier/eval。
- 记录 subagent 报告和用户附件作为证据。
- 不修改生产代码。

## 6. Execution Notes

当前根因不是“模型不够强”或“prompt 不够长”，而是上下文、记忆、检索、工具、验证和评测没有形成统一闭环。路线图应避免直接扩大长期记忆范围；必须先做 privacy guard 和 eval gate。

## 7. Verification

本轮为 docs-only，未运行后端/前端测试。验证方式是文件存在性、内容完整性和证据/验收记录。
