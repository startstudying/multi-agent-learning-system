# Retrospective - RAG 索引任务超时恢复

## 1. Feature Summary

完成 P0-3 的 RAG 索引长任务恢复最小切片：`IndexService.recoverTimedOutRunningTasks(...)` 能将超时 `RUNNING` 任务标记为 `FAILED`，写入恢复证据，并让后续 reindex 创建新任务。

## 2. What Went Well

- subagent 并行审查把 P0-3/P0-4 和 P0-1 的下一步切片拆清楚，避免把多个大项混在一起。
- TDD 先暴露缺失方法和字段访问器，再做最小生产实现。
- 沿用现有表字段，无需新增迁移或公开 API。

## 3. What Didn't Go Well

- 新增 `@DataJpaTest` 一开始没有套 test profile，触发 H2 执行 MySQL Flyway 脚本失败。
- 当前只完成 Service 能力，还没有后台调度入口，P0-3 仍未完全闭环。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| RAG 长任务恢复服务切片 | No | 仍可由 `educational-rag-pipeline` 覆盖 |
| DataJpaTest 使用项目 test profile 和 ddl-auto | No | 作为测试约定写入 memory 即可 |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | 先做 subagent 审查再选切片 | 后续继续用于 P0-1/P0-3/P0-4 交叉项 |
| Testing | 新 DataJpaTest 初始配置不完整 | 后续新增 DataJpaTest 直接复制现有 test profile 配置 |
| Documentation | 本轮边界明确记录为 Service 恢复能力 | 下一轮单独设计后台调度，不把本轮误标为完整恢复闭环 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 接入后台定时恢复扫描或启动恢复 | Backend/RAG | 后续 P0-3 |
| 接入 `RAG_QA` Orchestrator workflow context | Backend/Orchestrator | 后续 P0-1 |
| 设计文档上传 `requestId` 幂等 | Backend/RAG | 后续 P0-3 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory file
- [x] SKILL_REGISTRY.md 不需要更新
- [x] ARCHITECTURE_BASELINE.md 不需要更新
