# Evaluation Set 管理验收

## 验收项

- [x] 支持创建 `RAG_QUESTION` evaluation set。
- [x] 支持创建 `GRADING_SAMPLE` evaluation set。
- [x] 支持创建 `RESOURCE_GENERATION_SAMPLE` evaluation set。
- [x] 同一创建者 `code + version` 重复提交执行 upsert。
- [x] 样本 payload 与 set type 不匹配时返回 `VALIDATION_ERROR`。
- [x] 普通学生无法访问 evaluation set 管理 API。
- [x] 教师不能将评估集绑定到其他教师的课程。
- [x] 列表接口不返回样本明细。
- [x] V13 migration 覆盖 `evaluation_set` 和 `evaluation_sample`。

## 未覆盖范围

- 未实现 evaluation run。
- 未实现按 prompt version 对比质量指标。
- 未做真实 MySQL migration smoke。
- 未实现正式 RBAC，只沿用当前项目临时 `teacher/admin` 用户边界。
