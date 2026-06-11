# PLAN - RAG Chunk 生产化元数据补齐

## Skill Selection Report

## Task Type

后端 RAG 索引生产化切片，涉及索引生产逻辑、数据库迁移、测试与安全边界。

## Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目要求按 PRD/REQ/SPEC/PLAN/TASK/CONTEXT 流程推进。 |
| `educational-rag-pipeline` | 本切片属于 RAG chunk / indexing 生产链路。 |
| `test-driven-development` | 必须先写 RED 测试，再实现 chunk 生产化。 |
| `confidence-check` | 先确认没有重复实现、架构合规、根因明确。 |
| `verification-before-completion` | 完成前必须有新鲜测试证据。 |

## Missing Skills

无。

## GitHub Research Needed

否。当前能力可由现有 Java、Spring Data JPA、Flyway 与项目现有模式覆盖。

## New Project-Specific Skill To Create

暂不需要。

## Subagent Decision

Use Subagents: Yes
Reason: 已经完成并行只读分析，范围跨 RAG、数据库、测试与安全。
Parallelism Level: L1
Selected Subagents: RAG / Backend Architect, Security & Quality, Test Engineer, Integration Reviewer
Implementation Mode: Parallel analysis completed, single Codex implementation

## Confidence Check

| Check | Result |
|---|---|
| No duplicate implementation | PASS - 现有仅有基础字符切分，没有稳定 hash / overlap / heading hierarchy。 |
| Architecture compliance | PASS - 继续使用 Service/Repository/Flyway/JPA。 |
| Official docs verified | PASS - 现有模式已覆盖 Spring scheduling / JPA / Flyway 使用方式。 |
| Working implementation reference | PASS - 项目现有 RAG index worker 与 recovery 模式可复用。 |
| Root cause identified | PASS - chunk 生产仍停留在基础切分和简单去重。 |

Confidence: 0.95

## 实施步骤

1. 先写 RED 测试，覆盖 chunk token-ish overlap、stable hash、heading hierarchy、worker 一致性、V17 文本。
2. 实现 `IndexService` chunk 生产逻辑和 `KbDocChunk` 新字段。
3. 增加 V17 migration。
4. 更新 schema smoke 和 migration 文本测试。
5. 跑聚焦测试，再跑全量后端测试。
6. 生成 Evidence / Acceptance / Retro，并更新 Memory / Changelog / TODO。

## 风险

- H2 和 MySQL 对 schema / constraint 语义可能有细微差异，需要以 MySQL smoke 为准。
- 若 overlap 或 heading 元数据设计过大，可能污染 metadataJson，因此必须保持结构化且短小。

## 完成结果

- 已实现 `TOKEN_WINDOW_V1`、220 token-ish 窗口、40 token overlap、稳定 SHA-256 `chunkHash` 和 Markdown heading hierarchy。
- 已新增 V17 migration，并通过真实 MySQL 8 smoke 验证 V1-V17。
- 本计划只关闭 chunk 生产化元数据子切片；parser/OCR、VectorDB、hybrid retrieval、RRF 和 reranker 仍在 P3-2 后续任务中。
