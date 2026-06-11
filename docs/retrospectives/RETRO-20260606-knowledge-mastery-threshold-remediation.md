# Retrospective - Knowledge DAG 掌握度阈值补救优先

## 1. Feature Summary

本切片在课程 DAG 路径规划中加入 `mastery < 0.6` 的补救优先策略，让低掌握度且会解锁下游知识的前置节点排在普通可学习节点前。

## 2. What Went Well

- RED 测试明确暴露当前排序只按创建顺序的问题。
- 子代理分别从算法和测试角度确认不应新增状态、不应改变 `0.80` 完成阈值。
- 实现只局限在 `LearningWorkflowService`，没有影响 Assessment 或 Replan。
- 相关回归同时覆盖了上一刀的依赖类型规则。

## 3. What Didn't Go Well

- 当前路径节点 DTO 表达力有限，只能通过排序和 `reasonSummary` 表达补救优先。
- 文档终端显示仍有乱码问题，但文件内容按 UTF-8 写入。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| DAG 分批拓扑排序 + 同批策略排序 | No | 等路径节点字段扩展后再沉淀 |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Testing | 通过 API 集成测试验证节点顺序和 reason | 保持，路径规划对端到端行为敏感 |
| Subagent | 子代理只读分析，完成后关闭 | 保持 |
| Scope | 不新增状态或字段 | 保持，后续字段扩展单独切片 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 扩展路径节点推荐原因、预计时长、资源类型和测评绑定 | 后续 | P1-2 |
| 设计 `recommendedNext` 或更明确的下一步推荐字段 | 后续 | 路径节点扩展 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [ ] API_MEMORY.md
- [ ] SKILL_REGISTRY.md
