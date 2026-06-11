# P3-4 子任务：kb-course-binding-governance 安全/权限并行分析

## 1. 审查范围

- 任务：补齐 RAG KB 与 Course 绑定 schema/lifecycle governance，避免 `PUBLIC` / owner KB、`document.course_id`、course ownership/enrollment 之间不一致造成越权检索/管理。
- 角色：Security & Quality 专家，只读分析，不改业务代码。
- 重点代码：
  - `backend/src/main/java/com/learningos/rag/application/PermissionService.java`
  - `backend/src/main/java/com/learningos/rag/application/KnowledgeBaseService.java`
  - `backend/src/main/java/com/learningos/rag/application/DocumentService.java`
  - `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
  - `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`
  - RAG / Tutor / Orchestrator / KnowledgeBase / Document 相关 Controller 与测试
  - `backend/src/main/resources/db/migration/V1__rag_foundation.sql`
  - `backend/src/main/resources/db/migration/V19__course_enrollment_scope.sql`

## 2. 检查执行记录

### 2.1 Skill Selection Gate

| 项 | 结论 |
|---|---|
| Task type | Security / authorization analysis；不执行实现 |
| Selected skills | `security-review`、`object-scope-authorization`、`auth-context-boundary` |
| 选择原因 | 本任务聚焦对象级授权、Bearer roles-first、header spoofing、RAG runtime permission filtering、KB/course lifecycle governance |
| Missing skills | 无；不需要 GitHub research |
| GitHub research | 不需要。当前问题是项目内权限模型收敛，不是外部框架缺口 |
| 新项目 skill | 可选：后续若完成 KB-course schema governance，可沉淀 `rag-kb-course-binding-governance` |

### 2.2 只读扫描

- Secrets scan：执行 `rg -n "api[_-]?key|password|secret|token|private[ _-]?key|BEGIN ...|credential|JWK|JWT" backend/src/main/java backend/src/test/java backend/src/main/resources docs`。命中主要为测试 JWT 假 secret、文档中的 secret policy / auth notes / dependency review，不发现真实生产 secret 明文。测试 secret 如 `unit-test-secret-32-bytes-long!!` 属于固定假值。
- Dependency audit：初次在仓库根执行 `mvn -q -pl backend org.owasp:dependency-check-maven:check -DskipTests` 失败，因为当前目录不是 Maven 多模块 reactor；随后在 `backend` 目录执行 `mvn -q org.owasp:dependency-check-maven:check -DskipTests`，被 Dependency-Check `.NET AssemblyAnalyzer` 因缺少 dotnet 8 runtime/sdk 阻断；最终执行 `mvn -q org.owasp:dependency-check-maven:check -DskipTests -DassemblyAnalyzerEnabled=false` 完成且无控制台报错。建议 CI 中固定该参数或安装 dotnet 8 后完整复跑。
- Git history secrets scan：尝试 `git log -p --all -S"AUTH_JWT_SECRET"` 失败，当前工作目录没有 `.git`，无法审查历史泄露。

## 3. 关键证据

| 证据 | 位置 | 安全含义 |
|---|---|---|
| `kb_knowledge_base` 只有 `visibility` / `owner_user_id`，没有 `course_id` / binding 状态 | `backend/src/main/resources/db/migration/V1__rag_foundation.sql:1` | KB 本身无法表达课程归属，不能靠 DB 约束证明 KB 与文档课程一致 |
| `kb_document` 有 `course_id` / `chapter_id` | `backend/src/main/resources/db/migration/V1__rag_foundation.sql:25` | 文档层已有课程元数据，但它不是 KB 层授权锚点 |
| `course_enrollment` 仅在 V19 独立存在 | `backend/src/main/resources/db/migration/V19__course_enrollment_scope.sql:1` | 学生课程读权限已有事实源，但 RAG KB 读写尚未绑定该事实源 |
| KB 创建 DTO 无 `courseId` | `backend/src/main/java/com/learningos/rag/api/dto/KnowledgeBaseDtos.java:14` | 新建 KB 无法声明课程归属或触发课程管理授权 |
| KB 创建只保存 owner / visibility | `backend/src/main/java/com/learningos/rag/application/KnowledgeBaseService.java:35` | 课程 teacher / enrollment 不参与 KB lifecycle |
| KB read：admin 全局；非 admin 只看 owner / PUBLIC / kb_permission | `backend/src/main/java/com/learningos/rag/application/PermissionService.java:187` | `currentUserTeacher` 参数当前没有实际参与 KB read；PUBLIC 可绕过课程 enrollment |
| KB write：admin 全局；非 admin 只看 owner / WRITE | `backend/src/main/java/com/learningos/rag/application/PermissionService.java:209` | owner KB 可以与课程事实脱钩；teacher 角色不是 course-bound write anchor |
| Document upload 先检查 KB write，再检查 course/chapter metadata | `backend/src/main/java/com/learningos/rag/application/DocumentService.java:93` | 已能阻止 document.course_id spoofing，但不能保证 KB 本身 course-bound |
| RAG query 只用 allowed KB IDs 检索 | `backend/src/main/java/com/learningos/rag/application/RagQueryService.java:271`、`:311` | query runtime 不按文档 course/enrollment 二次过滤；KB allowed 即可检索该 KB 全部 chunk |
| RAG query 成功后持久化 question、kbIds、sources | `backend/src/main/java/com/learningos/rag/application/RagQueryService.java:362` | 禁止请求必须在此之前 fail closed，否则会留下查询/引用副作用 |
| Course read/manage 语义已集中 | `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java:36`、`:64` | KB-course governance 应复用该服务，不应在 RAG 控制器复制课程规则 |

## 4. 威胁模型

### 4.1 攻击者与入口

| Actor | 能力 | 目标 |
|---|---|---|
| student | 可提交 `kbIds` 查询、拥有自建 private/public KB、可能 enrolled 某些课程 | 读取未 enrolled / foreign course 的 KB 文档 chunk；通过 PUBLIC KB 或 owner KB 混入课程文档 |
| teacher | 可管理自己课程、可创建 KB、可上传课程文档元数据 | 将 foreign course 文档绑定到自己可写 KB；读取/管理其他 teacher 课程 KB |
| admin | 全局管理者 | 应允许管理已存在课程/KB，但响应仍需避免泄漏 secret/raw content |
| spoofed header 用户 | 在 dev/test 或误配置环境尝试伪造 `X-User-Id` | 用 `X-User-Id: admin` / `teacher_1` 获取高权限 |
| Bearer `USER sub=admin` / `USER sub=teacher_1` | token subject 名像高权限，但 roles 不含 `ADMIN` / `TEACHER` | 利用 subject-name role confusion 访问 private/foreign/course-bound KB |
| public KB 读者 | 任何已认证用户，甚至学生 | 通过 `PUBLIC` 可读语义绕过课程 enrollment |
| private KB owner | KB owner，但不是课程 teacher/admin | 以 owner 权限管理或查询 course-bound 文档 |
| foreign course requester | 非课程 owner / 非 active enrolled | 通过 KB ID、document ID、index task ID、query replay 探测或读取 foreign course 数据 |

### 4.2 主要攻击路径

1. `PUBLIC` KB 绕过课程 enrollment：
   - 当前 `PermissionService.canReadKnowledgeBase` 对 `Visibility.PUBLIC` 直接放行。
   - 如果某个 KB 后续绑定 course 或包含 `document.course_id`，未 enrolled student 仍可能通过 `PUBLIC` KB 查询课程 chunk。

2. owner KB 绕过课程 manage：
   - 当前 KB owner 可 write；如果 KB 后续被 course-bound 或已有 course document，owner 不一定是 course teacher/admin。
   - 学生自建 private/public KB 已被 DocumentService 阻止写入 `courseId` 元数据，但若 KB 绑定迁移或历史数据不一致，owner write 仍可能成为管理绕过。

3. document.course_id 与 KB course_id 不一致：
   - 当前 DB 无 KB course_id，无法做一致性约束。
   - 如果一个 KB 内混入多个课程文档，query 只按 KB ID 检索，不能保证返回 sources 都属于 requester 可读课程。

4. requestId replay 绕过：
   - RAG query replay 已在 `replayContext` 中重跑 `requireReadableKbIds`，这是好的。
   - 但如果后续 course-bound KB 权限变更未纳入 `requireReadableKbIds`，旧 response snapshot 可在 enrollment/teacher ownership 变化后被 replay。

5. missing vs foreign oracle：
   - 文档详情和 index task 对非 admin 已有 safe `FORBIDDEN`。
   - KB list/detail 如果新增 course-bound detail/update/delete，必须沿用 non-admin missing/foreign collapse，否则可探测课程/KB 存在性。

## 5. 推荐授权语义

### 5.1 建议 schema / lifecycle 语义

建议给 KB 增加显式绑定字段，并使绑定成为所有 RAG 权限判定的第一等事实：

```java
// GOOD: KB 自身承载课程绑定事实，PermissionService 在读写/query 前统一检查。
public final class KnowledgeBaseCourseScope {
    private final String courseId;       // null 表示非课程模板/个人 KB
    private final String bindingStatus;  // ACTIVE / ARCHIVED
}
```

推荐 DB 方向：

- `kb_knowledge_base.course_id varchar(80) null`
- `kb_knowledge_base.binding_status varchar(40) not null default 'ACTIVE'`
- index：`idx_kb_course_status(course_id, binding_status, deleted_at)`
- 对历史数据：
  - 若 KB 下所有非删除文档 `course_id` 一致，可迁移为该 `course_id`。
  - 若混合多个 course 或有 course/non-course 混合，必须标记为待治理状态，例如 `BINDING_INCONSISTENT`，默认不可 query / 不可写 course document，admin 可审计修复。

### 5.2 Read 语义

| KB 类型 | admin | teacher | student / ordinary user |
|---|---|---|---|
| `courseId == null` + PRIVATE | owner / explicit permission / admin | owner / explicit permission | owner / explicit permission |
| `courseId == null` + PUBLIC | admin 可读 | 任何认证用户可读，但不得含 course-bound document | 任何认证用户可读，但不得含 course-bound document |
| `courseId != null` | admin 可读 existing course | 必须是该 course teacher，或显式 KB permission 且通过 course read | 必须 active enrolled；PUBLIC 不得绕过 enrollment |

核心原则：

- 一旦 KB 绑定 `courseId`，`PUBLIC` 只能表示“课程内公开”，不是全站公开。
- `kb_permission.READ` 不能单独覆盖 course enrollment / course ownership；它最多是附加 grant，仍必须通过 course read。
- 非 admin 对 missing KB、foreign KB、course inaccessible KB 返回同类 `FORBIDDEN`，不含 KB ID / courseId / title。

安全示例：

```java
// BAD: course-bound KB 仍被 PUBLIC 或 owner 直接放行
return userId.equals(kb.getOwnerUserId())
        || kb.getVisibility() == Visibility.PUBLIC
        || hasPermission(perms, kb.getId(), Set.of("READ", "WRITE", "OWNER"));

// GOOD: course-bound KB 先检查课程 read，再应用 KB 语义
if (hasText(kb.getCourseId())) {
    Course course = courseAccessService.requireCourseRead(userId, isAdmin, isTeacher, kb.getCourseId());
    if (isAdmin) {
        return true;
    }
    return isCourseScopedKbReader(kb, userId, isTeacher, course, perms);
}
return userId.equals(kb.getOwnerUserId())
        || kb.getVisibility() == Visibility.PUBLIC
        || hasPermission(perms, kb.getId(), Set.of("READ", "WRITE", "OWNER"));
```

### 5.3 Write / manage 语义

| 操作 | 授权要求 |
|---|---|
| create course-bound KB | `courseId` 必填时必须 `CourseAccessService.requireCourseManage`；student 禁止 |
| update KB binding | admin 或 course teacher；不允许普通 owner 任意换绑 |
| upload course document | KB write + KB `courseId == request.courseId` + course manage + chapter belongs to course |
| upload non-course document | 只能进入 `courseId == null` 的 KB；不得写入 course-bound KB |
| reindex/delete document | KB write + 若 document/course-bound，则当前仍满足 course manage |
| grant KB permission | admin/course teacher；grant 不得使受让人绕过 course read |

安全示例：

```java
// BAD: 只检查 KB write，不检查 KB 与请求课程一致
ensureCanWrite(userId, isAdmin, isTeacher, kbId);
validateCourseChapterScope(userId, isAdmin, isTeacher, requestCourseId, chapterId);

// GOOD: KB 写权限、课程管理权限、KB-course/document-course 一致性全部通过才持久化
KnowledgeBase kb = loadActiveKb(kbId);
ensureCanWrite(userId, isAdmin, isTeacher, kb);
String normalizedCourseId = normalize(courseId);
if (hasText(kb.getCourseId())) {
    if (!kb.getCourseId().equals(normalizedCourseId)) {
        throw new ApiException(ErrorCode.FORBIDDEN, "Knowledge base course scope denied");
    }
    Course course = courseAccessService.requireCourseRead(userId, isAdmin, isTeacher, kb.getCourseId());
    courseAccessService.requireCourseManage(userId, isAdmin, isTeacher, course);
} else if (hasText(normalizedCourseId)) {
    throw new ApiException(ErrorCode.FORBIDDEN, "Knowledge base course scope denied");
}
```

### 5.4 Query 语义

| 场景 | 期望 |
|---|---|
| requested KB 混合 allowed + forbidden | 整体 `FORBIDDEN`，不降级为只查 allowed |
| course-bound KB | `requireReadableKbIds` 必须同时验证 KB read 与 course read/enrollment |
| PUBLIC course-bound KB | 未 enrolled student 仍 `FORBIDDEN` |
| owner 但非 course teacher/enrolled | `FORBIDDEN` |
| admin | 可 query existing active KB；missing 可以为 `FORBIDDEN` 或 admin-only `NOT_FOUND`，但需一致测试 |
| replay query | replay 前重跑 KB-course authorization；失去 enrollment/teacher ownership 后不得 replay |
| retrieval | `ChunkService.retrieveAllowedChunks` 接收到的 allowed KB 必须已通过 course-bound validation；若支持 chunk/doc course filter，则额外以 `document.course_id == kb.course_id` fail closed |

安全示例：

```java
// GOOD: replay 和真实 query 使用同一个权限解析结果，权限失败在日志/引用持久化前发生
ResolvedKbScope scope = permissionService.requireReadableKbIdsForQuery(
        currentUserId,
        currentUserAdmin,
        currentUserTeacher,
        requestedKbIds
);
if (scope.allowedKbIds().isEmpty()) {
    throw new ApiException(ErrorCode.FORBIDDEN, "No accessible knowledge bases for this query");
}
return queryResolved(currentUserId, scope.allowedKbIds(), question, limit, traceId, requestId, requestHash);
```

## 6. 必须 fail closed 的场景

1. KB 有 `courseId`，当前用户不是 admin / course teacher / active enrolled student。
2. `Visibility.PUBLIC` KB 绑定了课程，而当前 student 未 active enrolled。
3. KB owner 不是该 course teacher/admin，却尝试 upload/reindex/delete course-bound document。
4. 上传请求 `courseId` 与 KB `courseId` 不一致。
5. 上传请求带 `chapterId`，但 `chapter.courseId != kb.courseId/request.courseId`。
6. KB 为非课程 KB，却上传 `courseId` / `chapterId`。
7. KB 为课程 KB，却上传空 `courseId` 或 non-course 文档。
8. KB 内历史文档存在多个不同 `document.course_id`；query 与 write 默认拒绝，admin audit 例外。
9. KB binding 状态为 `ARCHIVED` / `BINDING_INCONSISTENT` / `DELETED`。
10. requested `kbIds` 混合 allowed 与 forbidden；必须整体拒绝，不允许静默过滤。
11. requestId replay 时当前用户已失去 course enrollment / teacher ownership / KB permission。
12. Bearer token roles 为 `USER`，即使 `sub=admin` 或 `sub=teacher_1`。
13. Bearer token 存在时客户端同时提交 spoofed `X-User-Id`。
14. prod/staging 无 Bearer token，仅有 `X-User-Id`。
15. forbidden RAG query / upload / reindex 不得产生 query log、source citation、document、index task、object storage 写入或成功 trace artifact。
16. 非 admin missing KB 与 foreign KB 响应不得形成存在性 oracle。

## 7. 回归/渗透测试矩阵

优先沿用现有测试风格：

- `KnowledgeBaseControllerTest`：`@SpringBootTest + @AutoConfigureMockMvc + @Transactional`，JWT helper 覆盖 Bearer roles 与 spoofed header。
- `DocumentControllerTest`：MockMvc multipart，断言 forbidden 后 `documentRepository.count()` / `indexTaskRepository.count()` 不变，响应不包含 foreign id。
- `RagQueryServiceTest`：service-level seed KB/document/chunk，断言 `queryLogRepository.count()` / `sourceCitationRepository.count()`。
- `OrchestratorWorkflowControllerTest`：RAG_QA wrapper，断言 failure evidence safe，且不生成成功 query/citation artifacts。
- `CourseAccessServiceTest`：course missing/foreign/enrollment 基础语义。

### 7.1 KB lifecycle / schema tests

| ID | 测试 | 期望 |
|---|---|---|
| KB-CREATE-01 | teacher Bearer `TEACHER` 为自己 course 创建 course-bound KB | 200；KB `courseId` 持久化 |
| KB-CREATE-02 | teacher 为 foreign course 创建 KB | 403；响应不含 foreign courseId/title |
| KB-CREATE-03 | student enrolled course 创建 course-bound KB | 403；无 KB row |
| KB-CREATE-04 | Bearer `USER sub=teacher_1` 创建 course-bound KB | 403 |
| KB-CREATE-05 | Bearer `ADMIN` + spoofed `X-User-Id=student` 创建 foreign course KB | 200；createdBy 为 token subject |
| KB-CREATE-06 | public course-bound KB 创建请求 | 若允许，语义为 course-public；未 enrolled student 不可读 |
| KB-BIND-01 | 更新 KB course binding 到 foreign course | 403 |
| KB-BIND-02 | 更新 KB binding 前存在 mixed document.course_id | 400/403；状态 remains inconsistent |
| KB-LIST-01 | student 只看到 active enrolled course-bound KB + allowed non-course KB | 不返回 foreign course-bound KB |
| KB-LIST-02 | teacher 只看到自己课程 course-bound KB + explicit allowed non-course KB | 不返回 foreign course-bound KB |

### 7.2 Document upload / lifecycle tests

| ID | 测试 | 期望 |
|---|---|---|
| DOC-UP-01 | course-bound KB 上传同 courseId 文档 | 200 |
| DOC-UP-02 | course-bound KB 上传 foreign courseId 文档 | 403；document/index/object storage 不增加 |
| DOC-UP-03 | course-bound KB 上传空 courseId 文档 | 403 或 400；无副作用 |
| DOC-UP-04 | non-course KB 上传 courseId 文档 | 403；防止课程文档混入个人/PUBLIC KB |
| DOC-UP-05 | chapterId 属于其他 course | 400 generic；不回显 chapterId/courseId |
| DOC-UP-06 | same requestId，courseId/chapterId 改变 | 409；不重复写 |
| DOC-UP-07 | student owner PUBLIC KB + active enrollment 上传 courseId | 403；沿用现有测试并增加 KB binding 断言 |
| DOC-REINDEX-01 | teacher reindex foreign course-bound KB document | 403；无新 index task |
| DOC-DETAIL-01 | non-admin foreign document vs missing document | 响应同类 `FORBIDDEN`，body 不含 id |

### 7.3 Query / retrieval tests

| ID | 测试 | 期望 |
|---|---|---|
| RAG-Q-01 | enrolled student query course-bound KB | 200；sources 属于该 course |
| RAG-Q-02 | unenrolled student query PUBLIC course-bound KB | 403；queryLog/sourceCitation 为 0 |
| RAG-Q-03 | KB owner 但非 course teacher/enrolled query course-bound KB | 403 |
| RAG-Q-04 | teacher query own course-bound KB | 200 |
| RAG-Q-05 | teacher query foreign course-bound KB | 403；无 query/citation |
| RAG-Q-06 | requested KB 混合 own allowed + foreign forbidden | 403；不得只查 own allowed |
| RAG-Q-07 | Bearer `USER sub=admin` query foreign private/course-bound KB | 403 |
| RAG-Q-08 | Bearer `ADMIN` + spoofed header query foreign course-bound KB | 200；userId 为 token subject |
| RAG-Q-09 | requestId replay 前移除 enrollment | replay 返回 403，不返回旧 response snapshot |
| RAG-Q-10 | mixed document.course_id in same KB | query 403 或跳过 inconsistent KB；建议 fail closed |

### 7.4 Wrapper / SSE / Orchestrator tests

| ID | 测试 | 期望 |
|---|---|---|
| CHAT-01 | `/api/rag/query` POST/GET 对 course-bound KB 均执行同一授权 | POST/GET 结果一致 |
| CHAT-SSE-01 | `/api/chat/sessions/{id}/stream` 对 forbidden course-bound KB | SSE error 前无 query/citation 成功 artifact |
| TUTOR-01 | `/api/tutor/ask` 未 enrolled student query course-bound KB | 403 |
| TUTOR-SSE-01 | tutor stream forbidden KB | 不泄漏 answer/source |
| ORCH-RAG-01 | Orchestrator RAG_QA forbidden course-bound KB | HTTP 403；保留 safe failed workflow evidence；无 query/citation success row |
| ORCH-RAG-02 | Orchestrator replay precheck enrollment revoked | 不 replay old answer；failure evidence 不含 raw question/kbId/courseId |

### 7.5 Migration / consistency tests

| ID | 测试 | 期望 |
|---|---|---|
| MIG-01 | MySQL empty schema migrates through new KB course binding migration | PASS |
| MIG-02 | historical KB with all docs same course | migration derives `kb.course_id` 或 writes audit row |
| MIG-03 | historical KB with mixed courses | migration marks inconsistent / leaves unbound and query fail closed |
| MIG-04 | historical PUBLIC KB with course docs | not globally readable after migration |
| MIG-05 | index exists on `kb_knowledge_base(course_id,binding_status,deleted_at)` | smoke verifies |

## 8. 必须避免的架构漂移

1. 不要让 Controller 写课程归属规则。Controller 只读取 `UserContext` 与请求参数，课程/K B授权留在 Service。
2. 不要让 Agent Tool / Orchestrator 直接访问 Mapper/Repository 做 KB 权限判断；必须调用 application service。
3. 不要用 Prompt 控制权限；RAG 检索前必须在后端代码过滤。
4. 不要让 `currentUserTeacher` 在 RAG 里变成 “所有课程可读”。teacher 只能读/写自己课程或显式授权且 course read 通过的 KB。
5. 不要保留 subject-name inference：`admin` / `teacher_1` 字符串不能授予 role。
6. 不要把 `PUBLIC` 继续解释为全站公开，当 KB 绑定 course 后应解释为 course-public 或直接禁止 PUBLIC course KB。
7. 不要在 ChunkService/VectorDB 层重新发明权限模型；应接收已验证的 scoped KB/document/course filter。
8. 不要引入新 dependency 解决 schema/authorization；本任务主要是 DB + service authorization governance。
9. 不要只校验 document upload；query、list、detail、reindex、replay、orchestrator wrapper 都是同一权限面。
10. 不要在 migration 中默默把 mixed-course KB 标记为可用；历史不一致必须显式隔离或 fail closed。

## 9. 敏感信息泄露要求

- forbidden 响应不包含 foreign `kbId`、`courseId`、`documentId`、`indexTaskId`、course title、teacherId、storage bucket/key、traceId、requestId。
- RAG query log / source citation 不记录 forbidden 请求的 success artifact。
- `sourcesJson` 只允许 source summary 字段；避免 raw provider error、prompt、full chunk、secret、storage key。
- `responseJson` replay snapshot 不应在用户失去权限后直接返回。
- SSE error 不应把 `ApiException` raw message、stack trace、foreign id 发给客户端。
- Agent trace failed evidence 只保留安全错误码和阶段，不写 raw question、raw KB IDs、course titles、document names。

## 10. OWASP Top 10 评估

| OWASP | 结论 |
|---|---|
| A01 Broken Access Control | 高风险主项。当前 KB 缺少 course binding，`PUBLIC` / owner / permission 与 course enrollment 可能冲突 |
| A02 Cryptographic Failures | 未发现本任务新增 crypto；测试 JWT secret 为假值；认证边界已有 roles-first 要求 |
| A03 Injection | 未发现 SQL 拼接；JPA repository 为主。本任务新增 migration 时需避免动态 SQL 拼接 |
| A04 Insecure Design | 高风险主项。KB lifecycle 与 document/course metadata 分离导致设计层不一致 |
| A05 Security Misconfiguration | prod/staging header spoofing 已有规则；需确保新增 endpoints 继续依赖 Bearer roles |
| A06 Vulnerable Components | Dependency-Check 以 `assemblyAnalyzerEnabled=false` 完成；建议 CI 固化并保留报告 |
| A07 Identification/Auth Failures | 需继续覆盖 Bearer `USER sub=admin/teacher_1` 和 spoofed `X-User-Id` |
| A08 Software/Data Integrity | requestId replay 必须重跑 course-bound authorization；历史 mixed-course migration 必须 fail closed |
| A09 Logging/Monitoring Failures | forbidden 不能写 success query/citation；safe failure evidence 可保留 |
| A10 SSRF | 本任务不涉及外部 URL fetch；RAG document storage/parser provider 不应新增 URL 拉取 |

## 11. 风险评级与优先级

**Overall Risk Level: HIGH**

原因：RAG retrieval 是高敏数据出口；当前 KB 本身无 course binding，query runtime 只按 KB ID 做授权，若后续或历史数据出现 `PUBLIC/owner KB + course document` 错位，未 enrolled student 或非 owner teacher 可能读取 foreign course chunk。该风险远高于普通对象详情泄露，因为 RAG answer/source 会把课程材料直接返回并持久化 query/citation evidence。

### High

1. **Course-bound KB read/query authorization gap**
   - 位置：`PermissionService.java:187`、`RagQueryService.java:271`
   - 类别：OWASP A01 / A04
   - Exploitability：远程，已认证用户，需知道或枚举 KB ID / PUBLIC KB。
   - Blast radius：跨课程 RAG 内容泄露、source citation 泄露、query replay 泄露。
   - 修复方向：KB schema 增加 course binding；`requireReadableKbIds` 统一验证 course read/enrollment。

2. **KB write 与 course manage 不一致**
   - 位置：`PermissionService.java:209`、`DocumentService.java:93`
   - 类别：OWASP A01
   - Exploitability：远程，KB owner / explicit WRITE。
   - Blast radius：foreign course material 被写入或 reindex 到错误 KB，污染后续 RAG。
   - 修复方向：write path 同时验证 KB binding 与 `CourseAccessService.requireCourseManage`。

3. **Historical/migration inconsistent data may become globally queryable**
   - 位置：`V1__rag_foundation.sql:1`、`:25`
   - 类别：OWASP A04 / A08
   - Exploitability：取决于历史数据或后续迁移。
   - Blast radius：mixed-course KB 被 PUBLIC/owner 语义放大。
   - 修复方向：migration 审计 mixed course KB，默认 `BINDING_INCONSISTENT` fail closed。

### Medium

4. **Sensitive metadata in logs/snapshots if forbidden checks move too late**
   - 位置：`RagQueryService.java:362`
   - 类别：OWASP A09
   - Exploitability：需要权限失败路径写入 artifact。
   - Blast radius：query/question/source metadata 泄露给审计或后续 API。
   - 修复方向：权限、course consistency、replay precheck 必须在 query log/source citation 之前。

5. **Dependency audit environment fragility**
   - 位置：Dependency-Check 执行环境
   - 类别：OWASP A06
   - Exploitability：间接。
   - Blast radius：CI 漏扫。
   - 修复方向：CI 固定 `-DassemblyAnalyzerEnabled=false` 或安装 dotnet 8 runtime/sdk，并归档报告。

## 12. 结论

当前已有 formal OAuth2/JWK、roles-first RBAC、CourseAccessService、document upload course metadata scope 和 RAG query runtime RBAC，这些基础是正确的。但 `kb_knowledge_base` 尚未成为 course-bound 对象，导致 KB read/write/query 仍以 owner/PUBLIC/kb_permission 为主。`document.course_id` 只能保护上传元数据，不能保护 KB 本身的生命周期和 query-time retrieval。

本子任务应优先把 KB-course binding 作为授权锚点落入 schema 和 `PermissionService`，再统一收口 create/list/read/write/upload/reindex/query/replay/orchestrator/SSE。最重要的 fail-closed 原则是：一旦 KB 或其文档涉及 course，`PUBLIC` 与 owner 权限都不能绕过 `CourseAccessService` 的 teacher ownership / active enrollment 事实。
