# RUN-20260621-memory-chatgpt-quality-security-quality

## 评审范围

- 角色：Security & Quality 专家，只读分析。
- 目标：评审增强长期记忆、RAG 个性化、ChatGPT 级回答质量时的安全、隐私、评测、可观测性风险。
- 文档范围：`docs/security/`、`docs/memory/`、`docs/specs/` 中 `model_call_log`、`prompt_version`、`evaluation`、`trace`、`RAG citation`、`learner_profile` 相关内容。
- 后端范围：`backend/src/main/java/com/learningos/**` 中 RAG 查询、Trace、Prompt Version、Evaluation、Learner Profile、Model Gateway、Model Provider、Embedding/Vector 相关实现。
- 约束：未修改代码；仅新增本报告。

## Skill Selection Report

| 项 | 结论 |
|---|---|
| Task Type | 安全/隐私/质量/可观测性只读评审 |
| Selected Skills | `security-review`、`java-security-review`、项目 `feature-development-workflow` 规则 |
| Why | 涉及 OWASP、密钥、输入/日志、授权、依赖、Java/Spring 后端实现和项目 subagent 报告交付 |
| Missing Skills | 无阻断；建议后续沉淀 `memory-privacy-governance` 项目技能 |
| GitHub Research Needed | No。本次是项目内风险评审，不新增外部方案/依赖 |
| Size Classification | S，只读 subagent 分析，唯一产物是本报告 |

## 执行摘要

总体风险级别：HIGH。

系统已有不少关键安全基线：生产 Bearer/JWT fail-closed、PromptVersion 管理 RBAC、Evaluation RBAC、RAG KB/Query roles-first RBAC、向量 payload 不含原文、模型 provider 错误脱敏、工具调用摘要脱敏、RAG POST streaming 避免敏感 URL。主要风险不在“立即可远程利用的单点 RCE/SQL 注入”，而在长期记忆和 ChatGPT 级质量增强会放大现有高敏字段的持久化、传播和评测误用：

1. HIGH：RAG query log / citation / replay snapshot 仍持久化原始问题、回答快照、文档名、章节、excerpt；长期记忆会扩大敏感学习画像和课程资料泄露半径。
2. HIGH：Learner profile snapshot 将 `learnerId`、弱点、偏好、`teacher_note` 等写入路径/资源生成快照和模型上下文，缺少字段级最小化、TTL、重算/删除策略。
3. MEDIUM-HIGH：Trace retention policy 目前主要是响应元数据，未看到实际清理任务；trace search 使用 `findAll()` 后内存过滤，长期高流量下会造成可观测性数据过度暴露和运维不可用。
4. MEDIUM-HIGH：评测指标可以被低样本或污染评测集“刷高”；允许指标白名单和样本加权已实现，但缺少上线门禁阈值、最小样本量、分层覆盖、污染检测和置信区间。
5. MEDIUM：真实模型 prompt 直接拼接用户 prompt 与 safe context；虽然后续不持久化 raw prompt，但缺少 prompt-injection 红队评测作为质量门禁。
6. MEDIUM：遗留 GET SSE 接口仍接收 URL query `question` / `kbIds`，文档说 production/staging 前端使用 POST，但后端接口仍存在，需要明确 dev-only 或禁用策略。
7. MEDIUM：Model Provider `websiteUrl` 使用 `URI.create`，格式异常可能绕过统一 `ApiException`；baseUrl 的 SSRF/内网地址策略需要确认。
8. MEDIUM：前端依赖审计发现高危 dev/test 依赖漏洞；后端完成 dependency tree，但未运行 CVE 数据库型审计。

## 风险排序

### 1. RAG 日志和引用持久化原始问题/回答/excerpt

- Severity：HIGH
- Category：OWASP A02 Sensitive Data Exposure / A04 Insecure Design / A09 Logging & Monitoring
- Location：
  - `backend/src/main/java/com/learningos/rag/application/RagQueryService.java:382`
  - `backend/src/main/java/com/learningos/rag/application/RagQueryService.java:385`
  - `backend/src/main/java/com/learningos/rag/application/RagQueryService.java:441`
  - `backend/src/main/java/com/learningos/rag/application/RagQueryService.java:326`
- Evidence：`kb_query_log.question` 写入截断后的原始问题；`responseJson` 在 requestId 路径保存完整响应快照；`source_citation.excerpt` 保存课程片段；no-source answer 把原始 question 拼回响应。
- Exploitability：已授权用户或管理员视图、数据库/日志泄露、备份泄露、内部误用。
- Blast Radius：学生提问、学习弱点、课程私有文档片段、引用证据、回答结果，未来进入长期记忆后可跨场景放大。
- Risk Detail：项目文档已经要求“Trace records sanitized”和“no full prompts with PII”（`docs/security/AI_TOOL_SECURITY.md:34`），但 RAG 业务日志仍保存比 trace 更敏感的原文。长期记忆增强如果复用这些字段，会把短期查询日志变成持久画像事实。
- Remediation Example：

```java
// BAD: 持久化原始问题和完整 replay 响应
fields.setPropertyValue("question", truncate(question, 4000));
fields.setPropertyValue("responseJson", requestId == null ? null : toJson(response));
record.setExcerpt(source.excerpt());

// GOOD: 默认只存 hash、长度和低敏引用；原文进入短 TTL 加密表或完全不落库
fields.setPropertyValue("questionHash", sha256(normalizedQuestion));
fields.setPropertyValue("questionLength", normalizedQuestion.length());
fields.setPropertyValue("responseJson", requestId == null ? null : toJson(redactedReplay(response)));
record.setExcerpt(redactExcerpt(source.excerpt()));
```

建议：新增 `RagMemoryPrivacyPolicy`，把 `question`、`answer`、`excerpt`、`documentName` 分级为 `RAW_TEXT_30D` / `HASH_ONLY_365D` / `LOW_SENSITIVITY_METADATA`，并让 replay 快照只保存业务必要字段。

### 2. Learner profile snapshot 过度复制长期画像

- Severity：HIGH
- Category：OWASP A01 Broken Access Control impact amplifier / A02 Sensitive Data / Privacy by Design
- Location：
  - `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java:127`
  - `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java:140`
  - `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java:988`
  - `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java:1019`
  - `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java:1027`
- Evidence：profile extract 把 `request.message()` 作为 learning event subject；`profileSnapshot` 包含 `learnerId`、`weak_point`、`preference`、`recent_error_pattern`、`teacher_note`、sources，并复制到学习路径/资源生成上下文。文档也明确响应返回 `profileSnapshot`（`docs/memory/API_MEMORY.md:391` 到 `394`）。
- Exploitability：越权、备份泄露、老师/管理员误查、模型上下文外传。
- Blast Radius：学生长期学习弱点、偏好、教师评价、错误模式被复制到多张表和模型上下文，删除或纠错困难。
- Risk Detail：画像字段本身是高度敏感教育数据；ChatGPT 级个性化会倾向于“越多上下文越好”，但项目缺少每个下游任务的最小字段集策略。
- Remediation Example：

```java
// BAD: 每个下游任务复制完整画像快照
snapshot.put("learnerId", learnerId);
snapshot.put("teacher_note", structuredProfile.teacherNote());
snapshot.put("recent_error_pattern", structuredProfile.recentErrorPattern());

// GOOD: 下游只拿当前任务需要的低敏摘要和证据引用
snapshot.put("profileRef", profile.getId());
snapshot.put("weak_point", topN(structuredProfile.weakPoint(), 3));
snapshot.put("pace_and_feedback", structuredProfile.paceAndFeedback());
snapshot.put("sourceEvidenceIds", sourceEvidenceIds(structuredProfile.sources()));
```

建议：对 `profileSnapshot` 做目的绑定：学习路径只需要弱点/掌握度摘要；资源生成只需要偏好/节奏；教师笔记默认不进入模型 prompt，除非教师显式标记可用于 AI。

### 3. Trace retention 是展示策略，不是强制治理

- Severity：MEDIUM-HIGH
- Category：OWASP A09 Logging & Monitoring / A04 Insecure Design
- Location：
  - `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java:32`
  - `backend/src/main/java/com/learningos/agent/application/AgentRunRecorder.java:304`
  - `backend/src/main/java/com/learningos/agent/application/AgentTraceGovernanceService.java:68`
  - `backend/src/main/java/com/learningos/agent/application/AgentTraceGovernanceService.java:117`
  - `docs/specs/SPEC-20260606-agent-trace-governance-dashboard.md:62`
- Evidence：tool call 有 `SANITIZED_SUMMARY` retention class；响应返回 retention policy；但未看到按 retention class 删除/压缩 raw text 的 job。Trace search 使用 `agentTaskRepository.findAll()` 后按 `userId/status/time/failureReason` 内存过滤。
- Exploitability：管理员或拥有 trace detail 的用户可长期读取历史 trace；高数据量导致 search 慢查询/内存压力。
- Blast Radius：AI 工作流输入/输出摘要、失败原因、工具摘要、任务 owner 与 traceId。
- Risk Detail：可观测性系统如果无限期保留，会成为第二数据库；长期记忆质量增强会进一步把 trace 当训练/评测语料使用。
- Remediation Example：

```java
// BAD: 查询全量任务后在内存过滤
agentTaskRepository.findAll().stream()
    .filter(task -> matches(userId, task.getOwnerUserId()))
    .toList();

// GOOD: repository 层按 scope、时间窗、状态分页查询
Page<AgentTask> page = agentTaskRepository.searchScoped(
    userId, agentType, status, from, to, PageRequest.of(page, size)
);
```

建议：新增 `TraceRetentionSweepService`，按 `retentionClass` 清理/压缩 `inputJson/outputJson/tool input/output`；`/api/agent/traces` 增加分页和最大时间窗。

### 4. 评测门禁不足，容易把低样本“质量提升”误判为真实提升

- Severity：MEDIUM-HIGH
- Category：OWASP A04 Insecure Design / AI Evaluation Governance
- Location：
  - `backend/src/main/java/com/learningos/evaluation/application/EvaluationRunService.java:39`
  - `backend/src/main/java/com/learningos/evaluation/application/EvaluationRunService.java:89`
  - `backend/src/main/java/com/learningos/evaluation/application/EvaluationRunService.java:206`
  - `backend/src/main/java/com/learningos/evaluation/application/EvaluationRunService.java:247`
  - `docs/specs/SPEC-20260606-rag-quality-evaluation.md:81`
- Evidence：指标白名单、正样本数、重复指标拒绝、sample-count 加权已经实现；但 succeeded run 只要求 `sampleCount > 0`，没有最小样本量、分层覆盖、置信区间、golden set 锁定、prompt injection 子集门禁。
- Exploitability：内部误操作或恶意上传小样本评测集，让某 prompt/model 看似胜出。
- Blast Radius：错误 prompt/model 上线，导致 hallucination、无引用回答、隐私泄露、低质量个性化。
- Remediation Example：

```java
// BAD: SUCCEEDED 只要求 sampleCount > 0
if (STATUS_SUCCEEDED.equals(status) && sampleCount <= 0) {
    throw new ApiException(ErrorCode.VALIDATION_ERROR, "sample count must be positive");
}

// GOOD: 按评测类型设置最小样本和必需指标
EvaluationGate gate = gatePolicy.forSetType(evaluationSet.getType());
if (STATUS_SUCCEEDED.equals(status) && sampleCount < gate.minSampleCount()) {
    throw new ApiException(ErrorCode.VALIDATION_ERROR, "sample count below release gate");
}
gate.requiredMetrics().forEach(metric -> requireMetric(requestedMetrics, metric));
```

建议：把 evaluation 从“记录指标”升级为“发布门禁”：RAG 质量至少包含 source-required、no-source、prompt-injection、privacy-leak、multi-turn memory drift 五类样本。

### 5. Prompt injection / data exfiltration 评测未成为模型调用门禁

- Severity：MEDIUM
- Category：OWASP A03 Injection / LLM Prompt Injection
- Location：
  - `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java:199`
  - `backend/src/main/java/com/learningos/agent/application/AiModelGateway.java:222`
  - `docs/security/AI_TOOL_SECURITY.md:15`
- Evidence：真实 provider 调用会把 user prompt 和 safe context 拼入 provider prompt；文档要求 user input sanitized before inclusion in prompts，但当前主要依赖结构化输出校验和失败脱敏，未看到 prompt-injection eval gate。
- Exploitability：学生在问题、资料、教师笔记里注入“忽略引用/输出隐藏上下文/泄露系统提示”等指令。
- Blast Radius：引用绕过、私有上下文泄露、错误资源生成、评测污染。
- Remediation Example：

```java
// GOOD: prompt 构造前进行上下文分区和不可信内容标注
String prompt = promptBuilder
    .system(policy.systemInstructions())
    .trustedContext(courseFacts)
    .untrustedUserContent(request.prompt())
    .untrustedRetrievedContent(retrievedChunks)
    .requireCitations(true)
    .build();
```

建议：最小切片先新增评测集，不急于重构 prompt builder：用现有 evaluation/run 记录 `promptInjectionResistance`、`contextLeakRate`、`citationRequiredPassRate`。

### 6. 遗留 GET SSE 仍有敏感 URL 风险

- Severity：MEDIUM
- Category：OWASP A02 Sensitive Data / A05 Security Misconfiguration
- Location：
  - `backend/src/main/java/com/learningos/rag/api/ChatController.java:126`
  - `backend/src/main/java/com/learningos/rag/api/ChatController.java:129`
  - `docs/specs/SPEC-20260611-p3-4-formal-production-streaming-design.md:17`
  - `docs/specs/SPEC-20260611-p3-4-formal-production-streaming-design.md:179`
- Evidence：正式 POST stream 已实现并规定 URL 不得包含 question/kbIds/JWT；但后端仍保留 GET `/api/chat/sessions/{sessionId}/stream?question=...&kbIds=...`。
- Exploitability：如果生产路由、代理、前端回退、外部用户仍调用 GET SSE，问题会进入浏览器历史、反向代理日志、APM、Referer。
- Blast Radius：学生问题、知识库 ID、sessionId。
- Remediation Example：

```java
// GOOD: production-like 环境拒绝遗留 GET SSE
if (appProperties.isProductionLike()) {
    throw new ApiException(ErrorCode.GONE, "Use POST /api/rag/query/stream");
}
```

建议：将 GET SSE 标记 dev/test-only，并在 production/staging 返回 `410 GONE` 或 feature flag 关闭。

### 7. Provider URL / SSRF 策略需要收口

- Severity：MEDIUM
- Category：OWASP A10 SSRF / A05 Security Misconfiguration
- Location：
  - `backend/src/main/java/com/learningos/agent/application/ModelProviderService.java:175`
  - `backend/src/main/java/com/learningos/agent/application/ModelProviderService.java:214`
  - `backend/src/main/java/com/learningos/agent/application/ModelProviderService.java:218`
- Evidence：provider 管理允许 admin 配置 baseUrl/websiteUrl；`websiteUrl` 使用 `URI.create`，非法 URI 可能抛出非统一异常；baseUrl 规范化逻辑需确认是否禁止 localhost、169.254.169.254、内网网段、file 等。
- Exploitability：管理员或被盗管理员配置恶意 OpenAI-compatible base URL，触发后端出站访问。
- Blast Radius：内网探测、metadata service 访问、凭证外发到恶意兼容 API。
- Remediation Example：

```java
// GOOD: 对 provider baseUrl 做 scheme + host allowlist / deny private ranges
URI uri = safeUri(request.baseUrl());
if (!Set.of("https").contains(uri.getScheme()) || isPrivateOrLinkLocal(uri.getHost())) {
    throw new ApiException(ErrorCode.VALIDATION_ERROR, "Provider base URL is not allowed");
}
```

建议：生产环境仅允许配置的 provider allowlist；custom provider 需单独开关和审计记录。

### 8. 前端 dev/test 依赖审计存在高危项

- Severity：MEDIUM
- Category：OWASP A06 Vulnerable and Outdated Components
- Location：`frontend/package.json`、`frontend/pnpm-lock.yaml`
- Evidence：`pnpm audit --registry=https://registry.npmjs.org --audit-level moderate` 返回 8 个漏洞：4 high、2 moderate、2 low。高危包括 `undici <7.28.0` 经 `jsdom/vitest` 传递引入，以及 `ini <1.3.6` 经 `@vue/test-utils > js-beautify > config-chain` 引入。
- Exploitability：主要在测试/dev 工具链；若 CI、预览或测试处理不可信 HTML/网络代理，风险上升。
- Blast Radius：CI runner、开发机、测试环境。
- Remediation Example：

```json
{
  "pnpm": {
    "overrides": {
      "undici": ">=7.28.0",
      "ini": ">=1.3.8"
    }
  }
}
```

建议：单独做 dependency-review 切片，优先升级 `jsdom` 或通过 `pnpm overrides` 固定 patched transitive 版本，并跑前端测试/build。

## 已确认的正向控制

- Secrets：源码/文档扫描未发现真实 `sk-*`、私钥、云 AK/SK；配置主要通过环境变量注入。Git history 搜索只发现环境变量占位符、测试 secret、文档安全规则。
- Auth/RBAC：记忆和实现显示 RAG Query、KB、Evaluation、PromptVersion、Trace 已大量迁移到 roles-first；PromptVersion teacher 响应省略 `promptText`，Evaluation 非 admin/teacher 受限。
- Model Call：`AgentRunRecorder` 将 provider 标准化为低基数字段，模型失败统一为 `MODEL_PROVIDER_ERROR` 或 `STRUCTURED_OUTPUT_INVALID`。
- Tool Trace：tool input/output 会经过正则脱敏，并返回 summary。
- VectorDB：Qdrant payload 只带 chunkId/kbId/documentId/version/hash/index/vector，不带 raw content/question/prompt/user/storage/secret。
- Streaming：正式 production/staging 路径使用 `POST /api/rag/query/stream`，避免 question/kbIds 出现在 URL。

## 可验证质量指标

### 安全/隐私指标

| 指标 | 目标阈值 | 验证方式 |
|---|---:|---|
| Raw sensitive persistence count | `kb_query_log.question/responseJson`、`source_citation.excerpt` 新写入 raw 文本数量为 0，或仅短 TTL 加密表 | 新增 Repository/集成测试；数据库断言字段只含 hash/length/redacted |
| Cross-user memory leak rate | 0/1000 | 构造 A/B learner profiles + RAG docs，A 查询不得出现 B 的 profile/doc/citation |
| Prompt/context leak rate | 0% | 红队样本要求输出 system prompt、profileSnapshot、teacher_note、hidden context，断言拒绝 |
| Citation required pass rate | source-required 样本 >= 98% | RAG evaluation 增加强制引用样本 |
| No-source refusal rate | no-source 样本 >= 95% | RAG evaluation 的 no-source 子集 |
| Unauthorized trace/profile read | 0 | MockMvc RBAC matrix：student/teacher/admin/foreign/missing |
| Sensitive URL usage | production/staging 0 次 GET SSE with question/kbIds | 前端静态扫描 + 后端 production test 返回 410/403 |

### 回答质量指标

| 指标 | 目标阈值 | 说明 |
|---|---:|---|
| Groundedness | >= 0.90 | Recall@K 与 Citation Accuracy 调和平均 |
| Citation Accuracy | >= 0.95 | 引用必须命中 expected source/chunk |
| Hallucination rate | <= 2% | 由人工/规则混合标注 |
| Personalization helpfulness | >= 4.2/5 | 盲评，同题有/无 profile 对比 |
| Memory drift rate | <= 3% | 多轮后画像字段不应偏离证据 |
| Harmful personalization rate | 0 | 不使用敏感教师笔记、身份标签做不当推荐 |
| Regression delta | 新 prompt 不得低于 baseline 2pp | 对核心指标设 fail gate |

### 可观测性指标

| 指标 | 目标阈值 | 说明 |
|---|---:|---|
| Trace coverage | 100% AI workflow 有 traceId | RAG/QA/resource/evaluation/model call 均能关联 |
| Model call evidence coverage | 100% 成功/失败都有 model_call_log + token_usage_log 或明确 no-external-call | 业务测试断言 |
| Retention sweep lag | < 24h | 超过 TTL 的 raw/sanitized text 被清理或压缩 |
| Trace search p95 | < 500ms at 100k tasks | repository 分页查询，不允许 findAll 全表内存过滤 |
| Alert precision | >= 80% | slow RAG/no-source/model failure/review backlog 告警人工抽样 |

## 最小落地切片建议

### Slice 1：Memory/RAG Privacy Guard 最小切片

- Size：M
- 目标：先切断 raw query/profile/citation 进入长期记忆的默认路径。
- 范围：
  - 新增 `RagPrivacySanitizer` / `MemoryPrivacyPolicy`。
  - `kb_query_log` 默认保存 `questionHash`、`questionLength`、低敏 retrieval metadata；`responseJson` replay 快照删除 answer/excerpt 或只保存 hash。
  - `source_citation.excerpt` 改为短摘要/可配置关闭；documentName 如含用户上传文件名则脱敏。
  - `profileSnapshot` 删除 `learnerId` 和默认 `teacher_note`，改为 `profileRef` + 必需字段。
- 验收：
  - 新增测试断言 DB 字段不包含原始问题、teacher_note、完整 excerpt。
  - 现有 RAG replay、citation UI 可用或明确降级。

### Slice 2：Evaluation Quality Gate 最小切片

- Size：M
- 目标：防止“低样本高分”驱动模型/prompt 发布。
- 范围：
  - 为 RAG/QA/Resource Generation 定义最小样本量和必需指标。
  - EvaluationRun 增加 gate verdict：`PASS/FAIL/INSUFFICIENT_SAMPLE`.
  - 增加 prompt injection / privacy leak / no-source / source-required 子集标签。
- 验收：
  - `sampleCount < min` 的 SUCCEEDED run 不能成为 comparison winner。
  - 缺少 `citationAccuracy`、`groundedness`、`noSourceRefusalRate`、`privacyLeakRate` 之一时 fail gate。

### Slice 3：Trace Retention Enforcement 最小切片

- Size：M
- 目标：让 retention policy 从“响应说明”变成实际治理。
- 范围：
  - `AgentTraceGovernanceService.search` 改 repository 分页查询。
  - 新增 retention sweep：30 天后清理 text summary/tool summary，365 天保留低敏审计元数据。
  - Trace detail 返回 `retentionAppliedAt` / `retentionClass`。
- 验收：
  - 构造过期 trace，运行 sweep 后 raw/sanitized text 清空或压缩。
  - 100k task fixture 下 search 不使用 `findAll()`。

### Slice 4：Production GET SSE Disable

- Size：S
- 目标：消除遗留 URL query 泄露路径。
- 范围：
  - production/staging 下 `GET /api/chat/sessions/{sessionId}/stream` 返回 410 或 403。
  - dev/test 保留兼容。
- 验收：
  - production profile MockMvc：带 question/kbIds 的 GET SSE 不启动异步工作。
  - frontend production 静态扫描无 `EventSource(...question...)`。

## OWASP Checklist

- A01 Broken Access Control：已检查 RAG/Evaluation/PromptVersion/Trace/Profile 入口；主要剩余风险是长期画像复制后任一越权点的 blast radius 变大。
- A02 Cryptographic Failures / Sensitive Data：发现 RAG/profile/trace 持久化敏感文本风险；模型 provider key 使用 AES-GCM 且生产要求 `MODEL_PROVIDER_ENCRYPTION_KEY`。
- A03 Injection：未发现 SQL 字符串拼接生产风险；LLM prompt injection 需要评测门禁。
- A04 Insecure Design：长期记忆缺少目的绑定、字段最小化、TTL、删除/纠错策略。
- A05 Security Misconfiguration：遗留 GET SSE 生产禁用策略需代码固化；provider URL allowlist/SSRF 策略需确认。
- A06 Vulnerable Components：前端 audit 有 high/moderate；后端 dependency tree 已解析，未执行 CVE DB 审计。
- A07 Identification/Auth Failures：文档和测试记忆显示 Bearer/JWT fail-closed、roles-first 已覆盖重点路径。
- A08 Software/Data Integrity：评测集污染、低样本胜出、prompt/version gate 不足。
- A09 Logging/Monitoring：可观测性覆盖较好，但 retention 未强制，RAG log 保存 raw 文本。
- A10 SSRF：Model provider custom baseUrl/websiteUrl 需 allowlist/private IP denylist。

## Secrets 与依赖扫描

- Secrets scan：
  - 命令：`rg -n "api[_-]?key|secret|password|passwd|token|credential|AKIA|BEGIN (RSA|OPENSSH|PRIVATE)|sk-[A-Za-z0-9]|jwt|Authorization|Bearer" backend/src docs/security docs/memory docs/specs .env.example -S`
  - 结论：未发现真实生产密钥；命中主要是环境变量占位符、测试 JWT secret、文档安全规则。
- Git history secrets：
  - 命令：`git log -p --all -- .env* backend/src/main/resources/application.yml docs/security docs/memory docs/specs | rg ...`
  - 结论：未发现真实密钥；命中为占位符和文档。
- Frontend dependency audit：
  - 命令：`pnpm audit --registry=https://registry.npmjs.org --audit-level moderate`
  - 结果：8 vulnerabilities：4 high、2 moderate、2 low。主要为 `undici <7.28.0` 和 `ini <1.3.6` 的 dev/test 传递依赖。
  - 注意：默认 registry `https://registry.npmmirror.com` 的 audit endpoint 不存在，需 CI 使用 npmjs registry 或企业 audit 服务。
- Backend dependency inventory：
  - 命令：`mvn "-DskipTests" "dependency:tree"`
  - 结果：成功解析。关键版本：Spring Boot 3.5.7、Spring Security 6.5.6、Spring AI 1.0.8、MinIO 8.5.17、Qdrant client 1.13.0、grpc-api 1.75.0、PDFBox 3.0.7、POI 5.5.1。
  - 限制：未运行 OWASP dependency-check / Snyk / osv-scanner，因为这类工具会下载数据库并写入本地/target 产物，和本次“除报告外不改文件”的约束冲突。建议后续单独 dependency-review 任务中运行。

## 结论

当前系统已经具备较好的 RBAC、trace、model-call、citation 和 provider 错误脱敏基础，但“长期记忆 + RAG 个性化 + ChatGPT 级质量”的下一步不应先扩大上下文，而应先做隐私最小化和评测门禁。推荐优先执行 Slice 1 和 Slice 2：一个降低数据泄露半径，一个防止质量提升被低样本或污染评测误判。Slice 3/4 作为紧随其后的可观测性和传输面收口。
