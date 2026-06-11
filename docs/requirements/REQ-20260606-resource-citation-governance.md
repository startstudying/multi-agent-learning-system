# 资源生成引用与幻觉治理需求

## 功能需求

1. 资源生成任务创建后，必须为有来源的生成任务写入至少一条 `source_citation` 记录。
2. `source_citation.trace_id` 必须使用资源生成任务的 `traceId`，与 Agent Trace 可关联。
3. `LearningResource.citationSummary` 必须区分 `COURSE_RAG` 和 `NO_SOURCE`。
4. Critic review 初始记录必须写入 `citationCheck`，至少包含引用数量和是否需要人工复核。
5. 无来源资源必须标记 `NO_SOURCE`，`safetyStatus` 保持 `NEEDS_REVIEW`，`reviewStatus` 保持 `PENDING_CRITIC`。
6. 无来源资源不能因为普通 approve 以外的路径被发布；现有 Review Gate 仍要求全部 review approve 后才发布。

## 数据需求

- 复用 `source_citation` 表。
- 复用 `resource_review.citation_check`。
- 不新增 migration，除非实现发现现有表无法承载最小闭环。

## 验收需求

- Resource generation create 后 `SourceCitationRepository.countByTraceId(traceId)` 大于 0。
- 资源响应中 citation summary 显示课程来源语义。
- Review queue/detail 中 `citationCheck` 非空。
- 无来源请求生成的资源含 `NO_SOURCE` citation summary，且不发布给学生。
- 相关测试覆盖 create/get/replay 或 review gate 行为。
