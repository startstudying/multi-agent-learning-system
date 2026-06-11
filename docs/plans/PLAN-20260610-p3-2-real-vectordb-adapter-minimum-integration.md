# PLAN：P3-2 real VectorDB adapter minimum integration

## 目标

以最小、安全、可测试方式将 Qdrant VectorDB 接入现有 `VectorIndexAdapter` 边界。

## 执行步骤

1. 完成专家并行分析与集成决策。
2. 完成 dependency review。
3. 创建 L 级 PRD / REQ / SPEC / TASK / CONTEXT。
4. RED：
   - 新增 `QdrantVectorIndexAdapterTest`。
   - 新增 `RagVectorConfigurationTest`。
5. GREEN：
   - 新增配置类与 adapter。
   - 更新 `pom.xml` 与 `application.yml`。
   - 将 `NoopVectorIndexAdapter` 改为普通实现类，并由 `QdrantVectorConfiguration` 提供 missing-bean fallback。
6. Focused verification。
7. Adjacent verification。
8. Full backend verification。
9. Evidence / Acceptance。
10. 更新 Changelog、Memory、backend TODO。

## 文件计划

### 允许修改

- `backend/pom.xml`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/learningos/config/RagVectorProperties.java`
- `backend/src/main/java/com/learningos/rag/application/NoopVectorIndexAdapter.java`
- `backend/src/main/java/com/learningos/rag/vector/QdrantVectorConfiguration.java`
- `backend/src/main/java/com/learningos/rag/vector/QdrantVectorIndexAdapter.java`
- `backend/src/main/java/com/learningos/rag/vector/QdrantVectorOperations.java`
- `backend/src/main/java/com/learningos/rag/vector/QdrantVectorPoint.java`
- `backend/src/test/java/com/learningos/rag/vector/QdrantVectorIndexAdapterTest.java`
- `backend/src/test/java/com/learningos/rag/vector/RagVectorConfigurationTest.java`
- 本任务相关 docs

### 禁止修改

- REST Controller / DTO
- DB migration
- frontend
- Agent / Orchestrator
- parser/OCR provider
- unrelated P3-4 permission code

## 风险控制

| 风险 | 控制 |
|---|---|
| 默认测试外连 Qdrant | 默认 disabled；configuration test 验证 Noop |
| 多 `VectorIndexAdapter` bean 冲突 | `QdrantVectorConfiguration` 统一注册 `@ConditionalOnMissingBean(VectorIndexAdapter.class)` Noop fallback |
| raw content 写入 VectorDB | point payload 类不包含 content；测试断言 |
| provider 异常泄漏 | adapter 捕获异常并返回固定错误码 |
| allowedKbIds filter 缺失 | search operation 参数包含 allowedKbIds；测试断言 |
| gRPC/Netty CVE | dependency tree 已记录；升级或风险接受仍拆为后续独立任务 |

## 测试命令

```powershell
cd backend
mvn --% -Dtest=QdrantVectorIndexAdapterTest,RagVectorConfigurationTest test
mvn --% -Dtest=NoopVectorIndexAdapterTest,IndexServiceVectorPayloadTest,IndexServiceEmbeddingVectorTest,ChunkServiceVectorRetrievalTest,RagQueryServiceTest test
mvn test
```

## 停止条件

- 发现 Qdrant client API 无法在不传 raw content 的情况下实现 upsert/search。
- 新依赖导致全量测试无法启动且不是局部配置问题。
- gRPC/Netty CVE 需要强制依赖覆盖但覆盖破坏编译。

## 后续独立任务

- 真实 Qdrant / Docker / Testcontainers smoke。
- Qdrant collection dimension startup validation。
- Ops health / alerting integration。
- 多 provider vector adapter 扩展。
- gRPC/Netty dependency upgrade / risk acceptance review。
