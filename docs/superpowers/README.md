# superpowers 历史文档（已归档）

## 状态：只读归档，不再新增

本目录来自早期 **Claude Code / superpowers** 工作流，与当前 **Codex + Cursor** 总工作流**并存但不冲突**——前提是：**新功能不写这里**。

## 与总工作流的区别

| | `docs/superpowers/`（旧） | `docs/plans/` `docs/specs/` 等（总工作流） |
|---|---|---|
| 来源 | superpowers 插件自动生成 | AGENTS.md + feature-development-workflow |
| 文档链 |  mostly Plan + Design Spec | PRD → REQ → SPEC → PLAN → TASK → Context Pack |
| Subagent | superpowers:executing-plans | Multi-Expert Subagent Gate（中和版） |
| 命名 | `YYYY-MM-DD-topic.md` | `PRD-YYYYMMDD-feature.md` 等 |
| 状态 | **归档，只读参考** | **新功能唯一入口** |

## 本目录内容

| 文件 | 类型 | 说明 |
|---|---|---|
| `plans/2026-06-04-learning-agent-initialization.md` | 历史 PLAN | 项目初始化 |
| `specs/2026-06-04-learning-agent-initialization-design.md` | 历史设计 SPEC | 初始化设计 |
| `plans/2026-06-04-spring-boot-rag-foundation.md` | 历史 PLAN | RAG 基础 |
| `plans/2026-06-05-frontend-ai-cockpit-ui.md` | 历史 PLAN | AI 驾驶舱 UI |
| `specs/2026-06-05-frontend-ai-cockpit-ui-design.md` | 历史设计 SPEC | 驾驶舱设计 |
| `plans/2026-06-05-soybean-frontend-uplift.md` | 历史 PLAN | 前端提升 |

## AI 规则

- **新需求** → 写 `docs/product/`、`docs/specs/`、`docs/plans/`、`docs/tasks/`，**不要**在 `superpowers/` 下新建文件。
- **查历史** → 可读本目录了解某功能当初怎么规划的。
- **有冲突** → 以总工作流文档 + `docs/memory/` 为准；superpowers 仅作参考。

详见 [`docs/INDEX.md`](../INDEX.md)。
