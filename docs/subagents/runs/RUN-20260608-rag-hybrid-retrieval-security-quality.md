# RUN-20260608 P3-2 Hybrid retrieval/RRF/reranker timeout fallback 安全与质量边界审查

**角色**：Security & Quality 子代理  
**日期**：2026-06-08  
**范围**：只读审查 P3-2 Hybrid retrieval / RRF / reranker timeout fallback 最小切片的安全与质量边界。  
**允许写入**：仅本报告文件。未修改业务代码。  

## 审查依据

- 项目记忆：`docs/memory/PROJECT_MEMORY.md`
- 技能注册：`docs/skills/SKILL_REGISTRY.md`
- 架构基线：`docs/architecture/ARCHITECTURE_BASELINE.md`
- 架构漂移检查：`docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- RAG 架构：`docs/architecture/rag-architecture.md`
- 相关实现：
  - `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
  - `backend/src/main/java/com/learningos/rag/application/ChunkService.java`
  - `backend/src/main/java/com/learningos/rag/application/RerankerService.java`
  - `backend/src/main/java/com/learningos/rag/application/PermissionService.java`
  - `backend/src/main/java/com/learningos/rag/repository/KbDocChunkRepository.java`
  - `backend/src/main/java/com/learningos/rag/domain/KbQueryLog.java`
  - `backend/src/main/java/com/learningos/rag/api/dto/RagQueryDtos.java`
  - `backend/src/main/resources/application.yml`
  - `backend/pom.xml`
- 相关测试：
  - `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java`
  - `backend/src/test/java/com/learningos/orchestrator/api/OrchestratorWorkflowControllerTest.java`

## 审查结论

**总体风险等级：MEDIUM。**

当前代码已具备关键权限前置基础：`RagQueryService` 在检索前调用 `PermissionService.requireReadableKbIds(...)`，`ChunkService.retrieveAllowedChunks(...)` 只接收 allowed KB ids，`KbDocChunkRepository.findTop20ByKbIdInOrderByCreatedAtDesc(...)` 以 `kbId in (...)` 查询 chunk。现有测试覆盖 forbidden / mixed allowed+forbidden 请求不写 `kb_query_log` 与 `source_citation`。

但 P3-2 新切片一旦引入 hybrid/vector/RRF/reranker，就必须避免把“先召回、后过滤”带入新链路；同时当前 query log 仍持久化 `question`、`responseJson`、`sourcesJson` 中的 answer/excerpt，属于最需要明确边界的敏感面。

依赖审计已尝试执行：

```powershell
cd backend
mvn -q org.owasp:dependency-check-maven:check -DskipTests -Dformat=JSON -DfailBuildOnCVSS=11
```

结果：失败，原因是 RetireJS 数据源下载 `jsrepository.json` 时连接重置。不能声明依赖审计通过；本切片若不新增依赖，应保持 `backend/pom.xml` 不变，并在最终 Evidence 中记录“不新增依赖”。如新增 reranker/vector/embedding 依赖，必须先走 `docs/security/` 依赖评审。

## 1) 必守边界

### B1. KB 权限过滤必须先于任何检索、融合、重排

**必须**：

1. 在 `RagQueryService` 或等价 Application Service 层先调用 `PermissionService.requireReadableKbIds(userId, requestedKbIds)`。
2. keyword retrieval、vector retrieval、hybrid retrieval、RRF merge、reranker 的输入候选都只能来自 allowed KB ids。
3. 即使 VectorDB 返回了 forbidden `chunkId/documentId/kbId`，Service 层也必须二次校验并丢弃，且不得写入 sources/query log/citation。
4. mixed allowed+forbidden `kbIds` 必须整体 `FORBIDDEN`，不能静默过滤后继续检索。

**当前证据**：

- `RagQueryService.java:163-171`：安全检查与 `requireReadableKbIds` 在 `queryResolved(...)` 之前。
- `RagQueryService.java:200-201`：先取 allowed candidates，再传入 reranker。
- `PermissionService.java:68-82`：存在 denied KB 时抛 `FORBIDDEN`。
- `ChunkService.java:18-24`：空 allowed 返回空；非空只按 allowed KB ids 查询。
- `KbDocChunkRepository.java:13`：`findTop20ByKbIdInOrderByCreatedAtDesc(Collection<String> allowedKbIds)`。
- `RagQueryServiceTest.java` 覆盖 forbidden 与 mixed allowed+forbidden 不写 query/citation artifacts。

**禁止**：

```java
// BAD: 先从全局 vector index 召回，再在 Java 内过滤，可能在日志/score/trace 中泄露 forbidden candidates
List<VectorHit> hits = vectorStore.search(question, topK);
List<VectorHit> allowed = hits.stream()
        .filter(hit -> allowedKbIds.contains(hit.kbId()))
        .toList();
```

**推荐**：

```java
// GOOD: 检索接口自身接收 allowed scope；返回后再做防御性校验
List<VectorHit> hits = vectorStore.search(new VectorSearchRequest(
        question,
        allowedKbIds,
        candidateLimit
));

List<VectorHit> safeHits = hits.stream()
        .filter(hit -> allowedKbIds.contains(hit.kbId()))
        .toList();
```

### B2. 不新增依赖、不改 DB schema，除非先完成安全/依赖评审

本最小切片应优先使用现有 Java/Spring 能力完成：

- RRF 纯 Java ranker。
- Fake/no-op reranker 或本地 deterministic reranker。
- timeout fallback 用 `CompletableFuture` / `ExecutorService` / Spring 配置完成。

若新增：

- embedding SDK
- VectorDB client
- external reranker SDK
- tokenizer / search engine dependency

必须先创建 `docs/security/` 依赖审查，记录 license、CVE、维护状态、替代方案、数据出境与降级策略，并更新 PLAN/TASK 明确批准。

### B3. 不越过 Service 层

必须保持：

```text
Controller -> Application Service -> RAG retrieval/reranker service -> Repository/adapter
Agent Tool -> Service -> Repository/adapter
```

禁止：

- Controller 直接调用 Repository/Mapper。
- Agent Tool 直接访问 Repository/Mapper。
- Reranker/Vector adapter 自行读取用户身份并绕过 `PermissionService`。
- Prompt 控制权限。

### B4. `query log`、`sourcesJson`、`rerankerStatus` 必须最小化、白名单化

当前 `KbQueryLog` 设计包括：

- `question`：原问题，`RagQueryService.java:247`
- `responseJson`：requestId 场景保存完整 response，`RagQueryService.java:250`
- `rerankerStatus`：当前写 `retrieval.strategy()`，`RagQueryService.java:252`
- `sourcesJson`：保存 `retrieval` + `sources`，其中 `sources.excerpt` 来自 chunk 截断，`RagQueryService.java:253`

本切片必须确保新增字段/状态不写入：

- raw prompt
- raw chunk 全文
- raw reranker request/response
- raw provider error
- secret / token / API key
- forbidden candidate ids/content
- 未授权 KB、document、chunk metadata

`rerankerStatus` 必须是固定枚举，不得保存异常消息或 provider 返回文本。

建议稳定枚举：

```java
enum RerankerStatus {
    NOT_CONFIGURED,
    SKIPPED_NO_CANDIDATES,
    SUCCEEDED,
    TIMEOUT_FALLBACK,
    ERROR_FALLBACK
}
```

### B5. fallback 错误码和降级语义必须稳定

Reranker 超时/异常不应导致 RAG 查询整体失败，除非检索前置权限或安全校验失败。

推荐行为：

| 场景 | HTTP/API code | retrieval.strategy | downgraded | rerankerStatus |
|---|---|---|---|---|
| 无候选 | `OK` | `NO_SOURCE_REFUSAL` | `true` | `SKIPPED_NO_CANDIDATES` |
| reranker 成功 | `OK` | `HYBRID_RRF_RERANKED` | `false` | `SUCCEEDED` |
| reranker timeout | `OK` | `HYBRID_RRF_FALLBACK` | `true` | `TIMEOUT_FALLBACK` |
| reranker exception | `OK` | `HYBRID_RRF_FALLBACK` | `true` | `ERROR_FALLBACK` |
| forbidden KB | `FORBIDDEN` | 不写成功 metadata | 不适用 | 不写 query/citation |
| unsafe question | `VALIDATION_ERROR` 或既有安全错误码 | 不写成功 metadata | 不适用 | 不写 query/citation |

## 2) 风险点

### R1. “先召回、后过滤”导致越权候选泄露

**严重性：HIGH**  
**类别：OWASP A01 Broken Access Control**  
**位置/证据**：未来 Hybrid/Vector/RRF 实现点；当前安全路径在 `RagQueryService.java:163-201`。  
**风险**：VectorDB 或 hybrid retriever 若先返回全局 candidates，forbidden chunk 可能进入 RRF score、reranker request、debug log、metrics、trace 或 query log。即使最终 response 过滤，仍可能通过排序差异、latency、candidateCount、logs 泄露存在性。

### R2. `kb_query_log.question` 与 `responseJson/sourcesJson` 持久化敏感学习问题和 chunk excerpt

**严重性：MEDIUM**  
**类别：OWASP A02 Sensitive Data Exposure / A09 Logging and Monitoring**  
**位置**：`RagQueryService.java:247,250,253`，`KbQueryLog.java`。  
**风险**：学生问题可能包含个人信息、考试内容、内部材料；`sourcesJson` 与 `responseJson` 可能包含 chunk excerpt/answer。当前这是既有设计，但 hybrid/reranker 切片不得扩大敏感面，例如不得记录 raw reranker prompt、候选全文列表、provider error。

建议后续单独治理：

```java
// GOOD: query log 中保存 hash/长度/计数/枚举，不保存 raw prompt/provider request
record.setQuestionHash(sha256(normalizedQuestion));
record.setQuestionLength(normalizedQuestion.length());
record.setRerankerStatus(rerankerStatus.name());
record.setSourcesJson(toJson(sanitizedSourceMetadataOnly));
```

### R3. `rerankerStatus` 当前复用 retrieval strategy，未来可能混淆“策略”和“失败状态”

**严重性：MEDIUM**  
**类别：OWASP A09 / Quality contract stability**  
**位置**：`RagQueryService.java:252`。  
**风险**：当前 `rerankerStatus` 写入 `COURSE_RAG` / `NO_SOURCE_REFUSAL` / `RAG_WITH_PROFILE` 等 strategy。真正接入 reranker timeout 后，如果继续复用，会无法稳定表达 `TIMEOUT_FALLBACK`、`ERROR_FALLBACK`，也不利于测试和运维告警。

### R4. `RerankerService` 当前为 passthrough，占位实现尚未验证 timeout/fallback

**严重性：MEDIUM**  
**类别：OWASP A04 Insecure Design / Reliability**  
**位置**：`RerankerService.java:8-12`。  
**风险**：一旦接入外部 reranker，没有 timeout、异常隔离、输入脱敏和固定 fallback，可能阻塞 RAG_QA 工作流或泄露候选原文。

### R5. `topK` 只有下限默认，没有显式全局上限

**严重性：LOW-MEDIUM**  
**类别：OWASP A04 Insecure Design / DoS**  
**位置**：`RagQueryService.java:170,327`，`ChunkService.java:22-24`。  
**当前缓解**：Repository 方法固定 `findTop20...`，实际最多 20。  
**风险**：Hybrid/vector 后如果候选池扩大，未设置 `maxTopK/maxCandidates` 会放大 DB/VectorDB/reranker 请求成本。必须在 service 层配置硬上限。

### R6. 依赖审计未完成，不能声明依赖安全通过

**严重性：MEDIUM**  
**类别：OWASP A06 Vulnerable and Outdated Components**  
**位置**：`backend/pom.xml`。  
**证据**：`dependency-check-maven:check` 因 RetireJS 下载连接重置失败。  
**要求**：本切片不新增依赖；若新增依赖，必须先完成 dependency review 和可重复审计。

## 3) 验收检查清单

### 权限与访问控制

- [ ] `requestedKbIds` 先经 `PermissionService.requireReadableKbIds(...)` 校验。
- [ ] mixed allowed+forbidden KB 请求整体返回 `FORBIDDEN`。
- [ ] forbidden 请求不写 `kb_query_log`。
- [ ] forbidden 请求不写 `source_citation`。
- [ ] keyword retriever 只查询 allowed KB。
- [ ] vector retriever 请求携带 allowed KB scope，且返回后做二次校验。
- [ ] RRF merge 输入不包含 forbidden candidates。
- [ ] reranker 输入不包含 forbidden candidates。
- [ ] response `sources` 不包含 forbidden document/chunk。

### 敏感数据与日志

- [ ] `query log` 不新增 raw prompt 字段。
- [ ] `sourcesJson` 不保存 raw chunk 全文，只允许必要 source metadata/excerpt。
- [ ] `rerankerStatus` 为固定枚举，不包含异常消息/provider 文本。
- [ ] metrics tags 不包含 `question`、`prompt`、`answer`、`excerpt`、`sourcesJson`、`responseJson`、`kbId`、`documentId`、`chunkId`、`userId`、`traceId`。
- [ ] trace/failure evidence 不包含 raw question、raw chunk、raw provider error、secret。
- [ ] replay snapshot 不扩大敏感字段范围。

### 架构边界

- [ ] Controller 只调用 Service。
- [ ] Agent/Tool 不直接访问 Mapper/Repository。
- [ ] RRF/reranker/hybrid retrieval 位于 RAG Service/adapter 边界内。
- [ ] 不新增 DB schema；如新增，SPEC/PLAN/TASK/migration/MySQL smoke 同步更新。
- [ ] 不新增依赖；如新增，`docs/security/` 依赖评审完成并被 PLAN/TASK 批准。

### fallback 合同

- [ ] reranker timeout 返回成功 RAG response，而不是 500。
- [ ] timeout fallback 保留 citations 或 NO_SOURCE 语义。
- [ ] timeout fallback 写稳定 `rerankerStatus=TIMEOUT_FALLBACK`。
- [ ] exception fallback 写稳定 `rerankerStatus=ERROR_FALLBACK`。
- [ ] no-source 写稳定 `NO_SOURCE_REFUSAL` / `SKIPPED_NO_CANDIDATES`。
- [ ] forbidden/validation/safety 失败不写成功 query/citation artifacts。

### 测试与证据

- [ ] 新增/更新 `RagQueryServiceTest` 覆盖 hybrid/RRF/reranker fallback。
- [ ] 新增 RRF 纯函数单测。
- [ ] 新增 fake slow reranker timeout 单测。
- [ ] 新增 forbidden vector hit 被丢弃且不写 citation 的集成测试。
- [ ] 新增 query log 脱敏/枚举断言。
- [ ] 运行 `mvn test` 或说明无法运行原因。
- [ ] 依赖审计成功，或明确记录失败原因与“不新增依赖”。

## 4) 建议安全测试

### T1. mixed allowed+forbidden 必须在检索前失败

```java
@Test
void hybridRetrievalRejectsMixedAllowedAndForbiddenKbIdsBeforeRetrieval() {
    seedIndexedChunk("kb_allowed", "doc_allowed", "Allowed course chunk.");
    seedPrivateKnowledgeBase("kb_hidden", "bob");

    assertThatThrownBy(() -> ragQueryService.query(
            "alice",
            List.of("kb_allowed", "kb_hidden"),
            "Explain joins",
            5
    ))
            .isInstanceOf(ApiException.class)
            .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

    assertThat(queryLogRepository.count()).isZero();
    assertThat(sourceCitationRepository.count()).isZero();
}
```

### T2. VectorDB 返回 forbidden hit 时必须二次过滤

```java
@Test
void dropsForbiddenVectorHitsBeforeRrfAndReranker() {
    fakeVectorStore.returnHits(
            hit("chunk_allowed", "kb_allowed"),
            hit("chunk_hidden", "kb_hidden")
    );

    RagQueryResponse response = ragQueryService.query("alice", List.of("kb_allowed"), "Explain joins", 5);

    assertThat(response.sources())
            .extracting(SourceCitation::documentId)
            .doesNotContain("doc_hidden");
    assertThat(fakeReranker.receivedChunkIds()).doesNotContain("chunk_hidden");
    assertThat(serializedQueryLog()).doesNotContain("chunk_hidden", "kb_hidden", "Hidden course chunk");
}
```

### T3. reranker timeout fallback 不失败、不泄露

```java
@Test
void rerankerTimeoutFallsBackWithStableStatusAndNoRawError() {
    fakeReranker.timeout();

    RagQueryResponse response = ragQueryService.query("alice", List.of("kb_sql"), "Explain joins", 5);

    assertThat(response.retrieval().downgraded()).isTrue();
    assertThat(response.retrieval().strategy()).isEqualTo("HYBRID_RRF_FALLBACK");

    KbQueryLog log = queryLogRepository.findAll().getFirst();
    var fields = new DirectFieldAccessor(log);
    assertThat(fields.getPropertyValue("rerankerStatus")).isEqualTo("TIMEOUT_FALLBACK");
    assertThat((String) fields.getPropertyValue("sourcesJson"))
            .doesNotContain("TimeoutException", "rawPrompt", "apiKey", "secret");
}
```

### T4. reranker exception fallback 不写 provider 原始错误

```java
@Test
void rerankerProviderErrorIsSanitizedInQueryLog() {
    fakeReranker.failWith("provider said apiKey=sk-test raw chunk: very sensitive");

    RagQueryResponse response = ragQueryService.query("alice", List.of("kb_sql"), "Explain joins", 5);

    assertThat(response.retrieval().downgraded()).isTrue();
    String logJson = serializedQueryLog();
    assertThat(logJson)
            .contains("ERROR_FALLBACK")
            .doesNotContain("sk-test", "raw chunk", "provider said");
}
```

### T5. query log 脱敏字段白名单

```java
@Test
void queryLogDoesNotPersistRerankerPromptOrFullCandidateChunks() {
    ragQueryService.query("alice", List.of("kb_sql"), "My private learning question", 5);

    String logJson = serializedQueryLog();
    assertThat(logJson)
            .doesNotContain("rerankerPrompt", "candidateRawText", "systemPrompt", "apiKey", "secret");
}
```

### T6. metrics tag 不包含敏感/高基数字段

```java
@Test
void ragMetricsDoNotUseSensitiveTagsForHybridFallback() {
    ragQueryService.query("alice", List.of("kb_sql"), "Explain joins", 5);

    assertThat(meterRegistry.getMeters().stream()
            .map(Meter::getId)
            .flatMap(id -> id.getTags().stream())
            .map(Tag::getKey))
            .doesNotContain("question", "prompt", "answer", "excerpt", "sourcesJson",
                    "responseJson", "kbId", "documentId", "chunkId", "userId", "traceId");
}
```

## OWASP 对照

| 类别 | 本切片关注点 | 结论 |
|---|---|---|
| A01 Broken Access Control | KB 权限必须先于 retrieval/RRF/reranker | 当前基础可接受；新增 vector/hybrid 时需二次过滤测试 |
| A02 Cryptographic Failures / Sensitive Data | question、sourcesJson、responseJson、prompt/secret 泄露 | MEDIUM；不得扩大 query log 敏感面 |
| A03 Injection | RAG query 当前 JPA derived query，无 SQL 拼接 | 未发现新增注入证据 |
| A04 Insecure Design | timeout、fallback、topK/candidate 上限 | 需明确稳定 fallback 合同 |
| A05 Security Misconfiguration | reranker timeout 配置、默认依赖 | 需固定配置与安全默认值 |
| A06 Vulnerable Components | dependency audit 未成功 | 不能声明通过；本切片不得新增依赖 |
| A07 Identification/Auth Failures | 依赖 `CurrentUserService` 与临时 auth | 非本切片主范围 |
| A08 Data Integrity Failures | query replay、source citation 一致性 | forbidden/冲突已有测试；hybrid 后需补充 |
| A09 Logging/Monitoring | query log、metrics、trace 脱敏 | 需新增负向断言 |
| A10 SSRF | 当前未见 URL 抓取 | 若外部 reranker/vector endpoint 可配置，需防止日志泄露 endpoint/secret |

## 最终建议

本最小切片可以继续设计/实现，但必须把以下项作为硬验收门槛：

1. **权限前置不可变**：allowed KB ids 必须在 keyword/vector/RRF/reranker 前确定。
2. **降级不泄露**：timeout/error fallback 只写稳定枚举和计数，不写 raw error/prompt/chunk。
3. **不新增依赖/不改 schema**：若无法做到，先补 dependency review 与 SPEC/PLAN/TASK。
4. **测试先行**：至少覆盖越权、forbidden vector hit、reranker timeout、error fallback、query log 脱敏、metrics tag 脱敏。
5. **Evidence 如实记录 dependency audit**：本次审计命令因网络失败，不能作为通过证据。
