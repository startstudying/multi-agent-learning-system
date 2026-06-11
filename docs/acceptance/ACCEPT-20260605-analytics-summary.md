# Analytics 学习分析扩展验收

## 验收结论

状态：通过。

## 验收项

| 验收项 | 结果 | 说明 |
|---|---|---|
| 保留 `GET /api/analytics/overview` | PASS | 旧字段继续由测试断言 |
| 新增学生端 summary endpoint | PASS | `GET /api/analytics/students/{learnerId}/summary` 通过测试 |
| 返回学习进度 | PASS | total/done/active/locked/completionRate |
| 返回掌握度趋势和当前掌握度 | PASS | 基于 `mastery_record` 与 path node 回退 |
| 返回最近错因 | PASS | 基于 `wrong_question` |
| 返回推荐下一步 | PASS | 优先未完成 path node |
| 新增 admin/agent summary 字段 | PASS | success/failure/latency/tokenCost/ragHitRate |
| 保留 token byModel/byAgentTask | PASS | 原测试断言保留 |
| 增加 byUser/byAgentName | PASS | 新测试断言通过 |
| 不修改其他 domain repository | PASS | 只改 analytics 主代码与 analytics 测试 |
| TDD | PASS | 已记录 RED 和 GREEN |

## 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 做用户校验并委托 Service |
| Frontend rules | PASS | 无前端变更 |
| Agent / RAG rules | PASS | 只读治理日志，不改变 Agent/RAG 执行 |
| Security | PASS | 学生 summary 阻止跨 learner 访问；无新依赖 |
| API / Database | PASS | SPEC 已记录新接口；无 schema 变更 |

## 限制

- Analytics 聚合为当前模块内只读内存聚合，后续数据量变大时建议给相关 repository 增加专用查询。
- 教师端 class summary 不在本次范围。
