# Retrospective - RAG 索引恢复调度与并发锁

## 1. Feature Summary

本切片为 RAG 文档索引任务补齐了服务层并发锁和后台恢复调度入口。`createPendingTask(...)` 现在基于 `kb_document` 行悲观锁执行 active task 去重；`IndexTaskRecoveryScheduler` 在启动后和固定间隔内调用既有超时恢复服务。

## 2. What Went Well

- 复用了既有 `recoverTimedOutRunningTasks(...)`，避免在 scheduler 中重复业务规则。
- 通过文档行锁覆盖“没有 task 可锁”时的并发创建窗口。
- 聚焦测试覆盖了并发、恢复、启动调度、定时调度和禁用配置。

## 3. What Didn't Go Well

- 用户指定最小命令最初被本切片外的 orchestrator 编译错误阻断；集成修复后已复测通过，并在 Evidence / Acceptance 中补齐 PASS 证据。
- 当前仓库目录没有 `.git` 元数据，无法用 `git status/diff` 精确审查并发改动，只能基于文件读取和 Maven 输出判断。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| 长任务 active 去重优先锁业务主表行，再查/建任务表记录 | Yes | 暂不新增；可并入未来 RAG 长任务治理 skill |
| Scheduler 只做有界触发并复用 Service 恢复规则 | Yes | 暂不新增 |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | 子 Worker 避免编辑 shared memory/changelog/todo | 保持，由主 Codex 汇总 |
| Testing | H2 聚焦测试覆盖应用层语义 | 后续 P3 用 MySQL 8 smoke 验证 `for update` 锁行为 |
| Documentation | 本切片独立 slug 文档完整 | 主 Codex 统一更新全局 memory/changelog |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 修复 `OrchestratorWorkflowResponse` 构造器不匹配导致的编译错误 | Orchestrator owner / 主 Codex | 非本切片 |
| 用 MySQL 8 验证文档行锁并发语义 | 后续 P3 worker | P3-1/P3-2 |
| 设计索引 worker heartbeat / 重新入队策略 | 后续 RAG worker | P3-2 |

## 7. Memory Updates Needed

- [ ] PROJECT_MEMORY.md（主 Codex 统一更新）
- [ ] Domain memory file（主 Codex 统一更新）
- [ ] SKILL_REGISTRY.md（本切片暂不新增 skill）
- [ ] ARCHITECTURE_BASELINE.md（无漂移，不需要更新）
