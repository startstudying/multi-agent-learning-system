# TASK - P3-2-A RAG Embedding Service 与可选 VectorDB Adapter 边界

## 1. 追踪

- PLAN：`docs/plans/PLAN-20260608-rag-embedding-vector-adapter.md`
- SPEC：`docs/specs/SPEC-20260608-rag-embedding-vector-adapter.md`

## 2. 目标

完成无新增依赖、无 schema/API/frontend 变更的 RAG embedding/vector adapter boundary：默认 disabled/noop，索引与查询链路可安全感知 vector 状态，并为后续真实 provider/VectorDB 接入留出边界。

## 3. 任务拆分

| Task | 内容 | Done |
|---|---|---|
| TASK-01 | 补齐 workflow docs 与 Context Pack | 已完成 |
| TASK-02 | 补 embedding/noop adapter contract tests | 已完成 |
| TASK-03 | 补 IndexService metadata 与 enabled-failure tests | 已完成 |
| TASK-04 | 补 ChunkService vector branch 与越权过滤 tests | 已完成 |
| TASK-05 | 收敛实现并运行验证 | 已完成 |
| TASK-06 | Evidence/Acceptance/Memory/Changelog/TODO/Retro/Skill | 已完成 |

## 4. 允许修改文件

同 `docs/plans/PLAN-20260608-rag-embedding-vector-adapter.md` 第 6 节。

## 5. 禁止修改文件

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`

## 6. 执行规则

- 每次只处理一个任务点。
- 新生产代码必须有对应测试。
- 不引入真实 provider、VectorDB、DB schema 或 dependency。
- 不在 docs/memory 中记录 secrets、API key、raw logs。
- 若测试失败，先定位根因再修复，不盲目重试。

## 7. 测试命令

```powershell
cd backend
mvn --% -Dtest=EmbeddingServiceTest,NoopVectorIndexAdapterTest,IndexServiceTest,ChunkServiceVectorRetrievalTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest,IndexTaskWorkerSchedulerTest,RagQueryServiceTest,RrfRankerTest test
mvn --% -Dtest=RagQueryServiceTest,DocumentControllerTest,OrchestratorWorkflowControllerTest,IndexServiceTest,IndexTaskWorkerSchedulerTest test
mvn test
```

## 8. 完成标准

- [x] PRD/REQ/SPEC/PLAN/TASK/Context 已存在。
- [x] embedding disabled/noop contract 有测试。
- [x] vector adapter disabled/noop contract 有测试。
- [x] `IndexService` 默认 disabled 索引成功且 metadata 安全。
- [x] enabled provider/adapter 失败时不伪装 `INDEXED`。
- [x] `ChunkService` vector branch 不返回 forbidden candidates。
- [x] `sourcesJson` vector metadata 安全且不含 raw chunk/vector/provider error。
- [x] 不新增 dependency/schema/frontend/API 变更。
- [x] focused/adjacent/full tests 已运行或限制已记录。
- [x] Evidence/Acceptance/Memory/Changelog/TODO/Retro/Skill 已更新。

## 9. 当前状态

已完成。当前只关闭 boundary/noop/fake 切片；真实 Spring AI embedding provider、真实 VectorDB SDK、真实向量集合与部署治理仍需后续独立 TASK。
