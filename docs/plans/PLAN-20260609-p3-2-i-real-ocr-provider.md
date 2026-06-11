# PLAN - P3-2-I Real OCR Provider

## 1. 追踪

- PRD：`docs/product/PRD-20260609-p3-2-i-real-ocr-provider.md`
- REQ：`docs/requirements/REQ-20260609-p3-2-i-real-ocr-provider.md`
- SPEC：`docs/specs/SPEC-20260609-p3-2-i-real-ocr-provider.md`
- TASK：`docs/tasks/TASK-20260609-p3-2-i-real-ocr-provider.md`
- Context：`docs/context/CONTEXT-20260609-p3-2-i-real-ocr-provider.md`
- Dependency Review：`docs/security/DEPENDENCY-REVIEW-20260609-p3-2-i-real-ocr-provider.md`
- Subagents：
  - `docs/subagents/runs/RUN-20260609-p3-2-i-real-ocr-provider-architecture.md`
  - `docs/subagents/runs/RUN-20260609-p3-2-i-real-ocr-provider-security.md`

## 2. Skill Selection Report

### Task Type

RAG parser / OCR provider productionization slice.

### Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目强制 spec-first 流程 |
| `educational-rag-pipeline` | OCR fallback 属于 RAG document indexing parser 层 |
| `rag-parser-boundary` | 约束 provider、safe error、raw fallback 禁止事项 |
| `security-review` | OCR 处理不可信文件和外部进程 |
| `dependency-review` | 明确不新增 Maven OCR 依赖，记录 runtime tool 风险 |
| `test-driven-development` | 新行为必须 RED/GREEN |
| `architecture-drift-check` | 防止边界扩散到 `IndexService` |
| `verification-before-completion` | 完成声明前 fresh verification |
| `Confidence Check` | 确认无重复、边界清晰、可实施 |

### Missing Skills

无。

### GitHub Research Needed

No。本切片不接 Java OCR SDK；外部命令方案基于 JDK `ProcessBuilder` 和现有 provider SPI。

### New Project-Specific Skill To Create

暂不创建；完成后扩展 `rag-parser-boundary`。

## 3. Multi-Expert Subagent Gate

| Item | Decision |
|---|---|
| Use Subagents | Yes |
| Reason | RAG + security/dependency + external process boundary |
| Parallelism Level | L1 analysis |
| Selected Subagents | Architecture expert, Security/dependency expert |
| Implementation Mode | Main Codex 单任务 TDD |

## 4. Confidence Check

| Check | Status | Notes |
|---|---|---|
| No duplicate implementation | PASS | 已有 SPI/configurable service，但无 process OCR provider |
| Architecture compliance | PASS | 只在 `rag/parser` 内新增 provider |
| Official documentation verified | N/A | 不新增外部 SDK/API；使用 JDK process pattern |
| Working OSS implementation referenced | N/A | 不复制外部项目代码 |
| Root cause identified | PASS | provider SPI 已存在，真实 provider 未实现 |

Confidence：0.93，可进入实现。

## 5. 实施阶段

| Phase | Description | Status |
|---|---|---|
| 1 | 创建 PRD/REQ/SPEC/PLAN/TASK/CONTEXT/Dependency/Subagent docs | Completed |
| 2 | RED：新增 process OCR provider tests | Completed |
| 3 | GREEN：扩展 OCR properties + process provider | Completed |
| 4 | GREEN：更新 application.yml/test.yml | Completed |
| 5 | Focused / adjacent / dependency / full verification | Completed |
| 6 | Evidence / Acceptance / Changelog / Memory / TODO / Retro / Skill | Completed |

## 6. 风险与缓解

| Risk | Mitigation |
|---|---|
| 命令注入 | 使用 `ProcessBuilder(List<String>)`，不经 shell |
| 资源耗尽 | timeout、max-output-chars、parser input/output limits |
| 敏感泄露 | stderr/raw exception/path/secret 不进入 result |
| 误启用 | 默认 disabled，command 缺失返回 unavailable |
| 范围漂移 | Context Pack 禁止改 `IndexService`/API/DB/retrieval/citation |

## 7. 依赖审批

本 PLAN 不批准新增 Maven dependency。外部 OCR command 是 runtime dependency，默认关闭，需由部署环境显式配置。

## 8. 验证结果

- TDD RED 已观察：新增测试在类缺失阶段失败，证明覆盖面有效。
- Focused verification：`21 run, 0 failures, 0 errors, 0 skipped`。
- Adjacent verification：`33 run, 0 failures, 0 errors, 0 skipped`。
- Dependency tree：未命中 Tess4J / JNA / Bytedeco / 云 OCR SDK。
- Compile：`mvn -DskipTests compile` 成功。
- Full backend：`392 run, 0 failures, 0 errors, 1 skipped`。
- 关键实现修复：`RagParserOcrProperties` 使用显式 constructor binding，`ProcessOcrFallbackProvider` 的 Spring 注入构造器显式标注 `@Autowired`，避免 Spring Boot 3.5 上下文绑定歧义。
