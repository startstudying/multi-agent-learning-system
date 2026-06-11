# Evaluation Set 管理复盘

## 有效做法

- 使用独立 `evaluation` 模块，避免把评估集管理散落到 RAG、assessment、agent 包中。
- 先做 set/sample 管理，不提前实现 run/compare，范围可控。
- 安全子任务提醒了课程越权边界，并通过 RED/GREEN 补上测试。

## 问题

- 第一次 GREEN 后数组 JSON 解析写错，`expectedSourceIds` 和 `qualityCriteria` 返回空列表。
- 初始权限逻辑允许 `teacher` 绑定外部课程，后续补测才发现。

## 后续改进

- 第 143 行应新增 `evaluation_run` 和 prompt version 对比 API。
- 后续正式 RBAC 完成后，替换当前 `teacher/admin` 临时用户判断。
- 如果评估样本包含标准答案明文，后续需要引入更细粒度的敏感字段脱敏策略。
