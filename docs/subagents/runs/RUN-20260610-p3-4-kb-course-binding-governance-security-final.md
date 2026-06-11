# P3-4 RAG KB-course binding governance 安全审查报告

**角色:** Security/Quality Expert  
**日期:** 2026-06-10  
**范围:** `PermissionService.java`、`DocumentService.java`、`KnowledgeBaseService.java`、`RagQueryService.java` 以及 RAG KB/document/query 相关控制器、迁移和测试。  
**结论:** 未发现 BOUND KB 可通过 PUBLIC / owner / explicit permission 绕过 `CourseAccessService` 的直接越权路径；发现 1 个中风险并发绑定治理缺口、1 个低风险幂等契约回归。  
**总体风险等级:** MEDIUM。

## Skill Selection Gate

1. Task type: Security/Quality review, authorization and fail-closed verification.
2. Selected skills: `security-review`, `java-security-review`.
3. Why: 本任务审查 Java/Spring Boot RAG 权限路径、OWASP A01 Broken Access Control、秘密扫描和依赖审计。
4. Missing skills: 无。
5. GitHub research needed: 否，判断依赖本仓库权限实现和测试证据。
6. New project-specific skill: 暂不需要。

## 审查与验证

- 代码只读审查: 已完成。
- 秘密扫描: 已对 `backend/src/main/java/com/learningos/rag`、`backend/src/test/java/com/learningos/rag`、`backend/pom.xml` 扫描 `api_key/password/secret/token/private key` 等模式。命中均为测试用假 JWT secret、敏感错误脱敏测试字符串或配置属性读取，未发现生产硬编码密钥。
- Git 历史秘密扫描: 未完成。`git log -p` 失败，当前 `D:\多元agent` 不含 `.git` 元数据。
- 依赖审计: `mvn org.owasp:dependency-check-maven:check` 在 `backend` 目录执行失败，原因是 RetireJS 仓库下载连接重置且本地 dependency-check 数据为空，不能给出 CVE 通过结论。
- 依赖树核验: `mvn dependency:tree "-Dincludes=org.springframework.security:*,org.apache.pdfbox:*,org.apache.poi:*,io.jsonwebtoken:*,com.mysql:mysql-connector-j,org.flywaydb:*"` 成功，版本包括 Spring Security 6.5.6、Flyway 11.7.2、MySQL Connector/J 9.4.0、PDFBox 3.0.7、POI 5.5.1。
- Focused tests: `mvn -q "-Dtest=PermissionServiceTest,RagQueryServiceTest,DocumentControllerTest,KnowledgeBaseControllerTest" test` 执行，56 个测试中 1 个失败：`DocumentControllerTest.rejectsSameRequestIdWhenCourseOrChapterMetadataChanges` 期望 409，实际 400。

## 发现

### 1. UNBOUND KB 自动绑定缺少 KB 行级锁，并发上传可能造成 KB/document 课程不一致

**Severity:** MEDIUM  
**Category:** OWASP A01 Broken Access Control / A04 Insecure Design  
**Location:** `backend/src/main/java/com/learningos/rag/application/DocumentService.java:94`, `backend/src/main/java/com/learningos/rag/application/DocumentService.java:197`, `backend/src/main/java/com/learningos/rag/application/DocumentService.java:217`, `backend/src/main/java/com/learningos/rag/application/DocumentService.java:226`  
**Evidence:** `KnowledgeBaseRepository` 仅有普通 `findById` 和 `findByIdInAndDeletedAtIsNull`，没有 `@Lock(PESSIMISTIC_WRITE)` 的 KB 加载方法；`DocumentService.upload` 先用普通 `findById(kbId)` 加载 KB，再在 `resolveEffectiveCourseScope` 对 UNBOUND KB 通过 `existsByKbIdAndDeletedAtIsNull` 判断是否已有文档，最后 `bindKnowledgeBase` 保存绑定。

**Exploitability:** 需要对同一个 UNBOUND KB 有写权限的并发请求；通常是同一 owner 或管理员/教师并发操作，不是匿名远程绕过。  
**Blast Radius:** 可能出现 KB 最终绑定到 course B，但已写入 course A 文档，或两个课程文档进入同一 KB。之后权限按 KB 的单一 `course_id` 判定，可能导致课程 B 授权用户读取 course A 文档内容，形成跨课程数据泄露。

**Issue:** 当前单请求路径是 fail-closed 的，但“判断 UNBOUND + 没有文档 + 绑定 + 写文档”不是在 KB 行锁下完成。已有索引任务对 document/task 使用了 `PESSIMISTIC_WRITE`，但 KB 绑定生命周期没有同等锁保护。

**Recommendation:** 为 KB 绑定入口增加 `findByIdAndDeletedAtIsNullForUpdate`，在上传事务内锁定 KB 行后再判断 binding status、已有文档和绑定状态；或增加数据库级约束/唯一治理，确保同一 KB 不可能产生多课程 active document。示例：

```java
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select kb from KnowledgeBase kb where kb.id = :id and kb.deletedAt is null")
    Optional<KnowledgeBase> findByIdAndDeletedAtIsNullForUpdate(@Param("id") String id);
}

@Transactional
public DocumentUploadResponse upload(...) {
    KnowledgeBase knowledgeBase = knowledgeBaseRepository.findByIdAndDeletedAtIsNullForUpdate(kbId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Knowledge base not found"));
    ensureCanWrite(userId, currentUserAdmin, currentUserTeacher, kbId);
    // resolveEffectiveCourseScope now runs while the KB row is locked.
}
```

### 2. 同一 requestId 改变 course/chapter 元数据时返回 400 而非 409

**Severity:** LOW  
**Category:** OWASP A04 Insecure Design / idempotency contract  
**Location:** `backend/src/main/java/com/learningos/rag/application/DocumentService.java:101`, `backend/src/main/java/com/learningos/rag/application/DocumentService.java:125`, `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java:361`  
**Evidence:** Focused tests 中 `DocumentControllerTest.rejectsSameRequestIdWhenCourseOrChapterMetadataChanges` 失败：期望 HTTP 409 / `CONFLICT`，实际 HTTP 400 / `VALIDATION_ERROR`。代码先解析 KB 绑定和请求课程一致性，再计算 request hash/replay，因此第二次相同 requestId 但不同 course/chapter 时，被 “Document course must match knowledge base course” 提前截获。

**Exploitability:** 已 fail-closed，不会产生第二份文档或越权读取。  
**Blast Radius:** 安全影响低，主要是幂等语义和客户端错误处理不一致；可能削弱“同 requestId 不同 payload 一律 409”的审计证据。

**Recommendation:** 若产品契约要求 idempotency conflict 优先级，应在 requestId 存在时先按 `(createdBy, requestId)` 查已有上传并比较原始/规范化 payload，再执行会受当前 KB 绑定状态影响的 course mismatch 校验；或调整测试/契约明确 400 是可接受的 fail-closed 结果。示例：

```java
String normalizedRequestId = normalizeOptionalRequestId(requestId);
if (normalizedRequestId != null) {
    ExistingUploadPayload existing = documentRepository.findByCreatedByAndRequestId(userId, normalizedRequestId)
            .map(this::toExistingPayload)
            .orElse(null);
    if (existing != null && !existing.matches(requestedPayload)) {
        throw new ApiException(ErrorCode.CONFLICT, "requestId already used with different document upload payload");
    }
}
// then resolve KB binding / course scope and create or replay.
```

## 已核验通过的防绕过证据

- `PermissionService` 对 read/write 都先处理 admin、`CONFLICTED`、`BOUND`，只有 `UNBOUND` 才落到 owner/PUBLIC/explicit permission：`backend/src/main/java/com/learningos/rag/application/PermissionService.java:206`, `:209`, `:213`, `:216`, `:235`, `:238`, `:242`, `:245`。
- BOUND read 进入 `CourseAccessService.requireCourseRead`，缺少 `courseId` 或 `ApiException` 均返回 false：`PermissionService.java:249`、`:255`、`:259`、`:261`。
- BOUND write 先 `requireCourseRead` 再 `requireCourseManage`，非课程管理者不能通过 owner/WRITE permission 写入：`PermissionService.java:266`、`:276`、`:282`。
- RAG query 和 replay 均用 `permissionService.requireReadableKbIds(userId, currentUserAdmin, currentUserTeacher, requestedKbIds)`，拒绝后不进入 chunk retrieval/query log：`backend/src/main/java/com/learningos/rag/application/RagQueryService.java:271`、`:496`。
- Document list/detail/reindex/index-task detail 均先调用 `ensureCanRead/ensureCanWrite`，这些方法转到 roles-first `PermissionService`：`backend/src/main/java/com/learningos/rag/application/DocumentService.java:247`, `:263`, `:280`, `:300`, `:318`, `:328`。
- KnowledgeBase 创建 course-bound KB 时要求 `requireCourseRead` + `requireCourseManage`，并设置 `BOUND/boundBy/boundAt`：`backend/src/main/java/com/learningos/rag/application/KnowledgeBaseService.java:51`-`:60`。
- 控制器从 `UserContext.roles()` 派生 admin/teacher facts，未使用 subject-name 推断：`KnowledgeBaseController.java:34`-`:38`, `DocumentController.java:40`-`:48`, `ChatController.java:41`-`:58`, `TutorController.java:42`-`:48`。
- 迁移 `V20__kb_course_binding_governance.sql` 将单一有效课程文档 KB 标记为 `BOUND`，混合/无效课程文档标记为 `CONFLICTED`，并加 `ck_kb_binding_status`、`ck_kb_binding_course_consistency`、`fk_kb_course_binding_course`：`backend/src/main/resources/db/migration/V20__kb_course_binding_governance.sql:7`-`:32`, `:34`-`:61`, `:69`-`:75`。
- 测试覆盖：dropped student 无法查询 BOUND PUBLIC KB 且不落 query/citation artifact：`backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java:299`-`:317`；CONFLICTED 不走 owner/public/course-derived read：`backend/src/test/java/com/learningos/rag/application/PermissionServiceTest.java:144`-`:155`；学生即使 owner/PUBLIC 且 enrolled 也不能伪造 course metadata 上传：`backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java:220`-`:266`。

## OWASP 快速清单

- A01 Broken Access Control: 主路径通过，发现并发绑定中风险。
- A02 Cryptographic Failures: 本范围无加密实现；测试 JWT secret 为测试配置，未发现生产硬编码密钥。
- A03 Injection: 本范围未见 SQL 拼接；JPA repository/JPQL 参数化使用正常。
- A04 Insecure Design: 并发绑定缺锁、requestId 契约优先级回归见发现。
- A05 Security Misconfiguration: dependency-check 未完成，不能给出依赖安全通过结论。
- A06 Vulnerable Components: OWASP dependency-check 因网络/数据源失败未完成；依赖树已核验版本但未 CVE 判定。
- A07 Identification/Auth Failures: 控制器使用 Bearer-derived `UserContext.roles()` facts；旧 subject-name admin/teacher 推断在审查路径未见使用。
- A08 Software/Data Integrity: V20 迁移有 check/FK 约束；建议补并发一致性保护。
- A09 Logging/Monitoring: 查询失败路径记录指标，测试覆盖 forbidden 不落业务 artifact；本次未深入日志泄露审查。
- A10 SSRF: 本范围无外部 URL fetch；文档存储/解析路径未见 SSRF 输入。

## 建议优先级

1. 修复 MEDIUM：为 KB 自动绑定增加 pessimistic lock 或等效并发控制，并补并发测试，确保同一 KB 不可能绑定/写入两个 course 的 active documents。
2. 修复 LOW：决定 requestId payload conflict 与 course mismatch 的错误优先级；若保持既有契约，调整 `DocumentService` 顺序以返回 409。
3. 重新运行依赖审计：网络恢复后执行 OWASP dependency-check，或使用已配置的企业 CVE 镜像；当前不能证明依赖无已知高危漏洞。
4. 补充测试：显式覆盖 BOUND KB 的 owner/explicit READ/WRITE 权限不可绕过 course access，当前逻辑已满足，但测试矩阵最好把 explicit permission 也固定下来。
