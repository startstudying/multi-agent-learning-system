# ACCEPT-20260621 Memory lifecycle governance MVP

## 1. Acceptance Verdict

Verdict: PASS

P2 MVP 已完成。该切片满足 `PLAN-20260621-memory-answer-quality-execution-readable.md` 中 P2 的最小落地目标：记忆可治理、可纠错、可清理，且不保存 raw question / full answer / hidden context。

## 2. 验收标准

| 标准 | 结果 | 证据 |
|---|---|---|
| V23 migration 扩展 session/message lifecycle columns | PASS | `SchemaConvergenceMigrationTest` 通过，`MysqlMigrationSmokeTest` latest version/count 更新到 V23/23 |
| QA 成功后写入低敏 memory summary | PASS | `AiQaService` 调用 `MemoryLifecycleService.recordQaTurn(...)`，`MemoryLifecycleServiceTest` 覆盖 |
| summary 不包含 raw question、完整 answer、teacher_note、provider key、profileSnapshot | PASS | `MemoryPrivacyPolicy.questionLogValue(...)` + `answerLength/sourcePolicy/verification/citationCount`；focused test 与 privacy grep 覆盖 |
| edit/delete owner-only | PASS | `findByIdAndLearnerIdAndDeletedAtIsNull(...)` 作为 service 边界，`AiQaMemoryController` 从当前用户取 owner |
| MemoryContext 排除 deleted/expired message | PASS | repository 查询限定 `DeletedAtIsNull` 与 `DecayAtAfter`，`MemoryContextServiceTest` 覆盖 active session memory 注入 |
| focused/adjacent/compile 通过 | PASS | Evidence 文件记录 26 + 7 tests 与 compile success |

## 3. 接受的边界

- 本切片不包含前端记忆治理 UI。
- 本切片不包含向量长期记忆、冲突合并或教师批量治理后台。
- 本切片未新增依赖。
- 本切片未运行外部 MySQL smoke；真实环境迁移仍建议在部署前显式执行 opt-in smoke。
