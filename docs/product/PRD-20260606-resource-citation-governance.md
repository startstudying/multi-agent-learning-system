# 资源生成引用与幻觉治理 PRD

## 背景

当前 RAG 问答已经能返回 `sources` 并持久化 `source_citation`，但资源生成只在 `LearningResource.citationSummary` 中写一句文本说明，没有落到统一的 citation 表。这样会导致课程资源、复习材料和练习题虽然声称基于课程材料，却缺少可审计来源。

P1-4 的目标是把资源生成纳入与 RAG 问答一致的引用治理：生成资源必须保存来源引用；没有来源时必须明确标记 `NO_SOURCE`，并进入更严格的审核状态。

## 目标

- 资源生成任务为生成内容保存 normalized `source_citation` 记录。
- Critic review 记录包含引用检查结果，说明引用数量、相关性和是否存在伪造来源风险。
- 无来源生成内容必须标记 `NO_SOURCE`，并保持 `NEEDS_REVIEW` / `PENDING_CRITIC`，不能绕过人工或 Critic 审核发布。
- 复用现有 `source_citation`、`resource_review.citation_check`、`ContentSafetyService` 和 Review Gate 状态模型。

## 非目标

- 不实现真实向量检索或 reranker。
- 不新增前端页面。
- 不改变 RAG 问答接口。
- 不新增独立资源引用表，先复用 `source_citation.trace_id` 关联资源生成 trace。
- 不实现自动教师审批。

## 用户价值

- 教师审核资源时可以看到引用检查证据。
- 答辩和论文中能说明资源生成有 citation grounding 闭环。
- 无来源内容不会伪装成已引用课程材料，降低幻觉风险。
