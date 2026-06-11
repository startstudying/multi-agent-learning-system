# ACCEPT-20260611 后端架构后续增强计划收口

## 验收范围

`docs/planning/backend-architecture-todolist.md` 后续增强收口、专家 subagent 并行评审、external smoke 实测化、MySQL smoke version 对齐。

## 验收结果

| 验收项 | 结果 | 证据 |
|---|---|---|
| 专家 subagent 并行评审落盘 | PASS | `docs/subagents/runs/RUN-20260611-backend-architecture-*.md` |
| Qdrant external smoke 不再是占位测试 | PASS | `QdrantVectorExternalSmokeTest` opt-in 连接 Qdrant 并检查 collection / dimension |
| Model Provider external smoke 不再是占位测试 | PASS | `ModelProviderExternalSmokeTest` opt-in 调用 OpenAI-compatible chat endpoint |
| MySQL migration smoke 对齐最新迁移 | PASS | `MysqlMigrationSmokeTest` latest version/count 更新为 V22 / 22，并断言 V21/V22 对象 |
| 默认测试不外连 | PASS | external smoke 和 MySQL smoke 默认 skipped |
| 相邻后端测试 | PASS | 56 run, 0 failures, 0 errors, 3 skipped |

## 限制

- 本机未配置真实 Qdrant、真实外部 model provider、MySQL smoke system property，因此这些 opt-in smoke 只验证了默认跳过和编译路径。
- 出站 URL allowlist、DashScope 专用 SDK、native/cloud OCR、分域 token budget gate 属于后续独立生产化任务。

## Verdict

PASS。计划后续增强收口达到当前任务边界。
