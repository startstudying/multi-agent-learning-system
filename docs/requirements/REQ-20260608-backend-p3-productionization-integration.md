# REQ-20260608 后端 P3 生产化集成总控

## 1. 总体需求

| ID | Requirement | Priority |
|---|---|---|
| REQ-P3-INT-1 | 必须使用专家 subagent 并行分析 P3-2/P3-3/P3-4 剩余项，并保存报告。 | P0 |
| REQ-P3-INT-2 | 每个实现切片必须先创建或更新 PRD/REQ/SPEC/PLAN/TASK/CONTEXT。 | P0 |
| REQ-P3-INT-3 | 实现阶段一次只执行一个 TASK，且只修改 Context Pack 允许文件。 | P0 |
| REQ-P3-INT-4 | 新增依赖前必须创建 `docs/security/DEPENDENCY-REVIEW-*.md` 并在 PLAN/TASK 中批准。 | P0 |
| REQ-P3-INT-5 | 完成切片后必须运行聚焦测试；可行时运行 `mvn test`。 | P0 |
| REQ-P3-INT-6 | 完成切片后必须创建 Evidence 和 Acceptance，并更新 Memory、Changelog、TODO。 | P0 |

## 2. 切片优先级

| Order | Slice | Reason |
|---:|---|---|
| 1 | P3-4-C 权限矩阵安全前置 | 真实模型和向量接入前必须先降低数据泄露面。 |
| 2 | P3-3-B provider observability schema | 接 provider 前先补齐 provider 落库字段。 |
| 3 | P3-3-C Spring AI Chat provider adapter | 只接 Chat，保持 `AiModelGateway` 边界。 |
| 4 | P3-3-D Spring AI Embedding provider adapter | 只接 Embedding，保持 `EmbeddingService` batch contract。 |
| 5 | P3-2-D VectorDB adapter | 在 Embedding 后接入向量库，继续 allowed-KB 双重过滤。 |
| 6 | P3-2-E parser layout/page hierarchy | 先做无依赖页码/层级增强。 |
| 7 | P3-2-F OCR fallback | 依赖与安全审查通过后再实现。 |

## 3. 当前切片需求索引

当前执行切片：`REQ-20260608-rbac-course-class-answer-matrix.md`。
