# DECISION_MEMORY.md

## Architecture Decisions

| Date | Decision | Reason | ADR |
|---|---|---|---|
| 2026-06-05 | Use Codex + Cursor only | Simplify toolchain | — |
| 2026-06-05 | Use Spring AI as main AI orchestration layer | Keep backend unified | — |
| 2026-06-05 | Spec-first development workflow | Prevent blind coding | — |
| 2026-06-05 | Project Memory system | Control context growth | — |
| 2026-06-05 | Multi-Expert Subagent Gate | Support cross-module development | — |

## Pending Decisions

## 2026-06-21 Memory / Answer Quality Decisions

| Decision | Reason | Reference |
|---|---|---|
| Start memory and answer-quality implementation with Privacy Guard before expanding context | Long-term memory and ChatGPT-like personalization amplify sensitive data if raw RAG/profile/trace text is persisted first | `docs/specs/SPEC-20260621-memory-answer-quality-roadmap.md` |

Additional pending decision items from this roadmap:

| Question | Options | Status |
|---|---|---|
| Memory storage MVP | Reuse MySQL + existing VectorIndexAdapter / introduce PostgreSQL + pgvector / hybrid migration | ADR required before schema or dependency change |
| OpenAI conversation state usage | Project DB as source of truth + external response id reference / external conversation as primary memory | ADR required before implementation |

| Question | Options | Status |
|---|---|---|
| ORM choice: MyBatis-Plus vs JPA | MyBatis-Plus / JPA | open |
| Vector DB for RAG | Milvus / pgvector / in-memory | open |
| UI library | Element Plus / Naive UI | open |
| HyDE retrieval branch | Keep disabled / enable for eval only / enable for production with budget and trace | open |
| MCP protocol boundary | No MCP / internal trusted MCP / external MCP with strict auth and allowlist | ADR required |
| A2A protocol boundary | No A2A / POC only / cross-service Agent interoperability | ADR required |
| High-risk tool approval model | Extend Review Gate / create Tool Approval model / defer until external tools exist | open |

## Rejected Decisions

| Date | Proposal | Reason Rejected |
|---|---|---|
| — | — | — |

Formal ADRs are stored in `docs/decisions/`.
