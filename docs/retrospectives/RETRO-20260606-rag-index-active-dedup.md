# Retrospective - RAG 索引任务 Active 去重

## 1. Feature Summary

完成 P0-3 的 RAG 索引最小幂等切片：`IndexService.createPendingTask(...)` 在同一文档最新任务为 `PENDING/RUNNING` 时复用已有任务，在 `FAILED/SUCCEEDED` 后创建新的 `PENDING` 任务。

## 2. What Went Well

- 先用 `DocumentControllerTest` 固定 API 行为，再把逻辑收敛到 `IndexService`，没有把业务判断放进 Controller。
- 范围控制清晰：只处理索引 active 去重，没有把上传 `requestId`、RAG query 重放、后台恢复混进同一切片。
- 聚焦测试和全量后端测试都能覆盖当前改动面。

## 3. What Didn't Go Well

- 初始交付文档晚于代码完成，需要补齐 Evidence、Acceptance、Memory、Changelog。
- 当前测试通过直接修改 repository 模拟任务状态，后续如果增加状态更新 API，应改用更接近真实路径的测试。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| RAG 长任务 active 去重最小切片 | No | 当前可由 `educational-rag-pipeline` 覆盖，不单独抽 skill |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | Evidence/Acceptance 在实现后补齐 | 后续小切片完成测试后立即同步交付文档 |
| Testing | 通过 Controller 集成测试覆盖索引任务状态 | 后续增加 worker 后补 Service/worker 层恢复测试 |
| Documentation | 总 TODO 已明确完成项和剩余项 | 继续避免把 P0-3 大项误标成整体完成 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 设计文档上传 `requestId` 幂等与响应快照 | Backend | 后续 P0-3 |
| 设计 RAG query 重放和冲突处理 | Backend/RAG | 后续 P0-3 |
| 增加索引任务恢复字段和后台扫描 | Backend/RAG | 后续 P0-3/P3-2 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory file
- [x] SKILL_REGISTRY.md 不需要更新
- [x] ARCHITECTURE_BASELINE.md 不需要更新
