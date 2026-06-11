# Review Gate 强约束规格

## 发布判断

复用 `ReviewGovernanceService.canReleaseToLearner(generationTaskId)`：

```text
task.reviewStatus == APPROVED
AND totalResources > 0
AND approvedResources == totalResources
AND totalReviews == totalResources
AND approvedReviews == totalReviews
```

## 响应规则

### 未发布任务详情

```json
{
  "resourceId": "res_xxx",
  "type": "LECTURE",
  "modality": "MARKDOWN",
  "title": "Professional course explanation document",
  "reviewStatus": "PENDING_CRITIC",
  "citationSummary": "Cites Course RAG sources before critic approval.",
  "safetyStatus": "NEEDS_REVIEW"
}
```

未审核时不返回 `markdownContent`。

### 已发布任务详情

审核全部通过后，`markdownContent` 可返回。

## 服务行为

- `ResourceGenerationService.toResponse(...)` 生成响应前计算一次 `releasedToLearner`。
- `GeneratedResourceResponse` 对空字段不序列化。
- `toResourceResponse(...)` 根据 `releasedToLearner` 决定是否填充 `markdownContent`。
- `getLearnerResources(...)` 仍调用 `ReviewGovernanceService.canReleaseToLearner(...)` 并在未发布时返回 403。

## 验收标准

- 创建资源生成任务后，响应中资源正文不可见。
- 查询未审核任务详情时，资源正文不可见。
- 未审核正式发布接口仍返回 403。
- 全部审核通过后，任务详情和正式发布接口都返回正文。
- 权限和审核判断都在 Service 层完成。
