# 资源生成引用与幻觉治理规格

## 最小 Citation 模型

本切片复用 `source_citation`：

```json
{
  "traceId": "trc_resource_generation",
  "documentId": "course_rag:goal_spring_boot",
  "documentName": "Course RAG evidence for goal_spring_boot",
  "pageNum": 1,
  "sectionTitle": "Resource grounding",
  "excerpt": "Generated resource is grounded by course context for path node node_sql_join.",
  "score": 0.80
}
```

`traceId` 与 `ResourceGenerationTask.traceId` 保持一致。当前没有真实 resource-citation 关联表，因此同一 trace 下的 citation 作为任务级引用证据。

## Citation Summary

有来源：

```text
COURSE_RAG: 1 citation(s) persisted for critic review.
```

无来源：

```text
NO_SOURCE: generated draft requires manual review before release.
```

## Critic Citation Check

初始 review 写入结构化文本：

```text
Citation check: 1 persisted source citation(s); no fabricated source detected in deterministic draft.
```

无来源：

```text
Citation check: NO_SOURCE. No persisted source citation; manual review required before release.
```

## 无来源策略

- `LearningResource.safetyStatus = NEEDS_REVIEW`
- `LearningResource.reviewStatus = PENDING_CRITIC`
- `ResourceGenerationTask.reviewStatus = PENDING_CRITIC`
- learner release 继续由 `ReviewGovernanceService.canReleaseToLearner(...)` 控制。

## 最小触发规则

- 默认资源生成使用 deterministic course citation。
- 如果请求 `goalId` 或 `pathNodeId` 包含 `no_source`，则模拟无来源路径，用于测试 `NO_SOURCE` 治理。

## 架构约束

- Controller 不增加业务逻辑。
- Citation 写入由 `ResourceGenerationService` 调用 `SourceCitationRepository` 完成。
- 不新增外部依赖。
- 不改 RAG 问答已有 citation 语义。
