# Evaluation Set 管理 PRD

## 背景

当前后端已有 RAG 质量评估接口和自动批改质量评估接口，但它们都是一次性传参计算，没有持久化的 benchmark / sample set。论文和后续实验需要稳定回答：同一批 RAG 问题、评分样本、资源生成样本，在不同 prompt version 下质量是否提升。

## 目标

- 增加统一的 Evaluation Set 管理能力，覆盖 RAG 问题集、评分样本集、资源生成样本集。
- 支持教师或管理员创建、查询、按类型筛选评估集。
- 每个评估集包含 code、version、type、status、promptCode、promptVersion 和样本列表。
- 样本按类型做最小结构化校验，避免任意 JSON 扩散。

## 非目标

- 本轮不实现自动运行评估集。
- 本轮不实现 prompt version 质量对比 API。
- 本轮不接入真实模型或真实 RAG 执行。
- 本轮不保存 raw prompt、raw model output 或学生真实答题原文日志。

## 用户价值

- 论文实验可以固定样本集，避免每次临时构造评估输入。
- RAG、评分、资源生成三类质量评估可以使用统一生命周期。
- 后续第 143 行 TODO 可直接基于 evaluation set/run 设计 prompt version 对比。

## MVP 范围

- 新增 `evaluation_set` 和 `evaluation_sample` 表。
- 新增 `POST /api/evaluation-sets`、`GET /api/evaluation-sets`、`GET /api/evaluation-sets/{setId}`。
- 支持 `RAG_QUESTION`、`GRADING_SAMPLE`、`RESOURCE_GENERATION_SAMPLE` 三类样本。
- 临时权限：`teacher`、`admin` 或课程 teacher 可管理；普通学生禁止访问管理 API。

## 验收标准

- 能创建包含 RAG 样本的评估集。
- 能创建包含人工分和 rubric 的评分样本集。
- 能创建包含学习目标、画像快照和质量标准的资源生成样本集。
- 重复提交同一创建者的 `code + version` 更新原评估集，不产生重复 set。
- 样本类型和 payload 不匹配时返回校验错误。
- 普通学生访问管理 API 返回 403。
