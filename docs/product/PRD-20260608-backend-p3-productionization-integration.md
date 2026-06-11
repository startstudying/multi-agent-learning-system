# PRD-20260608 后端 P3 生产化集成总控

## 1. 背景

`docs/planning/backend-architecture-todolist.md` 的 P0/P1/P2 与 P3 多个基础切片已完成，剩余未完成项集中在：

- P3-2：复杂 PDF/DOCX、OCR fallback、真实页码与章节层级识别。
- P3-3：真实 Spring AI / Spring AI Alibaba Chat 与 Embedding provider 接入，`model_call_log.provider` 等 provider 观测字段补齐。
- P3-4：课程/班级/知识库/学习路径/资源/答题记录的生产级权限矩阵与渗透测试。

用户要求完成该后端架构计划，并使用专家 subagent 并行开发。

## 2. 产品目标

把后端从“可演示闭环”推进到“可生产化答辩”的安全、可观测、可扩展基线：

1. 接真实模型和向量能力前，先收口数据访问权限，避免扩大越权数据暴露面。
2. 模型调用日志能区分 provider、model、prompt version、latency、token、safe error。
3. RAG parser 与索引链路继续保持安全边界，不因 OCR 或复杂格式解析泄露原始二进制、路径或 provider 错误。

## 3. 范围

### 本总控覆盖

- 汇总 P3 剩余项。
- 启用专家 subagent 并行分析。
- 定义切片顺序、文件边界、验证命令与验收矩阵。

### 本轮优先实现

P3-4-C：课程读取与知识图谱读取权限收口、评分评估接口权限收口、课程/知识图谱 missing-vs-foreign 防枚举测试。

## 4. 非目标

- 不在一个切片内同时实现 JWT、class enrollment 表、Spring AI provider、VectorDB、OCR。
- 不写入 `docs/superpowers/**`。
- 不新增未审查依赖。
- 不让前端直接调用 LLM 或保存 API key。

## 5. 成功标准

- 总控文档和当前切片 PRD/REQ/SPEC/PLAN/TASK/CONTEXT 存在。
- 专家 subagent 报告落盘到 `docs/subagents/runs/`。
- 当前切片完成后更新 Evidence、Acceptance、Memory、Changelog 和 TODO。
- 所有权限检查在后端 Service 层完成，不依赖 Prompt 或前端。
