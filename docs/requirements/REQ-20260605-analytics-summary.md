# Analytics 学习分析扩展需求

## 功能需求

1. `GET /api/analytics/overview` 必须继续返回原有字段。
2. overview 新增 `agentSummary`：
   - `totalTasks`
   - `successCount`
   - `failureCount`
   - `successRate`
   - `failureRate`
   - `averageLatencyMs`
   - `tokenCost`
   - `ragHitRate`
3. overview 的 `tokenUsage` 保留：
   - `byAgentTask`
   - `byModel`
   并新增：
   - `byUser`
   - `byAgentName`
4. `GET /api/analytics/students/{learnerId}/summary` 返回：
   - `learnerId`
   - `progress`
   - `currentMastery`
   - `masteryTrend`
   - `recentWrongCauses`
   - `recommendedNextSteps`
5. 学生端 summary 只能查询当前请求用户自己的 `learnerId`。

## 数据来源

- 学习进度：`learning_path_node`。
- 当前掌握度与趋势：`mastery_record`，没有记录时回退到 path node mastery。
- 最近错因：`wrong_question`。
- 下一步建议：优先使用未完成 path node，其次使用 wrong question 的 `resourcePushStrategy`。
- Agent 运行统计：`agent_task`。
- Token 成本：`token_usage_log` + `model_call_log` + `agent_task`。
- RAG 命中率：`kb_query_log.retrievalCount > 0` 视为命中；无日志时返回 0。

## 约束

- 缺失数据返回 0 或 empty list，不伪造。
- 不修改其他 domain repository。
- 不新增依赖。
- 工作流文档正文使用中文，API path、JSON 字段和代码标识保持英文。
