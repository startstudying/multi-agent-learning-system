# EVIDENCE - RAG POC Context Builder

## 1. 范围

本证据覆盖 `wiki-rag` POC 思路迁移到现有 RAG 后端后的实现验证：

- `PocContextBuilder` 结构化上下文扩展。
- `RagQueryService` 回答使用扩展上下文。
- citations / `source_citation` 仍锚定原始命中源 chunk。
- `sourcesJson.pocContext` 只记录安全 metadata。

## 2. RED

Command：

```bash
cd backend && mvn test "-Dtest=PocContextBuilderTest,RagQueryServiceTest"
```

Result：失败，符合预期。

关键失败：

```text
找不到符号 类 PocContextBuilder
找不到符号 类 PocContextResult
```

说明：测试先于生产实现编写，并捕获了缺失的 POC context builder 能力。

## 3. GREEN / Focused Tests

Command：

```bash
cd backend && mvn test "-Dtest=PocContextBuilderTest,RagQueryServiceTest"
```

Result：PASS。

摘要：

```text
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

覆盖点：

- parent / adjacent / child 扩展。
- forbidden KB 与 documentVersion 二次过滤。
- RAG answer 使用 POC 上下文。
- citation 数量不因扩展上下文增加。
- `sourcesJson` 写入 `pocContext` 安全计数，不写扩展正文。
- 既有 no-source、reranker fallback、requestId replay、privacy guard 行为保持通过。

## 4. Adjacent Tests

Command：

```bash
cd backend && mvn test "-Dtest=AiQaControllerTest,QaRuntimeTest,MemoryContextServiceTest,RagEvaluationServiceTest,ChunkServiceVectorRetrievalTest"
```

Result：PASS。

摘要：

```text
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

覆盖点：

- AI QA controller/runtime 可以继续消费 RAG 结果。
- MemoryContextService citation context 不被 POC metadata 破坏。
- RAG evaluation metric path 保持稳定。
- Chunk vector retrieval allowed-KB filtering 保持稳定。

## 5. Compile

Command：

```bash
cd backend && mvn compile -q
```

Result：PASS。

## 6. 未执行项

- 未执行 full backend `mvn test`。
- 原因：本切片无 API、DB schema、依赖、frontend、model provider 或 VectorDB provider 改动；已执行 focused + adjacent + compile。
