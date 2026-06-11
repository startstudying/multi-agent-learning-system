# TASK：P3-2 子任务：real VectorDB adapter minimum integration

## Goal

实现 Qdrant real VectorDB adapter 最小集成，挂在现有 `VectorIndexAdapter` 后面，并保持默认 Noop、安全 payload、权限过滤和 fallback。

## Task Type

RAG / VectorDB 生产化增强。

## Size Decision

- Size: L
- 原因：新增依赖、外部服务连接、配置、RAG indexing/retrieval adapter、安全审查。

## Selected Skills

- feature-development-workflow
- educational-rag-pipeline
- spring-ai-agent-backend
- rag-embedding-vector-adapter
- security-review
- dependency-review
- test-driven-development

## Context Pack Summary

完整 Context Pack：`docs/context/CONTEXT-20260610-p3-2-real-vectordb-adapter-minimum-integration.md`

## Implementation Tasks

1. RED：新增 `QdrantVectorIndexAdapterTest`。
2. RED：新增 `RagVectorConfigurationTest`。
3. GREEN：新增 `RagVectorProperties`。
4. GREEN：新增 `QdrantVectorOperations` / `QdrantVectorPoint`。
5. GREEN：新增 `QdrantVectorIndexAdapter`。
6. GREEN：新增条件配置 `QdrantVectorConfiguration`。
7. GREEN：更新 `pom.xml` 与 `application.yml`。
8. GREEN：将 Noop 改为普通实现类，并由 `QdrantVectorConfiguration` 提供 missing-bean fallback。
9. 运行 focused / adjacent / full 测试。
10. 写 Evidence / Acceptance。
11. 更新 Changelog / Memory / TODO。

## Acceptance Criteria

- 默认禁用时使用 Noop，不连接 Qdrant。
- 启用且配置完整时可以装配真实 adapter。
- upsert payload 不含 raw content / question / prompt / user id / storage key / secret。
- search 下推 `allowedKbIds` 和 `topK`。
- provider 失败返回固定低敏错误码。
- RAG 查询侧 vector failure 仍 fallback。
- indexing upsert/delete failure 不伪装成功。
- `mvn test` 通过，或限制被清晰记录。
