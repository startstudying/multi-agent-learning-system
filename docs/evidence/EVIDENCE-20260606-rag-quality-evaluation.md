# RAG 质量评估证据

## RED

命令：

```powershell
cd backend; mvn "-Dtest=RagEvaluationServiceTest,RagEvaluationControllerTest" test
```

结果：

- 失败，符合预期。
- 失败原因：`RagEvaluationRequest` / `RagEvaluationResult` 尚不支持 `benchmark`、`noSourceRefusalRate`、`benchmarkSummary`、`sampleResults` 和 `report`。

## GREEN

命令：

```powershell
cd backend; mvn "-Dtest=RagEvaluationServiceTest,RagEvaluationControllerTest" test
```

结果：

- Tests run: 5
- Failures: 0
- Errors: 0
- BUILD SUCCESS

## 覆盖点

- 旧版 `expectedSourceIds + actualCitations + topK` 请求保持兼容。
- benchmark 样本支持 `expectedChunkIds`、`expectedAnswer`、`forbiddenAnswerScope`、`expectedNoSource`、`actualAnswer`、`actualCitations`。
- 指标覆盖 `Recall@K`、`Citation Accuracy`、`Groundedness`、`No-source Refusal Rate`。
- 响应包含 benchmark 汇总、样本级结果和可归档 report。

## 仍需说明

- 本轮不直接读取 `evaluation_set/evaluation_sample`，由调用方或后续脚本把固化样本装配为请求。
- 无来源拒答采用确定性 `NO_SOURCE` 文本识别，未引入模型裁判。
# Worker A 补充：RAG 评估归档自动化证据

## RED

命令：

```powershell
cd backend; mvn "-Dtest=RagEvaluationArchiveReportTest" test
```

结果：
- 失败符合预期：新增合约测试要求 `scripts/run-rag-evaluation-archive.ps1` 存在，但脚本尚未创建。
- 失败点：`RagEvaluationArchiveReportTest.scriptInvokesArchiveReportTestWithConfigurableArchiveDirectory`。

## GREEN

命令：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-rag-evaluation-archive.ps1
```

结果：
- 脚本退出码：0。
- 输出报告：`backend/target/rag-evaluation-archive/latest/rag-quality-evaluation-report.md`。
- 报告包含 benchmark 元数据、样本数、`Recall@K`、`Citation Accuracy`、`Groundedness`、`No-source Refusal Rate`、样本级结果和 Service 原始可归档报告文本。

## 当前阻塞说明

命令：

```powershell
cd backend; mvn "-Dtest=RagEvaluationArchiveReportTest" test
```

结果：
- 当前仓库 testCompile 被非本任务文件阻塞：
  - `AnalyticsControllerTest` 引用缺失的 `seedTokenBudgetGovernanceSignals()`。
  - `IndexServiceTest` 引用缺失的 `IndexService.processIndexTask(String)`。
- 当前仓库 main compile 也被非本任务 token budget/analytics 代码阻塞：
  - `AnalyticsController` 引用缺失的 `AnalyticsService.TokenBudgetGovernanceSummary`。
  - `AnalyticsController` 调用缺失的 `tokenBudgetGovernance(...)`。
- Worker A 未修改 token budget、analytics、index 或生产 RAG 服务；归档脚本通过最小源码编译绕过无关阻塞。
