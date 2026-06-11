# RUN-20260606 后端架构 P3 Security & Quality 并行审查报告

**子代理**：Security & Quality  
**审查时间**：2026-06-06  
**审查范围**：`docs/planning/backend-architecture-todolist.md` 尚未完成项，聚焦 P3-1、P3-4、依赖/安全/测试风险、P3-5 运维告警安全边界。  
**执行边界**：只读分析；未修改代码；仅创建本报告。  
**总体风险级别**：HIGH

## 1. 审查输入与方法

已按要求读取：

- `AGENTS.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/security/SECRET_POLICY.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/harness/TEST_COMMANDS.md`

额外只读核查：

- 使用 `rg`，并排除 `frontend/node_modules` 与 `backend/target`。
- 扫描硬编码 secret / token / password 关键字。
- 扫描 RAG、权限、teacher/student、Flyway/MySQL、危险查询/命令执行模式。
- 读取关键 Java/Spring 代码、Flyway smoke 测试、Controller/Service 测试。

未执行会产生构建产物或外部状态的命令：

- 未运行 `mvn test` / `mvn dependency-check` / `npm audit`，因为本任务明确只允许创建一个报告文件，Maven/审计命令通常会生成 `target/`、缓存或锁文件。
- `git log -p` 历史 secret 扫描尝试失败：当前 `D:\多元agent` 不是 git repository，无法审查 git history。

## 2. 当前证据

### 2.1 MySQL/Flyway 真实验证

证据：

- `backend/pom.xml:60-70` 已引入 `flyway-core`、`flyway-mysql`、`mysql-connector-j`。
- `backend/docker-compose.yml:2-13` 提供 MySQL 8.0 服务；`backend/docker-compose.yml:21-32` 提供 MinIO。
- `backend/src/main/resources/application.yml:7-17` 生产默认使用 MySQL JDBC URL，`spring.jpa.hibernate.ddl-auto=validate`，Flyway enabled。
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java:12-39` 明确指出 V1/V3/V4/V5 含 MySQL-only 方言，且 `application-test.yml` 使用 H2 MySQL mode 并禁用 Flyway。
- `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java:19-27` 存在显式开启的 MySQL smoke test，`@EnabledIfSystemProperty(named = "learningos.mysql.smoke", matches = "true")`。
- `MysqlMigrationSmokeTest.java:33-46` 从空 schema 通过 Flyway target `5` 迁移 V1-V5。
- `MysqlMigrationSmokeTest.java:59-90` 校验 MySQL 8 版本、关键表、V4/V5 请求幂等字段/索引、InnoDB、utf8mb4、临时 procedure 清理、`flyway_schema_history`。
- `docs/context/CONTEXT-20260606-mysql-migration-smoke.md` 已定义执行命令，包括普通跳过命令和真实 MySQL smoke 命令。

判断：

- “实现路径”已经存在，但 `docs/planning/backend-architecture-todolist.md:178-180` 仍标记 P3-1 未完成。
- 未在已读 evidence/acceptance 中发现 `MysqlMigrationSmokeTest` 实际连接 MySQL 8 成功的验收报告；因此 P3-1 不能视为生产闭环完成。
- 当前 smoke 只 target `5`，与 `DATABASE_MEMORY.md` 记录的 V6-V15 迁移存在覆盖缺口。若后续 P3-1 只验证 V1-V5，无法证明 V6-V15 的 MySQL 方言、约束和索引在真实 MySQL 中可执行。

### 2.2 RAG `kbIds` 越权边界

已实现证据：

- `PermissionService.filterAllowedKbIds(...)` 在 `backend/src/main/java/com/learningos/rag/application/PermissionService.java:50-63` 按请求 `kbIds` 加载未删除 KB，并逐个检查读权限。
- `PermissionService.canReadKnowledgeBase(...)` 在 `PermissionService.java:89-96` 允许 owner、`PUBLIC`、显式 `OWNER/READ/WRITE` 权限读取。
- `RagQueryService.query(...)` 在 `backend/src/main/java/com/learningos/rag/application/RagQueryService.java:147-154` 先做内容安全检查，再调用 `permissionService.filterAllowedKbIds(...)`；没有可访问 KB 时返回 `FORBIDDEN`，且不继续 retrieval。
- `RagQueryService.queryResolved(...)` 在 `RagQueryService.java:172-183` 只把 `allowedKbIds` 传给 `chunkService.retrieveAllowedChunks(...)`。
- `RagQueryService.persistQueryLog(...)` 在 `RagQueryService.java:199-225` 存储的是 `allowedKbIds`，不是原始伪造 `requestedKbIds`。
- `RagQueryServiceTest.refusesQueryWhenRequestedKnowledgeBaseIsNotAllowed` 位于 `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java:290-298`，验证 alice 请求 bob 的 private KB 时抛出异常，且 `kb_query_log` 与 `source_citation` 均不写入。
- `PermissionServiceTest.filtersRequestedKnowledgeBasesByOwnerAndVisibility` 位于 `backend/src/test/java/com/learningos/rag/application/PermissionServiceTest.java:39-52`，验证 owner/public/private-other 过滤。

风险与缺口：

- 当前测试覆盖了“全部 `kbIds` 越权”的拒绝场景，但未看到“混合 `kbIds`：一个合法、一个越权”的安全语义测试。当前实现会静默丢弃越权 KB 并继续用合法 KB 回答。这可避免泄露越权内容，但可能掩盖攻击尝试，建议为 API 层明确策略：`strict` 模式返回 403，或记录安全审计事件。
- 未看到 Chat/Orchestrator API 层对 forged `kbIds` 的 MockMvc 安全测试，只看到 Service 层测试和 Orchestrator 功能测试引用。P3-4 明确要求“权限渗透测试，尤其 RAG `kbIds` 越权检索”，应补 Controller/Workflow 入口的端到端断言。
- `PermissionService.loadUserPermissions(...)` 使用 `findAll()` 再内存过滤，属于性能/可用性风险；安全上可能导致大权限表下超时，形成 DoS 面。建议改为 repository 查询 `subjectType + subjectId`。

### 2.3 teacher/student 边界

已实现证据：

- Review Gate：`ReviewGovernanceService.assertReviewerAccess(...)` 在 `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java:104-108` 只允许字面值 `teacher` 与 `admin`。
- Review list/decision 在 `ReviewGovernanceService.java:44-58` 先执行 `assertReviewerAccess(...)`，再读取 review 详情，避免学生通过 reviewId 枚举泄露详情。
- `ResourceReviewControllerTest.studentCannotListResourceReviews` 在 `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java:51-70` 验证学生 list 返回 403 且响应不包含 task/review/resource id。
- `ResourceReviewControllerTest.studentCannotSubmitReviewDecision` 在 `ResourceReviewControllerTest.java:72-97` 验证学生 decision 返回 403 且不泄露详情。
- 学生 analytics：`AnalyticsController.studentSummary(...)` 在 `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java:38-45` 要求 `currentUserId == learnerId`；测试 `AnalyticsControllerTest.studentSummaryRejectsCrossLearnerAccess` 存在于 `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java:300`。
- 教师班级 analytics：`AnalyticsService.teacherClassSummary(...)` 在 `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java:200-206` 要求 admin 或 course.teacherId；测试 `teacherClassSummaryRejectsForeignTeacherAndStudent` 位于 `AnalyticsControllerTest.java:344`。
- Resource generation：`ResourceGenerationService.createTask(...)` 与 `createTaskWithContext(...)` 通过 `ensureLearnerOwner(...)` 检查 `userId == request.learnerId()`；证据位于 `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java:91-92`、`129`、`620-623`。
- Assessment：`AssessmentService` 在 `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java:90-91`、`124-125` 对提交/重放进行 learner owner 检查。

未实现或弱实现证据：

- 当前认证是开发替代：`DevAuthFilter` 在 `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java:23-34` 直接信任 `X-User-Id`，未签名、未认证、角色固定 `USER`。任何调用者可伪造 `X-User-Id: admin` 或 `teacher`。
- `CurrentUserService.currentUserId()` 在 `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java:10-13` 无上下文时回退 `dev_user`，生产环境若误用会产生匿名默认身份。
- `ProfileController.extract(...)` 在 `backend/src/main/java/com/learningos/learning/api/ProfileController.java:23-25` 未注入 `CurrentUserService`，直接信任 `ProfileExtractRequest.learnerId`。
- `LearningPathController.create/get(...)` 在 `backend/src/main/java/com/learningos/learning/api/LearningPathController.java:25-32` 未注入 `CurrentUserService`；`LearningWorkflowService.generatePath(...)` 在 `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java:110-123` 直接信任请求体 `learnerId`；`getPath(...)` 在 `LearningWorkflowService.java:140-153` 仅按 pathId 返回，不校验 owner。
- `ReviewGovernanceService.assertReviewerAccess(...)` 只基于字符串 userId 判断，而不是角色、课程范围、班级授权。`teacher_123` 这类真实教师 id 不能审核；但伪造 `teacher` 又能审核全量队列。
- `ReviewGovernanceService.listResourceReviews(...)` 在 `ReviewGovernanceService.java:44-52` 对 `teacher/admin` 返回全量/按 status 的 review，没有 course/class scope 过滤。
- `AnalyticsController.overview(...)` 在 `AnalyticsController.java:33-36` 无 admin/teacher 权限控制，可能暴露全局 agent/task/token/review 聚合信息。
- `HealthController.health(...)` 在 `backend/src/main/java/com/learningos/health/api/HealthController.java:19-21` 无权限控制；`HealthService.currentHealth()` 在 `backend/src/main/java/com/learningos/health/application/HealthService.java:37-52` 暴露环境、DB URL、Redis host、MinIO endpoint/bucket、model provider/model name。未泄露密码，但属于部署指纹信息。

### 2.4 依赖、secret、危险模式

依赖证据：

- `backend/pom.xml` 使用 Spring Boot parent `3.5.7`、Spring AI BOM `1.0.3`、MinIO `8.5.17`、MySQL Connector/J、Flyway MySQL、Redis starter、Actuator。
- 未发现 Maven dependency audit 执行证据。本轮未运行审计命令以避免生成文件。

Secret 扫描证据：

- `backend/.env.example` 含示例密码：`MYSQL_PASSWORD=learning_os`、`MYSQL_ROOT_PASSWORD=learning_os_root`、`MINIO_ROOT_PASSWORD=minioadmin`、`DB_PASSWORD=learning_os`、`MINIO_SECRET_KEY=minioadmin`。
- `backend/src/main/resources/application.yml:10-11` 默认 DB 用户/密码为 `learning_os`。
- `backend/src/main/resources/application.yml:36-38` 默认 MinIO access/secret 为 `minioadmin`。
- `backend/docker-compose.yml:6-9`、`26-27` 默认 MySQL/MinIO 凭据同上。
- 这些是开发默认值/示例值，未发现真实云 API key 或私钥；但若直接以默认配置部署，会违反 `docs/security/SECRET_POLICY.md` 的生产 secret 管理要求。
- git history secret 扫描失败，因为当前目录不是 git repository。

危险模式证据：

- 未发现生产代码中 `Runtime.getRuntime`、`ProcessBuilder`、原生 SQL 拼接、`JdbcTemplate`、`EntityManager.createNativeQuery` 风险点。
- `MysqlMigrationSmokeTest` 中使用 `Statement.execute("DROP DATABASE IF EXISTS `" + schemaName + "`")`，但 `assertSmokeSchemaName(...)` 在 `MysqlMigrationSmokeTest.java:115-125` 限制 schema 名必须匹配 `[A-Za-z0-9_]+` 且默认要求包含 `migration_smoke`，测试路径安全边界合理。
- JPA `@Query` 主要为静态 JPQL，未发现用户输入字符串拼接。

## 3. 已实现与未实现清单

### 已实现

- MySQL/Flyway 依赖和生产配置基础已存在。
- `backend/docker-compose.yml` 已提供 MySQL 8 本地服务。
- `MysqlMigrationSmokeTest` 已提供显式开启的 V1-V5 真实 MySQL smoke 路径，并包含破坏性 schema 名保护。
- `SchemaConvergenceMigrationTest` 已覆盖 V1-V15 迁移文本断言，并明确 H2/Flyway 不等价。
- RAG query service 已在 chunk lookup 前过滤 `allowedKbIds`。
- 全部越权 `kbIds` 请求已有 Service 层拒绝测试，且断言不写 query/citation 成功 artifact。
- Review Gate 已阻止普通学生 list/decision，并避免错误响应泄露 review/task/resource id。
- Analytics student summary 已限制学生只能访问自己。
- Teacher class summary 已限制 course teacher 或 admin。
- Assessment/resource generation 已对 `learnerId` 做 owner 检查。
- Token budget governance endpoint/service 已存在，并限制 admin 字面身份。
- Health 输出未直接暴露 password/secret。

### 未实现 / 未闭环

- P3-1 TODO 未更新；未发现 MySQL 8 smoke 实际成功 evidence/acceptance。
- MySQL smoke 仅覆盖 V1-V5，未覆盖 V6-V15；当前迁移历史已到 V15。
- 未执行 dependency vulnerability audit；未形成依赖安全报告。
- 未形成 git history secret 审查，原因是当前目录不是 git repo。
- 生产级认证/RBAC 未实现，`X-User-Id` 可伪造是当前最高优先级安全风险。
- Review Gate 的 `teacher/admin` 是临时字面值权限，不具备 course/class scope。
- RAG `kbIds` 越权缺少 API/Orchestrator 端到端渗透测试，混合合法+越权 `kbIds` 策略未明确。
- `ProfileController`、`LearningPathController`/`LearningWorkflowService` 直接信任请求体 `learnerId` 或 pathId，存在 IDOR 风险。
- `AnalyticsController.overview` 未做 admin/teacher 权限控制。
- `HealthController` 无权限控制，暴露部署指纹信息。
- P3-5 结构化日志、Micrometer 指标、深度健康检查、慢查询/慢模型/无引用回答/审核积压告警未见实现闭环；当前只能查询部分 analytics/health，不等同运维告警。
- 默认开发凭据仍存在于配置默认值与 compose 示例中；生产配置缺少“默认值禁止启动”的强制检查。

## 4. 安全发现与优先级

### HIGH-1：开发身份头可伪造，导致所有基于 userId 字符串的权限可被绕过

**类别**：OWASP A01 Broken Access Control / A07 Identification and Authentication Failures  
**位置**：`backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java:23-34`，`CurrentUserService.java:10-13`  
**可利用性**：远程，未认证调用者只需设置 `X-User-Id: admin` 或 `X-User-Id: teacher`。  
**影响范围**：Review Gate、analytics、token budget governance、teacher/admin 逻辑、所有 currentUserId 业务边界。

问题：

```java
String userId = request.getHeader(USER_ID_HEADER);
if (userId == null || userId.isBlank()) {
    userId = DEFAULT_USER_ID;
}
UserContextHolder.setCurrentUser(new UserContext(userId, userId, List.of("USER")));
```

修复示例：

```java
@Component
@Profile("dev")
public class DevAuthFilter extends OncePerRequestFilter {
    // 仅 dev/test profile 启用。
}
```

```java
@Configuration
@EnableWebSecurity
@Profile("!dev & !test")
class SecurityConfig {
    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").hasRole("OPS")
                        .requestMatchers("/api/analytics/overview").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt())
                .build();
    }
}
```

并将 `CurrentUserService` 改为从 `SecurityContext` 解析已认证 principal，不允许生产 fallback：

```java
public String currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
        throw new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required");
    }
    return authentication.getName();
}
```

### HIGH-2：Profile/Learning Path 接口存在 IDOR，直接信任 `learnerId`/`pathId`

**类别**：OWASP A01 Broken Access Control  
**位置**：`ProfileController.java:23-25`，`LearningPathController.java:25-32`，`LearningWorkflowService.java:93-123`、`140-153`  
**可利用性**：远程，伪造请求体 `learnerId` 或枚举 `pathId`。  
**影响范围**：学生画像、学习路径、profile snapshot、learning_event、mastery 相关上下文。

问题示例：

```java
@PostMapping("/dialogue/extract")
public ApiResponse<ProfileExtractResponse> extract(@Valid @RequestBody ProfileExtractRequest request) {
    return ApiResponse.success(learningWorkflowService.extractProfile(request));
}
```

修复示例：

```java
@PostMapping("/dialogue/extract")
public ApiResponse<ProfileExtractResponse> extract(@Valid @RequestBody ProfileExtractRequest request) {
    String currentUserId = currentUserService.currentUserId();
    if (!currentUserId.equals(request.learnerId())) {
        throw new ApiException(ErrorCode.FORBIDDEN, "Learner profile access denied");
    }
    return ApiResponse.success(learningWorkflowService.extractProfile(request));
}
```

```java
@GetMapping("/{pathId}")
public ApiResponse<LearningPathResponse> get(@PathVariable String pathId) {
    LearningPathResponse response = learningWorkflowService.getPath(pathId);
    if (!currentUserService.currentUserId().equals(response.learnerId())) {
        throw new ApiException(ErrorCode.FORBIDDEN, "Learning path access denied");
    }
    return ApiResponse.success(response);
}
```

### HIGH-3：Review Gate 临时 teacher/admin 权限无课程/班级范围，教师可读写全量审核队列

**类别**：OWASP A01 Broken Access Control / A04 Insecure Design  
**位置**：`ReviewGovernanceService.java:44-58`、`104-108`  
**可利用性**：远程，已登录/伪造 teacher 身份。  
**影响范围**：所有资源审核记录、审核决策、发布状态。

问题：

```java
private void assertReviewerAccess(String reviewerUserId) {
    if (!"teacher".equals(reviewerUserId) && !"admin".equals(reviewerUserId)) {
        throw new ApiException(ErrorCode.FORBIDDEN, REVIEW_ACCESS_DENIED);
    }
}
```

修复示例：

```java
private void assertCanReview(String reviewerUserId, ResourceGenerationTask task) {
    if (roleService.isAdmin(reviewerUserId)) {
        return;
    }
    Course course = courseRepository.findById(task.getGoalId())
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Course not found"));
    if (!reviewerUserId.equals(course.getTeacherId())) {
        throw new ApiException(ErrorCode.FORBIDDEN, "Resource review access denied");
    }
}
```

List 接口也应先按 reviewer scope 查询，避免先拉全量 review 后过滤。

### MEDIUM-1：RAG 混合 `kbIds` 越权策略未明确，缺少 API/Workflow 渗透测试

**类别**：OWASP A01 Broken Access Control / A09 Security Logging and Monitoring Failures  
**位置**：`PermissionService.java:50-63`，`RagQueryService.java:147-154`  
**可利用性**：远程，提交合法 KB + 越权 KB 混合列表。  
**影响范围**：不会直接读取越权 chunk，但可能掩盖攻击尝试，且 API 语义不清晰。

建议修复示例：

```java
List<String> requested = normalizeKbIds(requestedKbIds);
List<String> allowed = permissionService.filterAllowedKbIds(userId, requested);
if (allowed.size() != requested.stream().distinct().count()) {
    securityAuditLogger.warn("RAG_KB_IDS_FORBIDDEN userId={} requested={} allowed={}", userId, requested, allowed);
    throw new ApiException(ErrorCode.FORBIDDEN, "One or more knowledge bases are not accessible");
}
```

若产品要求 partial success，也必须记录安全事件并在响应 metadata 中避免暴露越权 KB 是否存在。

### MEDIUM-2：全局 analytics overview 与 health 无权限控制，暴露运营与部署指纹

**类别**：OWASP A01 Broken Access Control / A05 Security Misconfiguration  
**位置**：`AnalyticsController.java:33-36`，`HealthController.java:19-21`，`HealthService.java:37-52`  
**可利用性**：远程，未认证或伪造身份。  
**影响范围**：系统运行状态、环境名、DB URL、Redis host、MinIO endpoint/bucket、model provider/model name、全局 agent/token/review 聚合。

修复示例：

```java
@GetMapping("/overview")
public ApiResponse<AnalyticsOverview> overview() {
    if (!roleService.isAdmin(currentUserService.currentUserId())) {
        throw new ApiException(ErrorCode.FORBIDDEN, "Analytics overview requires admin access");
    }
    return ApiResponse.success(analyticsService.overview());
}
```

```java
private ComponentStatus databaseStatus() {
    return ComponentStatus.configured("database configuration loaded", Map.of(
            "configured", dataSourceProperties.getUrl() != null && !dataSourceProperties.getUrl().isBlank()
    ));
}
```

### MEDIUM-3：MySQL smoke 覆盖滞后于迁移版本，真实数据库兼容性仍未完全验证

**类别**：OWASP A08 Software and Data Integrity Failures / Quality Risk  
**位置**：`MysqlMigrationSmokeTest.java:33-46`，`DATABASE_MEMORY.md` V6-V15 迁移记录  
**可利用性**：非直接攻击，但生产部署/回滚失败会影响可用性与数据完整性。  
**影响范围**：V6-V15 review governance、RAG replay、document upload、profile snapshot、evaluation、agent tool call governance。

建议：

```java
Flyway flyway = Flyway.configure()
        .dataSource(config.schemaUrl(), config.username(), config.password())
        .locations("classpath:db/migration")
        .load(); // 不设置 target，验证当前全量 V1-V15
```

保留 V1-V5 专项 smoke，同时新增 full-current smoke 或将 target 参数化。

### LOW-1：开发默认凭据存在，生产缺少默认值禁止启动策略

**类别**：OWASP A02 Cryptographic Failures / A05 Security Misconfiguration  
**位置**：`application.yml:10-11`、`36-38`，`docker-compose.yml:6-9`、`26-27`，`.env.example`  
**可利用性**：取决于部署是否使用默认值。  
**影响范围**：数据库、MinIO。

修复示例：

```java
@PostConstruct
void rejectDefaultProductionSecrets() {
    if ("prod".equals(appProperties.environment())
            && ("learning_os".equals(dataSourceProperties.getPassword())
            || "minioadmin".equals(storageProperties.secretKey()))) {
        throw new IllegalStateException("Default credentials are not allowed in production");
    }
}
```

## 5. 安全测试矩阵

| 测试主题 | 当前证据 | 缺口 | 建议用例 |
|---|---|---|---|
| RAG 全部 `kbIds` 越权 | `RagQueryServiceTest.refusesQueryWhenRequestedKnowledgeBaseIsNotAllowed` | 仅 Service 层 | MockMvc: `POST /api/chat/query` alice 请求 bob private KB 返回 403，响应不含 kb/document/chunk 信息，DB 无 `kb_query_log`/`source_citation` |
| RAG 混合 `kbIds` | 未见明确测试 | 策略未定义 | alice 请求 `[kb_owned, kb_hidden]`：若 strict，返回 403 且无检索；若 partial，响应 sources 只能来自 `kb_owned` 且写安全审计 |
| RAG Orchestrator forged `kbIds` | Orchestrator/RAG 功能测试存在，但未定位到专门越权测试 | Workflow 入口缺渗透覆盖 | `POST /api/orchestrator/workflows` type `RAG_QA` forged `kbIds`，断言 failed evidence 安全、无成功 query/citation artifact |
| RAG replay + 权限变化 | replay snapshot 已有 same payload/409 测试 | 未测试权限撤销后 replay 是否仍返回历史 sources | alice 首次可读 KB 生成 snapshot，撤销权限后同 requestId replay：应拒绝或只允许 owner 历史审计读取，需产品定策 |
| Profile IDOR | 未见 current user 检查 | 高风险 | alice 调用 `/api/profile/dialogue/extract` with `learnerId=bob` 返回 403，无 bob profile/event 写入 |
| Learning Path IDOR create | 未见 current user 检查 | 高风险 | alice 创建 `learnerId=bob` path 返回 403，无 bob path/node/event 写入 |
| Learning Path IDOR get | 未见 path owner 检查 | 高风险 | bob 枚举 alice pathId 返回 403/404，不返回 profileSnapshot/nodes |
| Resource task owner | `ResourceGenerationService.ensureLearnerOwner` | 需要 API 级覆盖扩展 | bob GET alice taskId 返回 403，learner-resources 未发布或非 owner 返回 403 |
| Assessment owner | `AssessmentService` owner check | 已有基础，继续覆盖 replay | bob 使用 alice `requestId` replay 返回 403/404，不返回 answer snapshot |
| Review student deny | `ResourceReviewControllerTest` 已覆盖 | teacher scope 未覆盖 | 非课程教师不能 list/decision；课程教师只能看本课程 review |
| Analytics student | `studentSummaryRejectsCrossLearnerAccess` | overview 未控 | `/api/analytics/overview` 非 admin 返回 403 |
| Teacher class scope | `teacherClassSummaryRejectsForeignTeacherAndStudent` | 依赖真实 RBAC 前仍临时 | 使用真实 role/course assignment 表后重测 |
| Health exposure | 未见权限测试 | 中风险 | 非 ops/admin 访问 `/api/health` 不返回 DB URL/MinIO endpoint/model name |
| MySQL migration | `MysqlMigrationSmokeTest` 存在 | 未见运行证据；只到 V5 | MySQL 8 空库跑 V1-V15，归档 evidence/acceptance |
| Dependency audit | 未执行 | 未闭环 | Maven dependency audit / OWASP dependency-check 或 GitHub Dependabot 报告归档 |
| Secret scan | rg 当前工作树完成，git history 失败 | 非 git repo | 在真实 repo 中跑 history secret scan；CI 加 secret scanning |

## 6. 是否需要 GitHub/官方文档研究

结论：需要官方文档研究，不建议 GitHub 代码借鉴作为主路径。

原因：

- 认证/RBAC、安全 header、Actuator/health 暴露、JWT Resource Server 属于标准 Spring Security/Spring Boot 安全配置，应以 Spring Security、Spring Boot Actuator、Micrometer、Flyway 官方文档为准。
- MySQL/Flyway smoke 的正确性应参考 Flyway 官方 target/migrate/info/clean 配置与 MySQL Connector/J 官方 URL 安全参数，不需要从 GitHub 项目复制实现。
- 依赖安全应参考 Maven/OWASP Dependency-Check、GitHub Dependabot、Spring Boot supported versions、MinIO Java SDK advisories。
- RAG `kbIds` 越权是项目领域授权问题，GitHub 参考价值低；需要项目内 threat model 和 API 契约明确 strict/partial 策略。

建议研究清单：

- Spring Security OAuth2 Resource Server / JWT 官方文档。
- Spring Boot Actuator endpoint exposure/security 官方文档。
- Micrometer metrics + Spring Boot Observability 官方文档。
- Flyway Java API / MySQL support 官方文档。
- OWASP ASVS：V4 Access Control、V7 Error Handling and Logging、V14 Configuration。

## 7. Context Pack 文件边界建议

建议拆成三个 Context Pack，避免 P3-1/P3-4/P3-5 互相踩文件。

### Context Pack A：P3-1 MySQL Migration 真实验证

允许修改：

- `backend/src/test/java/com/learningos/migration/MysqlMigrationSmokeTest.java`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `backend/docker-compose.yml`（仅 smoke 必需）
- `docs/evidence/EVIDENCE-20260606-mysql-migration-smoke.md`
- `docs/acceptance/ACCEPT-20260606-mysql-migration-smoke.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/DATABASE_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/changelog/CHANGELOG.md`

禁止修改：业务 Controller/Service、frontend、RAG/Agent 业务逻辑、生产依赖。

测试命令：

```bash
cd backend && mvn "-Dtest=SchemaConvergenceMigrationTest" test
cd backend && mvn "-Dtest=MysqlMigrationSmokeTest" test
cd backend && mvn "-Dtest=MysqlMigrationSmokeTest" "-Dlearningos.mysql.smoke=true" "-Dlearningos.mysql.smoke.url=jdbc:mysql://127.0.0.1:3306/learning_os_migration_smoke?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" "-Dlearningos.mysql.smoke.username=root" "-Dlearningos.mysql.smoke.password=learning_os_root" test
```

### Context Pack B：P3-4 权限与安全加固

允许修改：

- `backend/src/main/java/com/learningos/common/auth/**`
- `backend/src/main/java/com/learningos/learning/api/**`
- `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
- `backend/src/main/java/com/learningos/rag/application/PermissionService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/api/ChatController.java`
- `backend/src/main/java/com/learningos/orchestrator/**`（仅 RAG_QA 权限测试/错误语义必需）
- `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java`
- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java`
- 对应测试文件：`*ControllerTest`、`*ServiceTest`
- 对应 PRD/REQ/SPEC/PLAN/TASK/CONTEXT/EVIDENCE/ACCEPT/changelog/memory 文档

禁止修改：Flyway migration（除非新增 RBAC 表并已完成依赖/DB 评审）、前端、模型接入、索引生产化。

测试命令：

```bash
cd backend && mvn "-Dtest=RagQueryServiceTest,ChatControllerTest,OrchestratorWorkflowControllerTest,LearningWorkflowControllerTest,ResourceReviewControllerTest,AnalyticsControllerTest,AssessmentControllerTest,ResourceGenerationControllerTest" test
cd backend && mvn test
```

### Context Pack C：P3-5 运维告警安全边界

允许修改：

- `backend/src/main/java/com/learningos/health/**`
- `backend/src/main/java/com/learningos/analytics/**`
- `backend/src/main/java/com/learningos/common/trace/**`
- `backend/src/main/resources/application*.yml`
- `backend/src/test/java/com/learningos/health/**`
- `backend/src/test/java/com/learningos/analytics/**`
- `docs/operations/**`
- 对应 workflow 文档

禁止修改：业务权限语义、RAG retrieval 算法、Flyway schema，除非 SPEC 明确。

安全边界要求：

- 健康检查默认不暴露 secret、完整 JDBC URL、bucket 内部路径、provider error 原文。
- 管理端告警只能 admin/ops 查看。
- 结构化日志必须清理 prompt/raw answer/private document/token/password/API key。
- Micrometer label 不允许使用高基数字段：raw userId、question、documentId、prompt、error message。

## 8. 架构漂移风险

当前漂移点：

- `AGENTS.md` 与 `BACKEND_MEMORY.md` 要求“Permission checks happen in backend code”，但 `DevAuthFilter` 仍是开发替代，生产级认证/RBAC 未落地。
- API memory 写明 Auth 为 Bearer token/session cookie，但当前实现依赖 `X-User-Id` header，存在文档与实现漂移。
- P3-4 要求 RAG KB、课程、学习路径、资源、答题记录全部补齐权限检查；当前只在部分 analytics/resource/assessment/RAG 路径实现，Profile/Learning Path/overview/health 仍缺口明显。
- P3-5 要求结构化日志、Micrometer、深度健康、告警；当前只有 TraceFilter、HealthService 和部分 analytics 汇总，尚非运维告警体系。
- P3-1 TODO 状态与仓库中 `MysqlMigrationSmokeTest`/Context Pack 不一致，说明规划状态未同步；但由于缺少 evidence/acceptance，不能简单勾选完成。

## 9. 秘密泄露风险

当前未发现真实 API key、私钥或 OAuth secret。风险主要来自：

- 默认开发凭据散落在 `application.yml`、`docker-compose.yml`、`.env.example`。
- Health endpoint 暴露配置指纹，虽不含 password/secret，但可辅助攻击者定位 DB/Redis/MinIO/model provider。
- `docs/context/CONTEXT-20260606-mysql-migration-smoke.md` 含 smoke 命令中的默认 root password 示例；属于本地默认值，不应复用于生产。
- 当前不是 git repository，历史泄露未能审查。

建议：

- 生产 profile 禁止默认凭据启动。
- CI 加 secret scanning 和 dependency scanning。
- `/api/health` 分层：公开 liveness 只返回 UP；readiness/dependency detail 只给 ops/admin。
- 运行真实 git history secret scan 后再声明“无历史泄露”。

## 10. 安全检查清单

- [x] 当前工作树 secret 关键字扫描完成，未发现真实 API key/私钥；发现默认开发凭据。
- [ ] git history secret 扫描完成：未完成，当前目录不是 git repository。
- [ ] 依赖漏洞审计完成：未完成，本任务只允许创建报告文件，未运行会生成产物的审计命令。
- [x] RAG `kbIds` 服务层权限过滤审查完成。
- [ ] RAG `kbIds` API/Orchestrator 渗透测试完成：未完成。
- [x] Review Gate student deny 证据审查完成。
- [ ] Review Gate course/class scope RBAC 完成：未完成。
- [ ] Profile/Learning Path learner owner 权限完成：未完成。
- [x] Assessment/resource generation owner check 抽查完成。
- [ ] Production authentication 完成：未完成，仍是 `X-User-Id` dev auth。
- [ ] MySQL V1-V15 真实 smoke 验证完成：未完成，仅存在 V1-V5 显式 smoke 路径。
- [ ] P3-5 安全告警闭环完成：未完成。

## 11. 结论

当前后端在 RAG `kbIds` 过滤、Review Gate 学生阻断、部分 learner owner 检查、MySQL smoke 路径方面已有基础实现。但 P3 生产化安全仍应判定为 HIGH 风险，主要因为认证仍可伪造、真实 RBAC/course scope 未落地、Profile/Learning Path 存在 IDOR、MySQL smoke 缺少执行证据且未覆盖 V6-V15、依赖审计和 git history secret scan 未闭环。

建议主线优先级：

1. 先实现生产认证与真实 RBAC/course scope，关闭 `X-User-Id` 伪造入口。
2. 立即补 Profile/Learning Path/overview/health 权限控制与测试。
3. 明确 RAG 混合 `kbIds` 策略，并补 API + Orchestrator 渗透测试矩阵。
4. 执行并归档 MySQL 8 V1-V15 smoke evidence/acceptance。
5. 接入依赖审计、secret scanning、运维健康/告警的安全边界。
