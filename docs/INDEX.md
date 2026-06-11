# 文档索引

`docs/` 根目录只保留本索引。基线文档已按类别归入子目录。

## 文档语言

工作流文档（PRD / REQ / SPEC / PLAN / TASK / Context Pack / 证据 / 验收）**正文使用中文**。API 路径、JSON 字段、SQL、代码标识符可保留英文。

## 目录结构

```text
docs/
├── INDEX.md                    ← 本文件
│
├── [工作流 — 新功能]
│   ├── product/                PRD
│   ├── requirements/           REQ
│   ├── specs/                  SPEC（新功能）
│   ├── plans/                  PLAN
│   ├── tasks/                  TASK
│   ├── context/                Context Pack
│   ├── evidence/               证据
│   └── acceptance/             验收
│
├── [记忆 — AI 先读]
│   └── memory/
│
├── [基线文档 — 已实现系统现状]
│   ├── architecture/           架构总览、RAG、可观测性、漂移检查
│   ├── api/                    接口契约与参考
│   ├── data/                   数据模型与种子数据
│   ├── operations/             部署运维
│   ├── planning/               总体规划与待办
│   └── research/               参考优先级、GitHub 调研
│
├── [归档]
│   └── superpowers/            历史计划（只读）
│
└── [工作流支撑]
    ├── skills/, subagents/, security/, harness/, backlog/, ...
```

## 基线文档归类

### 架构 `docs/architecture/`

| 文件 | 内容 |
|---|---|
| [overview.md](architecture/overview.md) | 系统总览、技术栈、分层、模块边界 |
| [rag-architecture.md](architecture/rag-architecture.md) | RAG 索引/检索/引用/权限 |
| [observability.md](architecture/observability.md) | Trace、健康检查、模型日志 |
| [ARCHITECTURE_BASELINE.md](architecture/ARCHITECTURE_BASELINE.md) | 工作流架构基线 |
| [ARCHITECTURE_DRIFT_CHECK.md](architecture/ARCHITECTURE_DRIFT_CHECK.md) | 架构漂移检查清单 |

### API `docs/api/`

| 文件 | 内容 |
|---|---|
| [contract.md](api/contract.md) | 前后端接口约定、路由、SSE |
| [reference.md](api/reference.md) | 端点明细与示例 |

### 数据 `docs/data/`

| 文件 | 内容 |
|---|---|
| [model.md](data/model.md) | 表结构、实体关系 |
| [seed-data.md](data/seed-data.md) | 演示/测试种子数据 |

### 运维 `docs/operations/`

| 文件 | 内容 |
|---|---|
| [deployment.md](operations/deployment.md) | 本地启动、Docker、环境变量 |

### 规划 `docs/planning/`

| 文件 | 内容 |
|---|---|
| [system-design-and-development-plan.md](planning/system-design-and-development-plan.md) | 需求对齐、阶段计划、完成度 |
| [research-backed-architecture-roadmap.md](planning/research-backed-architecture-roadmap.md) | 论文/OSS 架构路线 |
| [backend-architecture-todolist.md](planning/backend-architecture-todolist.md) | 后端待办清单 |

### 研究 `docs/research/`

| 文件 | 内容 |
|---|---|
| [reference-priority.md](research/reference-priority.md) | 参考资料优先顺序 |
| [github-references/](research/github-references/) | GitHub 调研报告 |

## AI 阅读顺序

### 新功能

```text
1. AGENTS.md + memory/PROJECT_MEMORY.md
2. research/reference-priority.md
3. 按领域选读基线文档（architecture/、api/、data/）
4. 写 product/、specs/、plans/、tasks/（不覆盖基线文档）
5. 实现后更新 memory/
```

### 快速对照

| 我要做… | 先读 |
|---|---|
| 新 API | `api/contract.md` → `specs/SPEC-*.md` |
| RAG | `architecture/rag-architecture.md` |
| Agent/Trace | `architecture/observability.md` + `architecture/overview.md` |
| 数据库 | `data/model.md` |
| 部署 | `operations/deployment.md` + `data/seed-data.md` |
| 排优先级 | `planning/backend-architecture-todolist.md` |

## superpowers 归档

`docs/superpowers/` 为历史文档，新功能不写此处。详见 [superpowers/README.md](superpowers/README.md)。
