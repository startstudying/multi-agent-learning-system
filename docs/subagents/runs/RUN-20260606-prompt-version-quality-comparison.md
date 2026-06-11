# Prompt Version 质量对比子任务记录

| Subagent | 任务 | 状态 |
|---|---|---|
| Architect | 设计 evaluation run / metric schema、API 和服务边界 | completed |
| Test Engineer | 设计 RED/GREEN 测试和回归命令 | completed |
| Security Reviewer | 分析权限、IDOR、敏感数据治理和负向测试 | completed |
| Code Reviewer | 审查本轮实现与测试 | completed |

## 集成结论

- 本轮采用 `evaluation_run` + `evaluation_run_metric`。
- API 统一放在 `/api/evaluation-runs`。
- Comparison 只读取 `SUCCEEDED` run。
- 权限沿用 evaluation set 的临时 teacher/admin + course ownership 模型。
- 不落 raw prompt、raw model output、学生原始答案全文。
- Comparison 响应只返回聚合指标，不返回样本输入、`promptText`、`answerText`、`inputJson`、`outputJson` 或 `rawOutput`。

## Code Review 结论

- 高风险问题：同一 metric 的多 run 聚合不能使用简单平均，已修复为按 `metric.sampleCount` 加权平均。
- 边界问题：`metric.sampleCount <= 0` 不能参与记录和聚合，已在服务层拒绝。
- 已通过定向和宽回归测试，证据见 `docs/evidence/EVIDENCE-20260606-prompt-version-quality-comparison.md`。
