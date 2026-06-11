# Review Gate 强约束需求

## 功能需求

1. `GET /api/resources/generation-tasks/{taskId}` 在资源未完全审核通过时，不返回任何资源的 `markdownContent`。
2. `POST /api/resources/generation-tasks` 创建完成后，如果任务处于 `WAITING_REVIEW`，响应同样不得返回 `markdownContent`。
3. `GET /api/agent/resource-generation/tasks/{taskId}/learner-resources` 继续作为正式发布读取接口，未通过审核返回 403。
4. 全部资源和全部 review 都为 `APPROVED` 后，任务详情和正式发布接口可以返回正文。
5. 审核发布判断必须在 Service 层完成，Controller 不包含审核判断。

## 数据需求

本轮不新增字段，不新增迁移。

复用：

- `resource_generation_task.review_status`
- `learning_resource.review_status`
- `resource_review.status`

## 约束

- 不新增依赖。
- 不改变 `ResourceGenerationResponse` 的主要结构。
- 未审核时可以保留资源元数据，正文为空或不序列化。
