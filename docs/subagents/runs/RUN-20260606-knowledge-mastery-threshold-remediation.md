# Knowledge DAG 掌握度阈值补救优先 Subagent 集成记录

## 背景

本切片用于补齐 `docs/planning/backend-architecture-todolist.md` 中 P1-2 的掌握度阈值规则：低于 `0.6` 时优先补救前置知识。

## 启用方式

Use Subagents: Yes

Parallelism Level: L1 只读并行分析

Implementation Mode: 主 Codex 串行实现

## 子代理分工

| 子代理 | 任务 | 结果 |
|---|---|---|
| Noether | 核查路径规划算法边界 | 完成，只读，无文件修改 |
| Planck | 设计 RED 测试场景 | 完成，只读，无文件修改 |

## 采用的分析结果

- 保留 `COMPLETION_THRESHOLD = 0.80`，不要用 `0.6` 替换完成阈值。
- 新增独立 `REMEDIATION_THRESHOLD = 0.60`。
- 不新增 `REMEDIATION` 或 `NEED_REVIEW` 状态，补救节点仍使用 `ACTIVE`。
- “优先补救”在当前 DTO 下主要通过节点顺序和 `reasonSummary` 表达。
- 排序只能在不违反 `PREREQUISITE` 拓扑约束的同批可学习节点中调整。
- `RELATED` / `ADVANCED` 不能重新参与补救优先判断。

## 冲突处理

- 子代理建议测试中加入孤立低掌握度节点；最终已纳入 `LearningWorkflowControllerTest`，用于证明低掌握度但无下游依赖的节点不会抢占补救优先级。
- 子代理建议 reason 只断言关键短语，最终采用关键短语断言，避免完整文案脆弱。

## 收口

两个子代理均已关闭，未留下后台 agent。
