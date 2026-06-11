# P3-3 真实模型 Provider 接入安全风险审查

**角色**：P3-3/P3-4 Security & Quality Expert  
**日期**：2026-06-08  
**范围**：`docs/planning/backend-architecture-todolist.md` 中 P3-3 真实 Spring AI Chat/Embedding provider 接入对安全边界的影响；最小审查 `AiModelGateway`、`ModelCallLog`、`TokenUsageLog`、`Health`、ops alerts、`CurrentUserService`、`StructuredRequestLoggingFilter`。  
**结论风险等级**：**MEDIUM**。当前代码尚未真实外呼 provider，直接暴露面有限；但一旦接入真实 Chat/Embedding SDK，密钥、日志、成本、授权和结构化输出边界会从“占位实现”变成生产高风险边界，必须在 adapter 落地前加约束和测试。

## 1. 审查结论摘要

- Critical：0
- High：0
- Medium：4
- Low：3
- Secrets scan：完成源码/文档扫描，未发现真实模型 API key；命中项主要是环境变量占位、本地 compose 默认密码、测试假 secret、文档安全规则。
- Dependency audit：`mvn -q -DskipTests dependency:tree` 可执行；`org.owasp:dependency-check-maven:check` 因无法解析 `raw.githubusercontent.com` 初始化 RetireJS 数据失败，未形成完整 CVE 结论。
- Git history secrets scan：未完成。当前工作区 `D:\多元agent` 不是 Git 仓库，`git log -p` 返回 `not a git repository`。

## 2. 已有安全保护

1. 模型调用集中在 `AiModelGateway`，未发现业务 Service 直接调用 `ChatClient` / `EmbeddingModel` / provider SDK 的生产代码。当前 `generateStructured` 仍返回占位结构，不真实外呼：`backend/src/main/java/com/learningos/agent/application/AiModelGateway.java:43`。
2. Provider 名称有低基数归一化，未知 provider 会写 `other`，避免把 URL、tenant、key 片段写入 DB/metrics：`AiModelGateway.java:200`、`AgentRunRecorder.java:463`。
3. `agent-resource-v1` 有结构化输出校验，缺 `resources[]`、必填字段或非法 `safetyStatus` 会变成 `STRUCTURED_OUTPUT_INVALID`：`AiModelGateway.java:148`。
4. Provider 异常会被统一为 `MODEL_PROVIDER_ERROR`，不会把 raw provider message、prompt、学生答案、RAG chunk 直接写入失败 evidence：`AiModelGateway.java:119`、`AgentRunRecorder.java:453`。
5. `HealthService` 的模型健康输出只返回布尔配置状态，不返回 provider 名称、模型名、base URL、key 或 raw exception：`backend/src/main/java/com/learningos/health/application/HealthService.java:151`。
6. `StructuredRequestLoggingFilter` 只记录 `traceId/userId/route/status/latencyMs/errorCode`，不记录 body/query/header：`backend/src/main/java/com/learningos/common/trace/StructuredRequestLoggingFilter.java:69`。
7. 生产/预发环境无 Bearer JWT 会 401；有 Bearer 时必须校验，invalid token 不 fallback 到 `X-User-Id`：`backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java:62`、`DevAuthFilter.java:72`。
8. `GET /api/analytics/ops/alerts` 使用 `currentUserService.isAdmin()` 做 admin-only gate：`backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java:97`；`SLOW_MODEL_CALL` 告警只聚合延迟/计数，不输出 raw error：`AnalyticsService.java:868`。

## 3. 主要风险与整改建议

### 1. 真实 Provider Adapter 可能绕过网关安全约束

**Severity**：MEDIUM  
**Category**：OWASP A04 Insecure Design / A05 Security Misconfiguration  
**Location**：`backend/src/main/java/com/learningos/agent/application/AiModelGateway.java:43`、`AiModelGateway.java:90`  
**Exploitability**：需开发引入真实 SDK 后触发；远程用户可通过资源生成/RAG/评测类入口间接触发。  
**Blast Radius**：可能造成 raw prompt、学生数据、RAG chunk、provider error、模型输出绕过脱敏和结构化校验进入 DB、日志、trace 或响应。

**Issue**：当前安全边界在 `generateStructuredWithRetry` 里集中体现，但真实 Chat adapter 尚未实现。后续若在 Service、Agent 或 RAG 层直接注入 `ChatClient` / `ChatModel` / provider SDK，将绕过重试、provider 归一化、结构化校验、safe error 和 model/token evidence 规则。

**Remediation 示例（Java）**：

```java
// BAD: 业务服务直接调用 provider，绕过日志/脱敏/结构化校验
String output = chatClient.prompt(rawPrompt).call().content();
resource.setMarkdownContent(output);

// GOOD: 只能通过 AiModelGateway，返回值必须走 schema 校验和 safe evidence
AiModelGateway.ModelResponse response = aiModelGateway.generateStructuredWithRetry(
        new AiModelGateway.ModelRequest(agentName, prompt, safeContext, promptVersion),
        executionContext,
        agentRunRecorder,
        failureTrace
);
agentRunRecorder.recordSuccessfulModelEvidence(executionContext, trace, response);
```

**安全约束**：
- 禁止在 Controller / Service / Agent / RAG 查询层直接注入 provider SDK。
- `AiModelGateway.ModelResponse` 是唯一模型 evidence 来源。
- 所有 provider error 必须映射为固定错误码，禁止持久化 raw exception。
- 所有 structured output 必须先校验再写业务表。

### 2. Token/Cost 从估算切换到真实 usage 时存在计费与预算误判风险

**Severity**：MEDIUM  
**Category**：OWASP A09 Security Logging and Monitoring Failures / A04 Insecure Design  
**Location**：`AiModelGateway.java:180`、`AgentRunRecorder.java:123`、`AgentRunRecorder.java:174`、`backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java:1296`  
**Exploitability**：远程认证用户可通过大量模型调用放大成本；错误 usage 会影响预算、告警和审计。  
**Blast Radius**：成本治理、token 预算、ops alerts、异常调用识别不可信，可能导致超预算调用未降级或误封正常调用。

**Issue**：当前 `estimateUsage` 以字符数估算且 cost 为 `0.0`。真实 provider 接入后必须从 provider usage 中提取 prompt/completion/total token 和成本；如果 provider 不返回 usage，不能静默记 0 并当作成功成本，而应标记 `usageSource=ESTIMATED/MISSING` 或至少使用保守估算并触发治理告警。当前 `token_usage_log` 表没有 usage 来源字段，短期可先在安全报告和测试中约束 adapter 行为。

**Remediation 示例（Java）**：

```java
// BAD: provider 没返回 usage 时静默写 0，预算治理失真
new AiModelGateway.TokenUsage(0, 0, 0, 0.0);

// GOOD: 优先使用 provider usage；缺失时保守估算并打固定错误/状态供告警识别
AiModelGateway.TokenUsage usage = providerUsage
        .map(u -> new AiModelGateway.TokenUsage(
                Math.max(0, u.promptTokens()),
                Math.max(0, u.completionTokens()),
                Math.max(0, u.totalTokens()),
                costCalculator.estimateUsd(provider, model, u)
        ))
        .orElseGet(() -> conservativeUsageEstimate(request, responseContent));
```

**安全约束**：
- token 不得为负数；`totalTokens` 必须等于或大于 prompt/completion 合理和。
- cost 必须使用 provider/model 白名单价格表，未知模型写 `0` 的同时触发治理告警或标记未知成本。
- metrics tag 中的 `model` 必须低基数归一化，不能使用 deployment URL 或 tenant-specific model id。

### 3. 密钥配置边界尚未覆盖真实 Spring AI 属性

**Severity**：MEDIUM  
**Category**：OWASP A02 Cryptographic Failures / A05 Security Misconfiguration  
**Location**：`backend/src/main/resources/application.yml:63`、`backend/src/main/java/com/learningos/config/AiModelProperties.java:5`、`HealthService.java:151`  
**Exploitability**：配置错误或日志输出即可泄漏；通常为运维/开发误配触发。  
**Blast Radius**：模型 API key 泄漏后可造成账户滥用、成本损失、第三方数据访问风险。

**Issue**：项目自有 `learning-os.ai-model` 只含 provider/chatModel/embeddingModel，没有 key 字段，这是好的。但真实 Spring AI starter 通常会引入 `spring.ai.*.api-key/base-url` 等配置。若后续把 key 写入项目配置、健康 metadata、错误消息、metrics tag 或文档记忆，会突破现有防线。

**Remediation 示例（YAML + Java）**：

```yaml
# BAD: 禁止提交真实 key 或默认 key
spring:
  ai:
    openai:
      api-key: <runtime-secret>

# GOOD: 只允许环境变量或 Secret Manager 注入，默认空
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:}
      base-url: ${OPENAI_BASE_URL:}
```

```java
// BAD: health 暴露 provider 配置值
metadata.put("baseUrl", openAiProperties.getBaseUrl());

// GOOD: health 只暴露布尔状态和固定错误码
metadata.put("providerConfigured", hasText(provider));
metadata.put("chatModelConfigured", hasText(chatModel));
```

**安全约束**：
- 不新增 `learning-os.ai-model.api-key` 这类自有 key 字段，除非完成 `docs/security/` dependency/security review。
- 禁止把 `spring.ai.*.api-key`、base URL、deployment URL、organization、tenant、region 写入 health、DB、metrics tag、日志、trace、memory。
- 生产环境启动时 provider 非 `none` 但 key 缺失，应 fail closed 或健康 `UNCONFIGURED`，不能 fallback 到 mock 并返回成功业务结果。

### 4. 未授权模型调用成本放大风险依赖现有认证/权限入口完整性

**Severity**：MEDIUM  
**Category**：OWASP A01 Broken Access Control / A07 Identification and Authentication Failures  
**Location**：`DevAuthFilter.java:62`、`DevAuthFilter.java:72`、`ResourceGenerationController.java:26`、`AnalyticsController.java:37`  
**Exploitability**：生产环境需要无效/缺失 token；dev/test 环境若暴露公网则 `X-User-Id` fallback 可被滥用。  
**Blast Radius**：攻击者可触发真实模型外呼，造成成本损失，或通过高频失败制造告警噪声。

**Issue**：生产/预发 auth fallback 已收口，但真实模型接入后“每个会触发模型外呼的入口”都变成成本敏感入口，需要确认所有入口都先完成后端权限检查和预算检查，再调用 gateway。另有一个低关联但真实存在的旧入口：`/api/analytics/overview` 仍用 `"admin".equals(currentUserService.currentUserId())`，没有使用 roles-first `isAdmin()`；这不直接触发模型外呼，但与 P3-4 auth 一致性不符。

**Remediation 示例（Java）**：

```java
// BAD: 旧 userId 字符串判断，不兼容真实 JWT roles
if (!"admin".equals(currentUserService.currentUserId())) {
    throw new ApiException(ErrorCode.FORBIDDEN, "Admin required");
}

// GOOD: 使用 roles-first RBAC helper
if (!currentUserService.isAdmin()) {
    throw new ApiException(ErrorCode.FORBIDDEN, "Admin required");
}
```

**安全约束**：
- 模型外呼前必须完成：认证、对象级授权、课程/班级/enrollment scope、预算/速率决策。
- `dev/test` fallback 不得在公网或共享环境启用。
- 后续正式 Spring Security/OAuth2/JWK 接入前，所有新增模型入口必须覆盖 invalid token、missing token、spoofed `X-User-Id` 测试。

## 4. 低风险问题与注意事项

### 5. Metrics tag 只截断不做模型/provider 白名单

**Severity**：LOW  
**Category**：OWASP A09  
**Location**：`backend/src/main/java/com/learningos/common/observability/LearningOsMetrics.java:201`  
**Issue**：`tag()` 只移除控制字符并截断。当前 provider 在 gateway/recorder 已归一化，但 model 来自配置或 provider response。真实 provider 接入后，若 model 字段携带 deployment URL、tenant、region 或动态后缀，会造成高基数和潜在信息泄漏。  
**建议**：在 gateway response 构造时对 model 做白名单/模式归一化，不把 `https://`、`apiKey`、`sk-`、tenant/account 片段写入 metrics。

### 6. Ops abnormal model call 可能透传 `errorMessage`

**Severity**：LOW（当前 `AgentRunRecorder` 已脱敏）  
**Category**：OWASP A09  
**Location**：`AnalyticsService.java:1325`  
**Issue**：`AbnormalModelCall` 包含 `modelCall.getErrorMessage()`。当前写入端只保存固定码，因此安全；真实 provider 接入后如果有旁路写入 raw error，此处会放大泄漏。  
**建议**：保留写入端固定码约束，并增加测试断言 ops/token governance 响应不含 raw provider error、prompt、secret。

### 7. 本地 compose 默认密码仅适合开发

**Severity**：LOW  
**Category**：OWASP A05  
**Location**：`backend/docker-compose.yml` secrets scan 命中 `learning_os`、`learning_os_root`、`minioadmin` 默认值。  
**Issue**：属于本地开发默认值，不是模型 provider key；生产不可复用。  
**建议**：继续要求生产通过环境变量/Secret Manager 覆盖，并避免写入项目 memory/evidence。

## 5. OWASP 对照检查

- A01 Broken Access Control：生产 Bearer token gate 已存在；真实模型外呼前仍需逐入口确认对象级授权和预算 gate。发现 `/api/analytics/overview` 仍使用 userId 字符串 admin 判断，建议后续 P3-4 修正。
- A02 Cryptographic Failures：JWT HS256 使用 env secret；模型 key 当前未入库/未入配置。真实 Spring AI key 必须只走 env/secret manager。
- A03 Injection：本次未发现 SQL/命令注入新增面；真实 structured output 必须作为不可信输入校验后再写业务表，不能拼接 SQL 或 HTML。
- A04 Insecure Design：核心风险是真实 adapter 绕过 `AiModelGateway` 和结构化校验。
- A05 Security Misconfiguration：真实 provider 配置、health、actuator/metrics 暴露需要保持红线；当前 `/actuator/metrics` 已暴露，tag 必须低基数非敏感。
- A06 Vulnerable and Outdated Components：POM 版本可读，完整 CVE 审计失败，需在网络/DNS 可用环境重跑 dependency-check 或企业 SCA。
- A07 Identification and Authentication Failures：`DevAuthFilter` 对 prod/staging 无 token fail closed；dev/test fallback 不可暴露公网。
- A08 Software and Data Integrity Failures：真实 provider response 不可信；必须 schema validate、固定错误码、禁止 raw malformed output 持久化。
- A09 Security Logging and Monitoring Failures：现有日志白名单较好；真实 usage/cost 和 provider error 脱敏是重点。
- A10 SSRF：若接入自定义 provider base URL，必须禁止用户可控 URL；base URL 只能来自受控配置，不能由请求参数、prompt 或课程数据决定。

## 6. 测试建议

1. `AiModelGatewayRealProviderAdapterTest`
   - mock `ChatModel/ChatClient` 返回合法 JSON：断言 provider/model/token/cost 来自 gateway response，成功后写 `model_call_log` 和 `token_usage_log`。
   - mock 返回缺字段/非法 `safetyStatus`：断言失败码为 `STRUCTURED_OUTPUT_INVALID`，DB/异常/日志不含 raw output。
   - mock 抛出包含 `sk-...`、prompt、student answer、RAG chunk 的异常：断言只记录 `MODEL_PROVIDER_ERROR`。

2. `EmbeddingServiceRealProviderAdapterTest`
   - provider disabled：仍返回 disabled，不外呼。
   - provider enabled 且 key/model 缺失：fail closed 或 `UNCONFIGURED`，不写成功向量状态。
   - provider timeout/error：写固定 `EMBEDDING_PROVIDER_ERROR`，chunk metadata 不含 raw vector、raw provider response、secret、full chunk。

3. `HealthServiceRealProviderSecurityTest`
   - 设置 `spring.ai.*.api-key/base-url` 和 `AI_CHAT_MODEL`：`/api/health` 响应不含 key、base URL、deployment URL、模型名。
   - provider `none`：状态为 disabled，不误报 ready。
   - provider 非 `none` 但 chat/embedding model 缺失：状态为 `UNCONFIGURED`。

4. `ModelCostGovernanceTest`
   - provider usage 正常：token/cost 数值与 response 一致。
   - provider usage 缺失：使用保守估算或标记 usage missing，不允许静默 `0` 成本通过预算治理。
   - 高 token/high cost：`/api/analytics/token-budget/governance` 和 ops alert 能识别。

5. `AuthBeforeModelCallTest`
   - prod/staging missing token：不会进入 gateway。
   - invalid Bearer + spoofed `X-User-Id: admin`：不会进入 gateway。
   - teacher/student 越权 course/resource generation：不会进入 gateway。

6. `StructuredRequestLoggingFilterProviderLeakTest`
   - 请求 query/body/header 包含 provider key、prompt、question 时，完成日志仍只含白名单字段。
   - provider 异常时，日志不含 raw exception。

## 7. 最小安全约束清单

- [ ] 真实 Chat/Embedding provider 只能封装在 gateway/adapter 边界内。
- [ ] 禁止业务 Service、Controller、Agent Tool 直接调用 provider SDK。
- [ ] 模型密钥只来自环境变量或 Secret Manager；不进入 `application*.yml` 默认值、memory、evidence、health、metrics、DB。
- [ ] Provider/base URL/deployment URL 不允许由用户输入、prompt、课程内容或请求参数控制。
- [ ] 所有 provider error 统一映射固定错误码。
- [ ] 所有 structured output 作为不可信输入，先 schema validate，再写业务表。
- [ ] 模型外呼前必须完成认证、对象级授权、课程/enrollment scope 和预算/速率判断。
- [ ] Token/cost 必须来自 provider usage 或保守估算，不能静默 0 成本。
- [ ] Health/ops alerts/metrics 只输出低基数、非敏感、白名单字段。
- [ ] Embedding/vector metadata 不保存 raw vector、raw chunk、raw provider response、secret。
- [ ] Provider fallback 只能降级为 deterministic/cached safe response；不能在未授权、预算超限或结构化校验失败后继续生成业务成功结果。

## 8. 验证记录

- 读取：`docs/memory/PROJECT_MEMORY.md`
- 读取：`docs/planning/backend-architecture-todolist.md`
- 读取：`docs/skills/SKILL_REGISTRY.md`
- 读取：`AiModelGateway`、`AgentRunRecorder`、`ModelCallLog`、`TokenUsageLog`、`HealthService`、`DevAuthFilter`、`CurrentUserService`、`StructuredRequestLoggingFilter`、`AnalyticsService`、`EmbeddingService`
- secrets scan：`rg -n "(?i)(api[_-]?key|apikey|secret|password|token|authorization|bearer|sk-|AKIA...)" backend docs -g '!backend/target/**'`
- provider boundary scan：`rg -n "ChatClient|ChatModel|EmbeddingModel|spring\\.ai|OpenAi|DashScope|generateStructured|recordSuccessfulModelEvidence" backend docs`
- dependency command：`mvn -q -DskipTests dependency:tree`，执行成功
- dependency audit：`mvn -q org.owasp:dependency-check-maven:check -DskipTests -Dformat=JSON -DfailBuildOnCVSS=11`，失败，原因是 DNS 无法解析 `raw.githubusercontent.com`，RetireJS/NVD 数据未初始化
- git history scan：失败，当前目录不是 Git 仓库

## 9. 最终建议

P3-3 真实 provider 接入可以继续推进，但必须作为安全敏感切片处理：先写 SPEC/PLAN/TASK 中的 provider adapter 安全约束和测试，再实现 Chat provider；Embedding provider 建议单独切片，避免同时引入 Chat、Embedding、VectorDB、计费和健康检查多个高风险边界。当前最优先的阻断条件是：**不得合入任何绕过 `AiModelGateway` 的真实模型 SDK 调用；不得在 usage/cost 缺失时把真实调用记为 0 成本成功。**
