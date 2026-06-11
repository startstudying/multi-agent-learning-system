# Resource Citation Governance

## 使用场景

当实现或审查 AI 生成学习资源的引用治理、`NO_SOURCE` 处理、Critic citation check、review gate 发布控制时使用。

## 推荐最小闭环

1. 生成资源任务必须有 `traceId`。
2. 有课程来源时保存 `source_citation`，至少包含 `traceId`、`documentId`、`documentName`、`excerpt`、`score`。
3. 资源响应的 `citationSummary` 必须明确区分 `COURSE_RAG` 和 `NO_SOURCE`。
4. 初始 `ResourceReview.citationCheck` 必须保存引用检查结果。
5. `NO_SOURCE` 内容保持 `PENDING_CRITIC` / `NEEDS_REVIEW`。
6. Review Gate 需要两层防线：审批时拦截 `APPROVED + NO_SOURCE`，release 时兜底检查资源和 review 是否包含 `NO_SOURCE`。

## 分层规则

- Controller 只处理 HTTP 和当前用户，不写引用治理规则。
- Service 层负责 citation 持久化、review check、release gate。
- Agent / Tool 不直接访问 Repository。
- 不用 prompt 代替权限或发布控制。

## 测试建议

- 有来源资源生成后，按 `traceId` 能查到 `source_citation`。
- `citationSummary` 包含 `COURSE_RAG`。
- 初始 review 的 `citationCheck` 包含 persisted source citation 和 fabricated source 检查语义。
- 无来源资源标记 `NO_SOURCE`，不写 citation。
- `NO_SOURCE` approval 返回 `409 CONFLICT`。
- 即使状态被手工设为 published，learner release 仍被 `NO_SOURCE` 兜底拦截。

## 已知边界

- `traceId` citation 是任务级证据，不是 resource-level 证据。
- 真实 Course RAG retrieval 接入后，应优先使用检索结果生成 citation，而不是 deterministic citation。
- 后续可增加 `source_citation.resource_id` / `generation_task_id` 或 `target_type/target_id`。
