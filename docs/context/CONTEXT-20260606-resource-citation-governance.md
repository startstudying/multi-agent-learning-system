# 资源生成引用与幻觉治理 Context Pack

## 当前任务边界

补齐 P1-4 中资源生成 citation 持久化、Critic citation check 和 `NO_SOURCE` 治理的最小后端闭环。

## 允许修改文件

- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java`
- `backend/src/main/java/com/learningos/rag/repository/SourceCitationRepository.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
- 本切片相关 docs 文件
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 禁止修改

- 不改前端。
- 不新增依赖。
- 不新增独立资源引用表。
- 不改 RAG 问答已有接口语义。
- 不绕过 Review Gate 发布逻辑。

## 验证命令

最小 RED / GREEN：

```powershell
cd backend
mvn "-Dtest=ResourceGenerationControllerTest#resourceGenerationPersistsSourceCitationsAndCriticCitationCheck+noSourceGeneratedResourcesRequireReviewAndRejectApproval" test
```

相关回归：

```powershell
cd backend
mvn "-Dtest=ResourceGenerationControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest,RagQueryServiceTest,SchemaConvergenceMigrationTest" test
```

最终 Orchestrator 回归：

```powershell
cd backend
mvn "-Dtest=OrchestratorWorkflowControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest,RagQueryServiceTest,SchemaConvergenceMigrationTest" test
```

## 当前任务边界说明

- 本切片使用 `source_citation.traceId` 作为任务级引用证据关联。
- 不新增 V11 / resource-level citation schema。
- deterministic citation 只用于闭合后端治理测试，不代表真实 Course RAG retrieval grounding。
