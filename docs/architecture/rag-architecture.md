# Course RAG Architecture

Course RAG is both a standalone document QA subsystem and the evidence layer for the Course RAG Agent. The current implementation establishes the controller, permission, persistence, citation, and SSE contracts; parser, embedding, vector retrieval, and reranker adapters are the next hardening steps.

## Layers

```text
Vue document/chat UI
-> KnowledgeBaseController / DocumentController / ChatController
-> PermissionService
-> DocumentService / IndexService / ChunkService / RagQueryService / RerankerService
-> JPA repositories
-> MySQL 8 metadata and text chunks
-> MinIO source documents
-> Redis cache/progress, optional
-> VectorDB adapter, optional
```

MySQL 8 is the primary database. Vector retrieval is optional and should be implemented as an adapter, for example Milvus, Chroma, Elasticsearch/OpenSearch vector search, or pgvector in a separate evaluated deployment. Do not reintroduce PostgreSQL as the default application store.

## Offline Indexing Pipeline

```text
upload document
-> store original object in MinIO
-> create kb_document
-> create kb_index_task
-> parse source format
-> clean and chunk text
-> persist kb_doc_chunk rows with metadata
-> optionally embed chunks into Redis cache / VectorDB
-> update index task status
```

Current code creates document metadata and pending index tasks. Production indexing still needs real parsers for PDF, DOCX, Markdown, and TXT, plus OCR fallback for scanned PDFs.

Required chunk metadata:

- `kb_id`
- `document_id`
- `document_version`
- `chunk_index`
- `page_num`
- `section_title`
- metadata JSON
- source text excerpt

`kb_doc_chunk.kb_id` is intentionally redundant so retrieval can hard-filter by allowed KB ids without joining through documents.

## Online Query Pipeline

```text
user question
-> safety check
-> permission filter requested kbIds
-> retrieve allowed chunks
-> optional lexical/vector hybrid retrieval
-> optional RRF fusion
-> optional reranker with timeout fallback
-> context trimming
-> grounded answer generation
-> citations + traceId
-> kb_query_log
```

The current backend uses deterministic retrieval over stored chunks and returns a grounded answer from retrieved content. It already persists query evidence to `kb_query_log`.

## Retrieval Requirements

- Permission filtering must happen before chunk retrieval.
- Every retrieval query must include `kb_id in allowedKbIds`.
- Prompt instructions are not an authorization mechanism.
- A missing retrieval result should produce an explicit "not found in course material" answer.
- Reranker failure must fall back to the fused TopN candidates.

## Citation Contract

RAG answers return:

```json
{
  "answer": "...",
  "sources": [
    {
      "documentId": "doc_001",
      "documentName": "database-course.md",
      "pageNum": 12,
      "sectionTitle": "Multi table joins",
      "excerpt": "JOIN will combine every matching child row...",
      "score": 0.91
    }
  ],
  "traceId": "trc_001"
}
```

Frontend citation viewers should show document name, page, section, excerpt, score, and trace id. Backend generation should never hide citations from the API response.

## SSE Contract

`GET /api/chat/sessions/{sessionId}/stream?question=...&kbIds=...` emits:

- `status`: query rewrite, retrieving, reranking, generating.
- `token`: streamed answer text.
- `done`: sources, trace id, latency, session id.

Because browser `EventSource` cannot send custom headers, production auth should use cookie/session auth or a signed stream token. The current local dev path falls back to the backend default user when no header is present.

## Storage Responsibilities

- MySQL 8: KB metadata, permissions, document metadata, chunks, index tasks, chat/query logs, citations.
- MinIO: original documents and later generated media assets.
- Redis: optional embedding cache, index progress cache, short-lived query cache, stream coordination.
- Optional VectorDB: dense embeddings and vector search only.

## Hardening Backlog

1. Real parser/chunker pipeline with document preview validation.
2. Embedding service through Spring AI/Spring AI Alibaba.
3. Optional VectorDB adapter behind a retrieval interface.
4. Hybrid lexical/vector retrieval and RRF.
5. Reranker with timeout and fallback.
6. Context budget trimming.
7. Citation persistence in `source_citation`.
8. RAG evaluation tests for citation accuracy, refusal behavior, permission leaks, and answer quality.

