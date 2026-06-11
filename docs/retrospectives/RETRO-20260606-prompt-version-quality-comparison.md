# Prompt Version 质量对比复盘

## 做对的点

- 先用 subagent 并行拆分架构、测试、安全问题，再由主 Codex 集成实现。
- 沿用 `evaluation` 模块，避免把持久化职责塞进 RAG 或 Assessment 的即时评估服务。
- 先做 RED，再补实体、migration、service、controller。
- Code review 后补了加权平均和 0 样本拒绝测试，避免指标统计被小样本 run 扭曲。

## 风险与后续

- 当前 run 是人工或离线评测结果记录，不是自动执行 prompt A/B。
- 后续可基于 `evaluation_run` 增加自动执行器和周期性报告。
- 后续 P2-2/P2-3 应把 RAG 和 grading 的样本级 benchmark 补得更完整。
- 完整 `mvn test` 未在本轮运行，后续合并前仍建议跑全量。
