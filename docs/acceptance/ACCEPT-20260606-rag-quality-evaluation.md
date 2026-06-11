# RAG 质量评估验收

## 验收项

- [x] 支持课程 benchmark 结构化输入。
- [x] 支持样本级 expected chunk、标准答案、禁止回答范围、无来源期望。
- [x] 支持 `Recall@K`。
- [x] 支持 `Citation Accuracy`。
- [x] 支持 `Groundedness`。
- [x] 支持 `No-source Refusal Rate`。
- [x] 返回 `benchmarkSummary`、`sampleResults` 和可归档 `report`。
- [x] 保持旧版 RAG evaluation 请求兼容。

## 非本轮范围

- 从数据库自动拉取 evaluation set 并批量运行。
- LLM-as-judge groundedness 裁判。
- 定时评估任务。
# Worker A 补充：RAG 评估归档自动化验收

## 验收项

- [x] 提供可周期执行脚本：`scripts/run-rag-evaluation-archive.ps1`。
- [x] 脚本重复执行时输出稳定归档文件：`backend/target/rag-evaluation-archive/latest/rag-quality-evaluation-report.md`。
- [x] 报告包含 benchmark 元数据、样本数、核心 RAG 指标、样本级结果和 Service 原始 `report` 文本。
- [x] 新增 `RagEvaluationArchiveReportTest` 作为 JUnit 合约测试，健康测试树恢复后可直接验证脚本和报告生成契约。
- [x] 不新增依赖、不修改生产服务、不修改数据库迁移、不触碰 token budget 相关实现。

## 验收命令

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-rag-evaluation-archive.ps1
```

当前结果：通过，生成 Markdown 归档报告。

## 限制说明

当前仓库存在非本任务的 Maven compile/testCompile 阻塞，导致完整 `mvn test` 和定向 JUnit 测试无法作为本轮最终绿色证据。脚本已限制在 RAG evaluation 归档所需源码集合内执行，不修改无关模块。
