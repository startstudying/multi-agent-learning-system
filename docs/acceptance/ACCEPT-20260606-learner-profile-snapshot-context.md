# Learner Profile 维度与快照验收报告

## 1. 验收清单

### 功能验收

- [x] Profile extraction 返回七个明确画像维度。
- [x] 每个 `ProfileDimension` 包含 `confidence`、`sourceType`、`lastEvidenceId`。
- [x] `structuredProfile` 返回 `recent_error_pattern` 和 `teacher_note`。
- [x] `POST /api/learning-paths` 返回 `profileSnapshot`。
- [x] `GET /api/learning-paths/{pathId}` 返回创建时保存的同一份 `profileSnapshot`。
- [x] `POST /api/resources/generation-tasks` 返回 `profileSnapshot`。
- [x] `GET /api/resources/generation-tasks/{taskId}` 返回任务保存的 `profileSnapshot`。
- [x] 资源生成 requestId replay 不重算或覆盖 `profileSnapshot`。
- [x] 历史画像字段别名和 `weakPointsJson` / `preferencesJson` 不会在快照中丢失。

### 数据验收

- [x] V10 migration 为 `learning_path` 增加 `profile_snapshot`。
- [x] V10 migration 为 `resource_generation_task` 增加 `profile_snapshot`。
- [x] `LearningPath` 实体包含 `profileSnapshot`。
- [x] `ResourceGenerationTask` 实体包含 `profileSnapshot`。

### 文档验收

- [x] PRD / REQ / SPEC / PLAN / TASK / CONTEXT 已创建。
- [x] Evidence / Acceptance / Subagent run 已更新。
- [x] Changelog / Memory / TODO 已更新。

## 2. 测试摘要

| 测试项 | 结果 |
|---|---|
| RED：快照字段缺失 | PASS，失败符合预期 |
| RED：历史字段别名和 profile 列 fallback 缺失 | PASS，失败符合预期 |
| GREEN：P1-1 主链路 | PASS，20 tests |
| 回归：Learning + Resource + Migration + Orchestrator | PASS，49 tests |

## 3. 验收结论

- [x] 通过
- [ ] 有条件通过
- [ ] 不通过

## 4. 遗留问题

- 后续可将 `profileSnapshot` 拆为结构化审计表，便于学习分析聚合。
- 教师端画像标注仍需要真实 UI 和权限模型支撑。
