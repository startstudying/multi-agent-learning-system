# 资源生成引用与幻觉治理验收报告

## 1. 验收清单

### 功能验收

- [x] 资源生成有来源时保存 `source_citation`。
- [x] `source_citation.traceId` 与资源生成任务响应 `traceId` 对齐。
- [x] 有来源资源的 `citationSummary` 包含 `COURSE_RAG`。
- [x] 初始 Critic review 的 `citationCheck` 包含已保存引用数量和伪造来源检查语义。
- [x] 无来源资源的 `citationSummary` 包含 `NO_SOURCE`。
- [x] 无来源资源保持 `NEEDS_REVIEW` / `PENDING_CRITIC`。
- [x] `NO_SOURCE` review 不能直接 `APPROVED`，接口返回 `409 CONFLICT`。
- [x] 学生端 learner resources release 不能绕过 `NO_SOURCE` gate。

### 数据验收

- [x] `SourceCitationRepository.countByTraceId(...)` 可统计任务级引用。
- [x] `SourceCitationRepository.findByTraceIdOrderByCreatedAtAsc(...)` 可恢复引用证据。
- [x] 引用记录包含 `documentId`、`documentName`、`excerpt`、`score`。

### 文档验收

- [x] PRD / REQ / SPEC / PLAN / TASK / CONTEXT 已存在。
- [x] Evidence / Acceptance / Retrospective 已创建。
- [x] Changelog / Memory / API docs / TODO 已更新。

## 2. 测试摘要

| 测试项 | 结果 |
|---|---|
| RED：资源生成引用治理缺失 | PASS，失败符合预期 |
| GREEN：新增两个 P1-4 控制器测试 | PASS，2 tests |
| 回归：resource / review / RAG / migration | PASS，45 tests |
| 回归：Orchestrator + resource / review / RAG / migration | PASS，69 tests |

## 3. 验收结论

- [x] 通过
- [ ] 有条件通过
- [ ] 不通过

## 4. 遗留问题

- 本切片未新增 `source_citation.resource_id` 或 `generation_task_id`，资源级引用治理待后续 schema 切片完成。
- 当前 deterministic citation 是治理占位证据，真实 RAG 检索引用接入仍需后续实现。
- 审核权限仍使用临时 `teacher/admin` 边界，真实 RBAC 仍是 P3 安全任务。
