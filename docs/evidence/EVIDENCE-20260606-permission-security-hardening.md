# Evidence - P3-4 权限与安全加固

## 1. 范围

本证据覆盖 P3-4 的最小安全收口切片：

- Profile owner 校验
- Learning Path 创建 / 查询 owner 校验
- `GET /api/analytics/overview` admin-only
- `GET /api/health` 敏感配置与部署指纹收敛
- RAG mixed `kbIds` strict 拒绝，且拒绝时不写 query/citation 伪成功证据

不覆盖完整生产认证、JWT/RBAC、课程/班级授权模型、资源和答题记录全量权限矩阵。

## 2. 代码证据

| 文件 | 证据 |
|---|---|
| `backend/src/main/java/com/learningos/learning/api/ProfileController.java` | 请求体 `learnerId` 与 `CurrentUserService.currentUserId()` 不一致时返回 `FORBIDDEN`，阻断画像写入。 |
| `backend/src/main/java/com/learningos/learning/api/LearningPathController.java` | 创建路径前校验 owner；查询路径后返回前校验 `response.learnerId()`。 |
| `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java` | `/overview` 限制 `admin` 访问。 |
| `backend/src/main/java/com/learningos/health/application/HealthService.java` | Health metadata 只返回 `configured` 布尔值，不返回 URL、endpoint、bucket、provider/model 细节。 |
| `backend/src/main/java/com/learningos/rag/application/PermissionService.java` | 新增 `requireReadableKbIds(...)`，任一 requested KB 不可读即 `FORBIDDEN`。 |
| `backend/src/main/java/com/learningos/rag/application/RagQueryService.java` | 普通 query 与 replay/hash 路径均改用 strict KB 校验。 |
| `backend/src/main/java/com/learningos/rag/api/ChatController.java` | 补齐 `GET /api/rag/query` 查询入口，复用同一 strict KB 校验服务路径。 |

## 3. 测试证据

### Red 证据

新增测试后、实现前运行：

```bash
cd backend
mvn "-Dtest=LearningWorkflowControllerTest,LearningWorkflowServiceTest,AnalyticsControllerTest,HealthControllerTest,ChatControllerTest,RagQueryServiceTest" test
```

结果：失败，37 个测试中 6 个失败。失败点符合预期：

- Profile 跨 learner 访问返回 200 而非 403。
- Learning Path 跨 learner 创建 / 查询返回 200 而非 403。
- analytics overview 非 admin 返回 200 而非 403。
- health 响应包含 `jdbc:h2:mem:health_test`、`localhost:9000`、`learning-os-documents`。
- RAG mixed allowed/forbidden `kbIds` 没有抛出异常。

代码审查后补充 `GET /api/rag/query` 覆盖。新增 `ChatControllerTest.getRagQueryUsesSameStrictQueryServicePath` 后、实现前运行：

```bash
cd backend
mvn "-Dtest=ChatControllerTest" test
```

结果：失败，`GET /api/rag/query` 未命中 handler。

### Green 证据

实现后运行同一聚焦命令：

```bash
cd backend
mvn "-Dtest=LearningWorkflowControllerTest,LearningWorkflowServiceTest,AnalyticsControllerTest,HealthControllerTest,ChatControllerTest,RagQueryServiceTest" test
```

结果：`BUILD SUCCESS`，`Tests run: 38, Failures: 0, Errors: 0, Skipped: 0`。

补齐 `GET /api/rag/query` 后单测复核：

```bash
cd backend
mvn "-Dtest=ChatControllerTest" test
```

结果：`BUILD SUCCESS`，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。

### 全量后端回归

```bash
cd backend
mvn test
```

结果：`BUILD SUCCESS`，`Tests run: 217, Failures: 0, Errors: 0, Skipped: 1`。

说明：全量回归前发现一个旧测试仍期待原始 provider 错误文本 `provider unavailable`，已改为验证安全合同 `MODEL_PROVIDER_ERROR` 且不包含原始 provider 错误。

## 4. 安全扫描

### MyBatis `${}` 扫描

```powershell
powershell -ExecutionPolicy Bypass -File C:\Users\wonderful\.codex\skills\java-security-review\scripts\scan-mybatis-dollar.ps1 -ScanDir backend
```

结果：仅命中 `IndexTaskRecoveryScheduler.java` 中 Spring `@Scheduled(fixedDelayString = "${learning-os.rag.index-recovery.fixed-delay:5m}")` 属性占位符，不是 MyBatis SQL 注入风险。

### hardcoded-secret 脚本

```powershell
powershell -ExecutionPolicy Bypass -File C:\Users\wonderful\.codex\skills\java-security-review\scripts\find-hardcoded-secrets.ps1 -ScanDir backend
```

结果：脚本自身解析失败，原因是本地 skill 脚本第 37 行字符串缺少终止引号。本轮不声明该脚本通过。

### 手工 `rg` 替代扫描

```powershell
rg --line-number --hidden --glob '!target/**' --glob '!backend/target/**' --glob '!frontend/node_modules/**' --glob '!*.md' '(?i)(api[_-]?key|secret[_-]?key|password|private[_-]?key|access[_-]?key)\s*[:=]' backend/src docs/security docs/memory docs/plans docs/tasks docs/specs docs/context docs/product docs/requirements
```

结果命中：

- `backend/src/main/resources/application-test.yml:9` 空测试 password。
- `backend/src/main/resources/application.yml:11` `${DB_PASSWORD:learning_os}` 本地默认值。
- `backend/src/main/resources/application.yml:37-38` `${MINIO_ACCESS_KEY:minioadmin}` / `${MINIO_SECRET_KEY:minioadmin}` 本地默认值。
- `backend/src/test/java/com/learningos/agent/application/AgentRunRecorderTest.java:374` 用于脱敏测试的示例 `password=plain-secret`。
- `backend/src/test/java/com/learningos/health/api/HealthControllerTest.java:35-36` 用于验证 health 不泄露的测试配置。

风险结论：未发现本轮新增真实密钥；现有本地默认凭据应在生产部署时由环境变量或 secret manager 覆盖。

## 5. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只做 HTTP/current-user 边界与委托；RAG 权限判断在 service。 |
| Frontend rules | N/A | 本轮未修改 frontend。 |
| Agent / RAG rules | PASS | RAG retrieval 前严格校验所有 requested KB；拒绝时不写 query/citation。 |
| Security | PASS | 权限检查在后端代码中；未新增依赖；未新增 schema；Health 不暴露配置细节。 |
| API / Database | PASS | API 行为与 SPEC 一致；无数据库迁移。 |
