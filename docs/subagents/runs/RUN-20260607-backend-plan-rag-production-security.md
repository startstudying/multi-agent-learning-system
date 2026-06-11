# P3-2 RAG 索引生产化安全边界审查报告

**审查角色**：安全与权限专家  
**审查日期**：2026-06-07  
**审查范围**：

- `backend/src/main/java/com/learningos/rag/**`
- `backend/src/main/java/com/learningos/knowledge/**`
- `backend/src/main/java/com/learningos/learning/**`
- `backend/src/main/java/com/learningos/agent/**`
- 关联 Flyway migration、`backend/pom.xml`、`backend/src/main/resources/application.yml`

**总体风险等级**：HIGH

结论：本轮 RAG 索引实现的核心安全边界总体可接受，未发现 chunk metadata 直接泄露原文、overlap 原文、storage key、hash 原料或 provider 原始错误；`chunkHash` 唯一索引限定在单文档版本内，未形成跨文档内容枚举索引；manual reindex 与 worker 处理复用同一 `IndexService` 路径，权限入口一致。但生产化门禁仍不能放行，主要原因是依赖审计发现 CRITICAL/HIGH 漏洞、上传 storage key 使用原始文件名、默认开发凭据仍存在生产误用风险，以及后续 parser/OCR/VectorDB 引入前必须补独立安全评审。

## 执行证据

| 检查项 | 结果 | 证据 |
|---|---|---|
| 代码范围定位 | 完成 | `rg --files backend/src/main/java/com/learningos/rag ...` |
| secrets 关键字扫描 | 完成，有默认开发凭据 | `application.yml` 默认 `DB_PASSWORD=learning_os`、MinIO `minioadmin`；未发现真实云 API key/私钥 |
| Java hardcoded secret 脚本 | 未完成 | `find-hardcoded-secrets.ps1` 本地脚本编码损坏，PowerShell 解析失败 |
| MyBatis `${}` 扫描 | 完成，无 SQL 注入证据 | 仅命中 `@Scheduled(fixedDelayString = "${...}")` 配置占位符 |
| dependency audit | 完成，有阻断项 | `mvn ... dependency-check-maven:check ... "-Danalyzer.assembly.enabled=false"` 成功生成 `backend/target/dependency-check-report.json` |
| git history secret scan | 未完成 | 当前 `D:\多元agent` 不是 git repository，`git status` 失败 |

## 风险清单

### 高风险

#### 1. 依赖审计发现 CRITICAL/HIGH 漏洞，生产门禁不应放行

**类别**：OWASP A06 Vulnerable and Outdated Components  
**位置**：`backend/pom.xml`，审计输出 `backend/target/dependency-check-report.json`  
**严重级别**：HIGH  
**利用条件**：取决于具体 CVE 触发面；Spring Boot/Tomcat/Netty 属于 HTTP 服务与底层网络栈，暴露面大。  
**影响范围**：后端 API、RAG 上传/查询、SSE、索引 worker 同一进程的运行安全。

依赖审计结果摘要：

- `spring-boot-3.5.7.jar`：CRITICAL/HIGH 多项，例如 `CVE-2026-40974`、`CVE-2026-40971`。
- `tomcat-embed-core-10.1.48.jar`：CRITICAL/HIGH 多项，例如 `CVE-2026-41293`、`CVE-2026-43512`、`CVE-2025-66614`。
- `netty-transport-4.1.128.Final.jar`：CRITICAL/HIGH 多项，例如 `CVE-2026-42581`、`CVE-2026-42579`。
- 另有 `spring-core`、`log4j-api`、`commons-lang3`、`kotlin-stdlib` MEDIUM/LOW 项。

**需要尽快修复**：

```xml
<!-- BAD: 生产门禁未闭环时固定在已审计出 CRITICAL/HIGH 的版本 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.7</version>
</parent>

<!-- GOOD: 升级到经 dependency-check 无 CRITICAL/HIGH 的 Spring Boot 维护版本，
     并用 dependencyManagement 覆盖 Tomcat/Netty 等传递依赖到修复版本。具体版本需以当次审计报告为准。 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>${approved.spring-boot.version}</version>
</parent>
```

修复验收要求：

- 重新运行 `dependency-check-maven`，CRITICAL/HIGH 为 0。
- 对 Tomcat/Netty/Spring Core 的传递依赖版本做显式记录。
- 若无法立即升级，需要在 `docs/security/` 创建例外说明、暴露面分析和临时缓解措施。

### 中风险

#### 2. MinIO / Noop storage key 包含原始文件名，可能在对象存储侧泄露 PII 或路径片段

**类别**：OWASP A02 Cryptographic Failures / Sensitive Data Exposure，A05 Security Misconfiguration  
**位置**：

- `backend/src/main/java/com/learningos/rag/storage/MinioDocumentStorageService.java:32-33`
- `backend/src/main/java/com/learningos/rag/storage/NoopDocumentStorageService.java:26-27`

**严重级别**：MEDIUM  
**利用条件**：攻击者需要对象存储控制台、bucket 列表权限、日志访问权，或通过未来接口暴露 `storageKey`。  
**影响范围**：上传文件名中的学生姓名、课程名、内部编号、路径片段可能进入 object key、对象存储审计日志或运维日志。

当前代码：

```java
String originalName = file.getOriginalFilename() == null ? "document.bin" : file.getOriginalFilename();
String key = kbId + "/" + UUID.randomUUID() + "/" + originalName;
```

建议修复：

```java
String extension = safeExtension(file.getOriginalFilename());
String key = kbId + "/" + UUID.randomUUID() + "/document" + extension;

private String safeExtension(String originalFilename) {
    if (originalFilename == null) {
        return ".bin";
    }
    String name = originalFilename.replace("\\", "/");
    int dot = name.lastIndexOf('.');
    if (dot < 0 || dot == name.length() - 1) {
        return ".bin";
    }
    String extension = name.substring(dot).toLowerCase(Locale.ROOT);
    return extension.matches("\\.(txt|md|markdown|pdf|docx)") ? extension : ".bin";
}
```

本轮接口 DTO 不返回 `storageBucket/storageKey`，所以当前不是直接远程泄露；但进入生产对象存储前应修。

#### 3. 文档上传缺少显式类型、大小和压缩炸弹限制，后续 parser/OCR 前会放大 DoS 风险

**类别**：OWASP A04 Insecure Design，A05 Security Misconfiguration  
**位置**：

- `backend/src/main/java/com/learningos/rag/api/DocumentController.java:30-43`
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java:64-129`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java:205-248`

**严重级别**：MEDIUM  
**利用条件**：有 KB 写权限的用户上传超大文件、畸形 PDF/DOCX、zip bomb 或 OCR 难例。  
**影响范围**：索引 worker CPU/内存、MinIO、数据库 chunk 表。

当前实现用 `MultipartFile` 直接存储，并在索引时 `readAllBytes()` / `zip.readAllBytes()`。本轮 parser 仍是轻量 Java 标准库解析，风险可控；但一旦加入 PDF parser、OCR、Office adapter，应先加资源上限和沙箱。

建议修复：

```java
private static final long MAX_UPLOAD_BYTES = 20L * 1024 * 1024;
private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "text/plain",
        "text/markdown",
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
);

private void validateUpload(MultipartFile file) {
    if (file == null || file.isEmpty()) {
        throw new ApiException(ErrorCode.VALIDATION_ERROR, "Document is required");
    }
    if (file.getSize() > MAX_UPLOAD_BYTES) {
        throw new ApiException(ErrorCode.VALIDATION_ERROR, "Document is too large");
    }
    if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
        throw new ApiException(ErrorCode.VALIDATION_ERROR, "Unsupported document type");
    }
}
```

#### 4. 默认开发凭据存在生产误用风险

**类别**：OWASP A05 Security Misconfiguration，A02 Cryptographic Failures  
**位置**：

- `backend/src/main/resources/application.yml:8-11`
- `backend/src/main/resources/application.yml:37-38`
- `backend/.env.example`

**严重级别**：MEDIUM  
**利用条件**：以默认配置部署生产或准生产环境。  
**影响范围**：MySQL、MinIO。

证据：

- 默认 DB password：`learning_os`
- 默认 MinIO access/secret：`minioadmin`
- `.env.example` 包含本地默认密码。

建议修复：

```java
@PostConstruct
void rejectDefaultProductionSecrets() {
    if ("prod".equals(appProperties.environment())
            && ("learning_os".equals(dataSourcePassword)
            || "minioadmin".equals(storageProperties.secretKey()))) {
        throw new IllegalStateException("Default development secrets are forbidden in production");
    }
}
```

### 低风险

#### 5. `recordRuntimeFailure(...)` 不强制脱敏，依赖调用方传入安全错误码

**类别**：OWASP A09 Security Logging and Monitoring Failures，A02 Sensitive Data Exposure  
**位置**：`backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java:174-205`  
**严重级别**：LOW  
**利用条件**：未来某个运行时失败路径把 provider 原始异常、prompt、storage key 或原文片段传入 `errorMessage`。  
**当前判断**：现有 RAG 索引失败通过 `IndexService.safeError(...)` 转换为安全码；模型失败用 `recordFailure(...)` 统一替换为 `MODEL_PROVIDER_ERROR`。因此本轮可接受，但应加约束避免未来回退。

建议修复：

```java
String safeRuntimeError = sanitizeModelError(errorMessage);
String failureSummary = summary + " Error: " + safeRuntimeError;
agentTask.setOutputJson("""
        {"status":"FAILED","summary":"%s","errorMessage":"%s","recoverable":true}
        """.formatted(escapeJson(summary), escapeJson(safeRuntimeError)).trim());
```

#### 6. SSE `sessionId` 未校验所属关系，但当前未按 session 读取持久化数据

**类别**：OWASP A01 Broken Access Control  
**位置**：`backend/src/main/java/com/learningos/rag/api/ChatController.java:65-91`  
**严重级别**：LOW  
**当前判断**：`sessionId` 只被回显在 `done` event，不用于读取 `kb_chat_session/kb_chat_message`。真正的数据访问仍由 `ragQueryService.query(...)` 的 KB 权限过滤保护。  
**后续风险**：一旦接入会话历史或消息回放，必须校验 `sessionId` owner。

建议修复：

```java
chatSessionService.requireOwner(userId, sessionId);
```

## 重点问题逐项结论

### 1. chunk metadata 是否泄露原文、overlap 原文、hash 原料、storage key 或 provider 错误

结论：本轮未发现直接泄露。

证据：

- `IndexService.metadataJson(...)` 仅写入 `parser`、`embeddingModel`、`contentLength`、`chunkingStrategy`、`chunkTokenCount`、`overlapTokenCount`、`headingLevel`、`headingPath`。
- metadata 未写入 chunk content、overlap content、hash input、`storageBucket`、`storageKey`、provider error。
- `IndexService.safeError(...)` 将索引异常映射为 `DOCUMENT_READ_FAILED`、`DOCUMENT_INDEX_FAILED`、`DOCUMENT_INDEX_UNEXPECTED_ERROR` 等安全码。

注意：`headingPath` 来自 Markdown 标题。标题本身可能包含敏感信息，例如学生姓名或内部项目代号。当前查询 citation 也可能暴露 `sectionTitle` 与 excerpt，这是 RAG 功能必要输出，但应只在 KB 权限过滤之后返回。

### 2. `chunkHash` 和唯一索引是否引入跨文档信息泄露或越权线索

结论：当前设计可接受，未发现跨文档去重侧信道。

证据：

- `IndexService.chunkHash(...)` 输入包含 `document.id`、`document.version`、`headingPath`、`content`。
- V17 唯一索引为 `(document_id, document_version, chunk_hash)`。
- 因为 hash 被 document scope 加盐式绑定，即使两个文档正文相同，也不会产生可直接比较的全局 hash。

残余风险：

- chunk 表仍保存明文 `content`，必须保证所有查询入口先做 KB 权限过滤。
- 后续 VectorDB 若用全局 collection + metadata filter，必须避免“先召回后过滤”导致日志、trace 或计分侧信道泄露。

### 3. worker/manual 索引路径是否存在权限边界不一致

结论：当前入口权限一致，处理路径一致。

证据：

- 上传入口 `DocumentService.upload(...)` 调用 `ensureCanWrite(userId, kbId)` 后创建文档和 index task。
- 手动 reindex `DocumentService.reindex(...)` 先加载文档，再 `ensureCanWrite(userId, document.getKbId())`。
- 任务详情 `DocumentService.getIndexTask(...)` 通过 `ensureCanRead(userId, task.getKbId())` 保护。
- worker 不接收用户输入 taskId，而是 `claimDuePendingTasks(...)` 消费已由受控入口创建的 `PENDING` 任务。
- manual 和 worker 最终都调用 `IndexService.processIndexTask(...)`。

残余风险：

- `IndexService.processIndexTask(String taskId)` 是 public service 方法，本身不做权限校验。当前仅由内部 service/scheduler 调用；未来若直接暴露调试接口，必须在 Controller/Application Service 层强制 `ensureCanWrite`。

### 4. 后续 parser adapter/OCR/VectorDB 可能引入的新安全风险

必须新增安全门：

1. Parser adapter / OCR：
   - 文件大小、页数、图片像素、压缩比、解压后大小上限。
   - 禁用外部实体、远程资源、宏、脚本、嵌入文件执行。
   - 解析超时、独立线程池、隔离临时目录、失败安全码。
   - 不把 parser 原始异常、文件路径、storage key、OCR provider 错误写入 DTO/trace。

2. VectorDB：
   - collection/namespace 必须按 tenant/course/KB 隔离，或强制服务端 metadata filter。
   - 禁止依赖 prompt 做权限控制。
   - 检索日志不得保存未授权候选 chunk、向量原文、storage key。
   - upsert/delete 必须绑定 `documentId/kbId/version`，避免旧 chunk 残留。

3. Embedding provider：
   - 原文会发送到外部模型服务，必须做数据出境/隐私评审。
   - provider error 必须映射安全码。
   - API key 只允许后端环境变量/密钥管理读取，不得进入前端或 trace。

4. Hybrid retrieval / reranker：
   - RRF/reranker 输入必须只包含已授权 KB 的候选。
   - 记录 reranker fallback 状态可以，但不得记录完整候选原文。

### 5. 本轮是否需要新 dependency review 或额外安全门

本轮没有新增实现依赖，但依赖审计已经发现阻断项，因此需要额外安全门：

- 需要 `docs/security/` 下的依赖整改/例外审查，记录 dependency-check 报告、升级计划、无法升级时的临时缓解措施。
- 后续只要引入 parser、OCR、tokenizer、VectorDB、reranker、embedding SDK，都必须新增 dependency review。
- 生产部署前必须补 CI dependency scan、secret scan、git history secret scan。

## OWASP Top 10 对照

| 类别 | 结论 |
|---|---|
| A01 Broken Access Control | RAG query、document、index task 已做 KB 读写权限；SSE session owner 校验仍是后续风险。 |
| A02 Cryptographic Failures | 未发现 API key 泄露；默认开发凭据与文件名进入 storage key 需整改。 |
| A03 Injection | 未发现 SQL 拼接；MyBatis `${}` 扫描只命中配置占位符。 |
| A04 Insecure Design | 后续 OCR/parser/VectorDB 前必须补资源限制、沙箱、namespace 权限设计。 |
| A05 Security Misconfiguration | 默认凭据、依赖漏洞、生产门禁缺口需要修复。 |
| A06 Vulnerable Components | 发现 CRITICAL/HIGH，生产阻断。 |
| A07 Identification/Auth Failures | 本范围依赖 `CurrentUserService`，完整 JWT/RBAC 仍为项目已知 P3 风险。 |
| A08 Data Integrity Failures | index task active dedup、row lock、unique hash 降低重复写入风险；VectorDB upsert/delete 仍待设计。 |
| A09 Logging/Monitoring | 索引错误安全码可接受；`recordRuntimeFailure` 后续应统一脱敏。 |
| A10 SSRF | 当前未见用户 URL 抓取；后续 parser 若支持远程 URL 或外部资源必须默认禁用。 |

## 需要尽快修复的点

1. 修复 dependency-check 报告中的 CRITICAL/HIGH，或形成正式安全例外与缓解方案。
2. storage object key 不再拼接原始文件名，只保留白名单扩展名。
3. 上传入口增加文件大小、类型、扩展名和空文件校验。
4. 生产环境启动时拒绝默认 DB/MinIO 凭据。
5. 为 parser/OCR/VectorDB/embedding/reranker 后续切片建立 dependency review + data boundary review。

## 本轮可接受的残余风险

- `chunkHash` 由 SHA-256 生成且限定在文档版本内，当前不需要加密或 HMAC。
- `metadataJson.headingPath` 可能包含标题文本，但只在数据库内部和授权查询/citation 内使用，本轮可接受。
- `IndexTaskDetailResponse.errorMessage` 当前只保存安全错误码，本轮可接受。
- manual 和 worker 复用同一索引处理路径，worker 不直接接收用户 taskId，本轮可接受。
- `git history secret scan` 因当前目录不是 git repo 未完成，本报告不声明历史无泄露。

## 对下一步切片的安全建议

1. 切 parser/OCR 前，先写 `docs/security/` dependency review，明确库版本、许可证、维护状态、CVE、沙箱策略、超时和资源限制。
2. 切 VectorDB 前，先设计 namespace/metadata filter/删除一致性，并写越权测试：用户 A 上传、用户 B 检索、混合 forbidden `kbIds`、旧版本 chunk 残留。
3. 切 embedding provider 前，补数据出境说明、API key 管理、provider error 清洗、模型调用日志字段白名单。
4. 切 hybrid retrieval/reranker 前，强制“授权 KB 过滤在召回前或同层完成”，禁止把未授权候选交给 reranker。
5. 给索引任务补安全测试：原始文件名不进入 `storageKey`、畸形文档失败只返回安全码、索引任务详情不暴露 bucket/key/provider error。

## 最终判定

代码边界：P3-2 当前 RAG 索引 metadata/hash/worker/manual 权限边界可作为下一步设计基础。  
生产门禁：不通过。依赖漏洞和上传/parser/存储边界需要在进入真实生产 parser/OCR/VectorDB 前关闭。
