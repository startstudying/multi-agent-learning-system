# TASK-20260621 wiki-rag GitHub 项目参考调研

## 1. Goal

调研 `moodlehq/wiki-rag` GitHub 项目，判断它对当前 AI 学习系统的 RAG、记忆系统、回答质量路线图有什么可借鉴之处。

## 2. Task Type

Docs / GitHub reference research。

## 3. Skill Selection

| Skill | Why |
|---|---|
| `feature-development-workflow` | 项目要求外部参考调研也要有任务边界、证据和验收 |
| `analyze` | 需要区分仓库直接证据与对本项目的推断 |
| `rag-project-review` | 该仓库核心是 RAG ingestion、retrieval、context optimisation |
| `agent-trace-design` | 该仓库涉及 LangGraph、OpenAI-compatible wrapper、MCP 和观测 |
| `security-review` | 需要评估 auth、secrets、MCP/resource 暴露和生产可用风险 |

Missing skills: 无阻塞。

GitHub research needed: Yes。用户明确要求查看 GitHub 项目。

## 4. Size Classification

Size: S

Reason:

- 只新增调研报告与任务记录。
- 不修改生产代码、API、DB schema、依赖或部署。
- 风险集中在解释和后续建议，不触发实现。

Required Documents:

- 本 TASK，内嵌 Context Pack / Evidence / Acceptance。
- `docs/research/github-references/GITHUB-20260621-wiki-rag-reference.md`

Can Skip:

- PRD / REQ / SPEC / PLAN / standalone Evidence / standalone Acceptance。

Upgrade Trigger:

- 如果后续要实现 POC Context Builder、HyDE、MCP 或检索改造，需要重新开 M/L slice。

## 5. Context Pack

Related docs:

- `docs/specs/SPEC-20260621-memory-answer-quality-roadmap.md`
- `docs/plans/PLAN-20260621-memory-answer-quality-execution-readable.md`
- `docs/research/REPORT-20260621-memory-answer-quality-beginner-notes.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/PROJECT_MEMORY.md`

Sources:

- `https://github.com/moodlehq/wiki-rag`
- `https://raw.githubusercontent.com/moodlehq/wiki-rag/main/README.md`
- `https://raw.githubusercontent.com/moodlehq/wiki-rag/main/pyproject.toml`
- `https://raw.githubusercontent.com/moodlehq/wiki-rag/main/config.yml.template`
- `https://raw.githubusercontent.com/moodlehq/wiki-rag/main/wiki_rag/load/util.py`
- `https://raw.githubusercontent.com/moodlehq/wiki-rag/main/wiki_rag/index/util.py`
- `https://raw.githubusercontent.com/moodlehq/wiki-rag/main/wiki_rag/search/util.py`
- `https://raw.githubusercontent.com/moodlehq/wiki-rag/main/wiki_rag/vector/milvus.py`
- `https://raw.githubusercontent.com/moodlehq/wiki-rag/main/wiki_rag/server/server.py`
- `https://raw.githubusercontent.com/moodlehq/wiki-rag/main/wiki_rag/server/util.py`
- `https://raw.githubusercontent.com/moodlehq/wiki-rag/main/wiki_rag/mcp_server/server.py`
- `https://github.com/moodlehq/wiki-rag/releases`
- `https://github.com/moodlehq/wiki-rag/security`

Allowed files:

- `docs/research/github-references/GITHUB-20260621-wiki-rag-reference.md`
- `docs/tasks/TASK-20260621-wiki-rag-reference-review.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/PROJECT_MEMORY.md`

Disallowed files:

- `backend/src/**`
- `frontend/src/**`
- DB migration files
- dependency / env / lock files

Test commands:

- Docs-only，不运行后端/前端测试。
- 验证文件存在、报告包含关键结论、git 状态范围符合预期。

## 6. Evidence

- GitHub web/raw 页面可访问。
- 本地 `git clone` 因当前环境 GitHub 443 代理连接失败，改用 web/raw GitHub 页面读取。
- 已读取 README、pyproject、config template、loader、index、search、vector、server、MCP、release、security 页面。
- 创建参考报告：`docs/research/github-references/GITHUB-20260621-wiki-rag-reference.md`。

## 7. Acceptance

| Criteria | Verdict |
|---|---|
| 找到并确认目标仓库 | PASS |
| 分析核心架构与实现链路 | PASS |
| 说明对本项目可借鉴点 | PASS |
| 说明不建议照搬点 | PASS |
| 记录来源链接 | PASS |
| 未修改生产代码 | PASS |

Acceptance Verdict: PASS
