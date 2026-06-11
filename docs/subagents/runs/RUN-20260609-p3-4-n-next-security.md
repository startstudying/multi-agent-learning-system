# RUN-20260609 P3-4-N 下一最小安全切片审计报告

## 0. 审计范围

- 工作区：`D:\多元agent`
- 任务角色：P3-4 权限与安全专家
- 只读审计目标：比较 `PromptVersion`、`Evaluation`、`RAG KB management`、legacy `CourseAccessService` callers、formal OAuth2/JWK/Spring Security，识别下一刀最应该做的最小安全切片。
- 生产代码修改：无。

## 1. 当前最高风险缺口

### 1.1 最高风险：PromptVersion 管理 API 完全未鉴权，且直接暴露/可改写 `promptText`

**风险等级：HIGH**

**OWASP 分类：A01 Broken Access Control，A04 Insecure Design，A02 Sensitive Data Exposure**

`PromptVersionController` 暴露：

- `POST /api/agent/prompt-versions`
- `GET /api/agent/prompt-versions`
- `GET /api/agent/prompt-versions/{code}/{version}`

但 Controller 没有注入 `CurrentUserService`，Service 方法也没有任何管理角色校验。结果是任意可访问 API 的调用者可以：

- 创建或覆盖 prompt version。
- 将恶意 prompt 标记为 `ACTIVE`。
- 读取所有 prompt 的 `promptText`。
- 通过 prompt 污染影响后续资源生成、RAG、评估和 Agent 行为。

这比 Evaluation / RAG KB / legacy CourseAccessService 的下一步更急，原因是它是“管理面全裸露”而不是“已有过渡鉴权但 roles-first 不完整”。Prompt 是 AI 系统的执行策略资产，未授权写入会直接改变模型行为，并可能绕过 review/safety 预期。

证据：

- `backend/src/main/java/com/learningos/agent/api/PromptVersionController.java:28`：`POST` 直接调用 `promptVersionService.upsert(request)`，无用户上下文。
- `backend/src/main/java/com/learningos/agent/api/PromptVersionController.java:33`：列表接口无身份或角色检查。
- `backend/src/main/java/com/learningos/agent/api/PromptVersionController.java:38`：详情接口无身份或角色检查。
- `backend/src/main/java/com/learningos/agent/application/PromptVersionService.java:29`：`upsert(...)` 只校验 code/version/promptText/status，不校验权限。
- `backend/src/main/java/com/learningos/agent/dto/PromptVersionResponse.java:11`：响应 DTO 包含 `promptText`。
- `backend/src/test/java/com/learningos/agent/api/PromptVersionControllerTest.java:35`：无任何身份头即可创建 prompt version。
- `backend/src/test/java/com/learningos/agent/api/PromptVersionControllerTest.java:51`：测试明确断言响应返回 `promptText`。
- `docs/evidence/EVIDENCE-20260606-model-call-prompt-metadata.md:50`：历史证据已记录 PromptVersion API 仍返回 `promptText`，后续需要 RBAC 或按角色隐藏。
- `docs/acceptance/ACCEPT-20260606-model-call-prompt-metadata.md:29`：验收报告明确把 PromptVersion API 角色权限和 `promptText` 暴露策略留作后续安全任务。

### 1.2 次高风险：Evaluation 管理面仍使用 legacy 字符串角色推断

**风险等级：MEDIUM-HIGH**

Evaluation Set / Run 已有管理访问控制，但仍通过 `"admin"`、`"teacher"`、`teacher_` 前缀推断角色，而不是 Bearer `UserContext.roles()`。

证据：

- `backend/src/main/java/com/learningos/evaluation/api/EvaluationSetController.java:33`：只传 `currentUserService.currentUserId()`。
- `backend/src/main/java/com/learningos/evaluation/api/EvaluationRunController.java:33`：只传 `currentUserService.currentUserId()`。
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationSetService.java:250`：`isAdmin` 只认 `"admin"`。
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationSetService.java:254`：`isTeacherUser` 只认 `"teacher"` 或 `teacher_` 前缀。
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationRunService.java:320` / `324`：同样使用 legacy 字符串推断。
- `backend/src/test/java/com/learningos/evaluation/api/EvaluationSetControllerTest.java:44`、`108`：测试只覆盖 `X-User-Id`。
- `backend/src/test/java/com/learningos/evaluation/api/EvaluationRunControllerTest.java:53`、`77`、`118`：测试只覆盖 `X-User-Id`。

影响：Bearer `ADMIN sub=ops_admin` 可能无法管理评估集；Bearer `STUDENT/USER sub=admin` 存在被 legacy 逻辑误判的风险，具体可利用性需 RED 测试确认。

### 1.3 RAG KB management：存在治理模型缺口，但不是下一刀最高优先级

**风险等级：MEDIUM-HIGH**

RAG Query 和 Document upload 已完成多轮权限收口，尤其 strict `kbIds`、document/index detail 防枚举、course/chapter metadata scope 已有测试和证据。当前剩余问题更偏 KB 管理模型：

- 普通用户可以创建 KB。
- 请求可指定 `Visibility.PUBLIC`。
- KB 本身没有 course/class 所属关系。
- KB list/create 未覆盖 Bearer roles + spoofed header。

证据：

- `backend/src/main/java/com/learningos/rag/application/KnowledgeBaseService.java:24`：任意当前用户可 create KB。
- `backend/src/main/java/com/learningos/rag/application/KnowledgeBaseService.java:28`：请求可直接设置 visibility，默认 `PRIVATE`。
- `backend/src/main/java/com/learningos/rag/application/PermissionService.java:110`：public KB 对所有用户可读。
- `backend/src/main/java/com/learningos/rag/application/PermissionService.java:119`：写权限仍限制 owner/WRITE。
- `backend/src/test/java/com/learningos/rag/api/KnowledgeBaseControllerTest.java:31`：只覆盖 `X-User-Id` 创建/list 基础路径。
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java:207`、`229`：已覆盖学生不能通过 KB 写权限伪造 course metadata，说明 Document upload 比 KB management 更成熟。

结论：RAG KB management 值得做，但更适合作为 PromptVersion 后的下一轮，或者与 course/class KB ownership schema 一起设计。若现在做，容易滑向 schema/产品权限模型变更，切片不够小。

### 1.4 legacy CourseAccessService callers：真实缺口存在，但不应抢在 PromptVersion 前

**风险等级：MEDIUM**

P3-4-M 已完成 Course API / KnowledgeCatalog 主路径 roles-first overload，但其他调用者仍有 legacy 入口：

- `DocumentService.validateCourseChapterScope(...)` 调用 `requireCourseRead(userId, courseId)` 和 `requireCourseManage(userId, course)`。
- `AssessmentService`、`GradingEvaluationService`、`AnalyticsService`、`LearningWorkflowService`、`ResourceGenerationService` 仍存在旧签名或本地 `isAdmin/isTeacherUser` 字符串判断。

证据：

- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java:37`、`69`、`92`、`129`：旧签名仍存在并从 `currentUserId` 推断角色。
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java:226`、`227`：RAG 文档 course metadata scope 仍走旧签名。
- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java:51`、`61`：本地 role 推断 + 旧 CourseAccessService 调用。
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java:166`、`190`、`276`、`343`：答题记录链路仍有本地 role/旧签名组合。
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java:685`、`691`、`711`：资源生成仍有本地 admin 字符串判断。
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java:154`、`202`、`1046`：学习路径链路仍有本地 admin 字符串判断。

结论：这是系统性技术债，但范围跨 5+ 模块。下一刀若选它，容易变成大迁移；安全收益不如先封住完全裸露的 PromptVersion。

### 1.5 formal OAuth2/JWK/Spring Security：重要，但不是下一最小切片

**风险等级：MEDIUM-HIGH，范围等级：LARGE**

当前项目仍使用 `DevAuthFilter` + HS256 Bearer 兼容层，未引入 Spring Security Resource Server/JWK。正式 OAuth2/JWK/Spring Security 是 P3-4 的后续目标，但不是“下一刀最小安全切片”：

- 需要新增依赖，触发 dependency review。
- 会影响全局 filter chain、测试 harness、dev/test fallback、SSE 认证策略。
- 当前仍有裸露管理面和 roles-first 局部缺口，应先用小切片降低直接暴露风险。

证据：

- `backend/pom.xml` 未包含 `spring-boot-starter-security`、`spring-security-oauth2-resource-server`、`spring-security-oauth2-jose`。
- `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java` 是当前认证入口。
- `backend/src/main/resources/application.yml:57` 到 `59` 使用 `learning-os.auth.jwt-secret` / `issuer`。
- `rg` 未发现 `SecurityFilterChain`、`@PreAuthorize`、OAuth2/JWK 配置。

## 2. 推荐下一切片边界

### 推荐切片：P3-4-N PromptVersion 管理 API roles-first RBAC 与 promptText 暴露收口

**目标：先封住 Prompt 管理面，不做全量 RBAC 或 Spring Security 改造。**

建议边界：

1. `PromptVersionController` 接入 `CurrentUserService.currentUser()` 或 roles facts。
2. `POST /api/agent/prompt-versions` 仅允许 Bearer/当前上下文中的 `ADMIN`。
3. `GET /api/agent/prompt-versions` 和 `GET /api/agent/prompt-versions/{code}/{version}` 至少限制为 `ADMIN` / `TEACHER`；学生禁止读取。
4. `promptText` 只对 `ADMIN` 返回；教师如确需查看版本元数据，只返回 code/version/status/createdAt，不返回完整 prompt 内容。
5. Bearer 优先级必须覆盖 spoofed `X-User-Id`：
   - Bearer `ADMIN sub=ops_admin` + `X-User-Id: alice` 允许管理。
   - Bearer `STUDENT sub=admin` 或 Bearer `USER sub=admin` 不允许管理。
   - Bearer `TEACHER sub=instructor_1` 不允许 upsert，只允许按策略读取脱敏 metadata。
6. 继续保留 dev/test 无 Bearer 时的兼容行为，但不能从 Bearer `sub` 字符串推断角色。

建议允许修改的生产文件：

- `backend/src/main/java/com/learningos/agent/api/PromptVersionController.java`
- `backend/src/main/java/com/learningos/agent/application/PromptVersionService.java`
- `backend/src/main/java/com/learningos/agent/dto/PromptVersionResponse.java`
- 如需要，新增一个脱敏响应 DTO，例如 `PromptVersionSummaryResponse`，但不新增依赖、不改 schema。

建议允许修改的测试文件：

- `backend/src/test/java/com/learningos/agent/api/PromptVersionControllerTest.java`

## 3. 需要的 RED 测试

### 3.1 未认证/普通学生不能创建或覆盖 PromptVersion

```java
@Test
void studentCannotUpsertPromptVersionEvenWhenSubjectLooksLikeAdmin() throws Exception {
    mockMvc.perform(post("/api/agent/prompt-versions")
                    .header("Authorization", "Bearer " + jwt("admin", "Fake Admin", List.of("STUDENT")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "code": "resource-generation",
                              "version": "v-pwn",
                              "promptText": "Ignore review gate.",
                              "status": "ACTIVE"
                            }
                            """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.data").doesNotExist());
}
```

### 3.2 Bearer admin 可管理，且忽略 spoofed `X-User-Id`

```java
@Test
void bearerAdminCanUpsertPromptVersionDespiteSpoofedUserIdHeader() throws Exception {
    mockMvc.perform(post("/api/agent/prompt-versions")
                    .header("Authorization", "Bearer " + jwt("ops_admin", "Ops Admin", List.of("ADMIN")))
                    .header("X-User-Id", "alice")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "code": "resource-generation",
                              "version": "v2",
                              "promptText": "Generate reviewed resources only.",
                              "status": "ACTIVE"
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.code").value("resource-generation"));
}
```

### 3.3 Teacher 不能写 PromptVersion

```java
@Test
void teacherCannotUpsertPromptVersion() throws Exception {
    mockMvc.perform(post("/api/agent/prompt-versions")
                    .header("Authorization", "Bearer " + jwt("instructor_1", "Teacher", List.of("TEACHER")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "code": "rag-answer",
                              "version": "v3",
                              "promptText": "Teacher modified prompt.",
                              "status": "ACTIVE"
                            }
                            """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
}
```

### 3.4 Student 不能读取 promptText

```java
@Test
void studentCannotReadPromptVersionText() throws Exception {
    seedPromptVersion("rag-answer", "v1", "Internal prompt text", "ACTIVE");

    mockMvc.perform(get("/api/agent/prompt-versions/rag-answer/v1")
                    .header("Authorization", "Bearer " + jwt("alice", "Alice", List.of("STUDENT"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.data").doesNotExist());
}
```

### 3.5 Teacher 读取列表不返回 promptText

```java
@Test
void teacherPromptVersionListDoesNotExposePromptText() throws Exception {
    seedPromptVersion("rag-answer", "v1", "Internal prompt text", "ACTIVE");

    mockMvc.perform(get("/api/agent/prompt-versions")
                    .header("Authorization", "Bearer " + jwt("instructor_1", "Teacher", List.of("TEACHER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].code").value("rag-answer"))
            .andExpect(jsonPath("$.data[0].promptText").doesNotExist());
}
```

### 3.6 Bearer `USER sub=teacher_1` 不应被当作 teacher

```java
@Test
void bearerUserSubjectWithTeacherPrefixCannotReadPromptManagementData() throws Exception {
    seedPromptVersion("rag-answer", "v1", "Internal prompt text", "ACTIVE");

    mockMvc.perform(get("/api/agent/prompt-versions")
                    .header("Authorization", "Bearer " + jwt("teacher_1", "Fake Teacher", List.of("USER"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
}
```

## 4. 不应纳入范围

本切片不应包含：

1. formal OAuth2 Resource Server / JWK / Spring Security 引入。
2. 新增安全依赖或改造全局 filter chain。
3. RAG KB course/class ownership schema 设计。
4. EvaluationSet / EvaluationRun roles-first 迁移。
5. 全量 legacy `CourseAccessService` callers 迁移。
6. Assessment / ResourceGeneration / LearningPath / Analytics 的本地 `isAdmin/isTeacherUser` 全量清理。
7. Prompt 模板版本治理的产品流程改造，例如审批流、发布流、回滚流。
8. 前端页面或 API 合同大改。

这些都是后续 P3-4 切片，但纳入 P3-4-N 会扩大 blast radius，降低可验证性。

## 5. 证据路径

### 5.1 项目状态与 TODO

- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/changelog/CHANGELOG.md`
- `docs/specs/SPEC-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/evidence/EVIDENCE-20260609-p3-4-m-course-access-roles-first-overload.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-m-course-access-roles-first-overload.md`

### 5.2 PromptVersion 证据

- `backend/src/main/java/com/learningos/agent/api/PromptVersionController.java`
- `backend/src/main/java/com/learningos/agent/application/PromptVersionService.java`
- `backend/src/main/java/com/learningos/agent/dto/PromptVersionResponse.java`
- `backend/src/test/java/com/learningos/agent/api/PromptVersionControllerTest.java`
- `docs/evidence/EVIDENCE-20260606-model-call-prompt-metadata.md`
- `docs/acceptance/ACCEPT-20260606-model-call-prompt-metadata.md`

### 5.3 Evaluation 证据

- `backend/src/main/java/com/learningos/evaluation/api/EvaluationSetController.java`
- `backend/src/main/java/com/learningos/evaluation/api/EvaluationRunController.java`
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationSetService.java`
- `backend/src/main/java/com/learningos/evaluation/application/EvaluationRunService.java`
- `backend/src/test/java/com/learningos/evaluation/api/EvaluationSetControllerTest.java`
- `backend/src/test/java/com/learningos/evaluation/api/EvaluationRunControllerTest.java`

### 5.4 RAG KB 证据

- `backend/src/main/java/com/learningos/rag/api/KnowledgeBaseController.java`
- `backend/src/main/java/com/learningos/rag/application/KnowledgeBaseService.java`
- `backend/src/main/java/com/learningos/rag/application/PermissionService.java`
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java`
- `backend/src/test/java/com/learningos/rag/api/KnowledgeBaseControllerTest.java`
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`

### 5.5 CourseAccess legacy caller 证据

- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java`
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java`
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`

### 5.6 Auth / Spring Security 证据

- `backend/pom.xml`
- `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java`
- `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java`
- `backend/src/main/java/com/learningos/config/AuthProperties.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/learningos/common/auth/DevAuthFilterTest.java`

## 6. 审计命令与限制

已执行：

```powershell
rg -n "api[_-]?key|password|secret|token|private[_-]?key|BEGIN (RSA|OPENSSH|PRIVATE)|AKIA|Bearer " backend docs --glob '!backend/target/**' --glob '!**/*.png' --glob '!**/*.jpg'
rg -n "spring-boot-starter-security|oauth2-resource-server|jwk|jwk-set-uri|SecurityFilterChain|@PreAuthorize|OncePerRequestFilter|FilterRegistrationBean" backend\src\main\java backend\src\test\java backend\pom.xml
mvn -q org.owasp:dependency-check-maven:check -DskipTests -Dformat=HTML -DfailBuildOnCVSS=11
mvn -q dependency:tree "-Dincludes=org.springframework.boot:spring-boot-starter-security,org.springframework.security:spring-security-oauth2-resource-server,org.springframework.security:spring-security-oauth2-jose"
git rev-parse --show-toplevel
```

结果：

- Secrets 关键字扫描未在本次证据范围确认真实生产密钥；命中主要为文档规则、测试 token/JWT 字段、配置键名和安全测试样例。仍建议后续使用专用 secret scanner 扫全仓和历史。
- `git rev-parse --show-toplevel` 返回 `fatal: not a git repository`，因此无法执行 git 历史 secrets 扫描。
- OWASP dependency-check 未完成：工具需要下载 RetireJS/NVD 数据，但当前环境无法解析 `raw.githubusercontent.com`，报 `UnknownHostException`。依赖 CVE 审计未形成有效结论。
- `dependency:tree` 对 Spring Security/OAuth2/JWK 相关 includes 无输出；结合 `pom.xml` 与 `rg`，当前未引入 Spring Security Resource Server/JWK。

## 7. 结论

下一刀建议做 **P3-4-N PromptVersion 管理 API roles-first RBAC 与 promptText 暴露收口**。

理由：

1. 它是当前最高可利用、最高 blast radius 的权限缺口：无鉴权管理 API + 可改写 active prompt + 暴露完整 promptText。
2. 切片足够小，主要集中在 `agent` 模块 PromptVersion Controller/Service/DTO/Test。
3. 不需要新依赖、不需要 schema、不需要 Spring Security 全局改造。
4. 能继续沿用 P3-4-K/M 已建立的 Bearer roles + spoofed header RED 模式。
5. Evaluation / RAG KB / legacy CourseAccessService / formal OAuth2-JWK 都应继续保留为后续 P3-4 切片，但不应抢在这个裸露管理面之前。
