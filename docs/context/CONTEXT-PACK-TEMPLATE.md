# Context Pack - 功能名称

> 语言：正文使用中文。

## 当前任务

这次要做什么？

## 关联记忆

- `docs/memory/PROJECT_MEMORY.md`

## 关联文档

- PRD：
- REQ：
- SPEC：
- PLAN：
- TASK：

## 已选 Skills

- skill 1
- skill 2

## Subagent 计划

### 是否启用 Subagent

是 / 否

### 原因

为什么需要或不需要 Subagent？

### 任务复杂度

| 影响模块数 | 涉及 Agent/RAG | 涉及安全 |
|---|---|---|
| 1 / 2 / 3+ | 是 / 否 | 是 / 否 |

### 选中的专家

- 产品分析师
- 规格架构师
- 前端专家
- 后端专家
- Agent/RAG 专家
- 安全与质量
- 集成评审员

### 并行级别

- [ ] L1 — 仅并行分析（中大型任务默认）
- [ ] L2 — 并行设计
- [ ] L3 — worktree 并行实现（需无文件重叠）

### 执行模式

- 单 Codex（小任务默认）
- 仅并行分析
- 仅并行设计
- worktree 并行实现

### 文件归属

| 领域 | 负责专家 | 允许修改的文件 |
|---|---|---|
| 前端 | 前端专家 | frontend/src/... |
| 后端 | 后端专家 | backend/src/... |
| Agent/RAG | Agent/RAG 专家 | backend/src/main/java/.../agent |
| 评审 | 集成评审员 | 不直接改代码 |

## 关联代码区域

- backend/src/main/java/...
- frontend/src/...

## 允许修改的文件

- file 1
- file 2

## 禁止修改的文件

- file 1
- file 2

## 测试命令

```bash
cd backend && mvn test
cd frontend && pnpm build
```

## 任务边界

这次只允许完成哪个 TASK？
