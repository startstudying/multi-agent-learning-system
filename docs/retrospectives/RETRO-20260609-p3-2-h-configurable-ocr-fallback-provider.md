# Retrospective - P3-2-H Configurable OCR Fallback Provider

## 1. Feature Summary

完成 OCR fallback 的可配置 provider boundary。新增 `RagParserOcrProperties`、`OcrFallbackProvider` SPI 与 `ConfigurableOcrFallbackService`，使 OCR 默认关闭，启用但 provider 缺失时稳定返回 unavailable，provider 异常安全失败，fake provider 成功路径可被 PDFBox image-only fallback 使用。

## 2. What Went Well

- P3-2-F/P3-2-G 的 parser boundary 让本切片可以只改 `rag/parser` 与配置，不碰 `IndexService`。
- Subagent 架构与安全结论一致：本切片不引入真实 OCR 依赖，只补配置边界。
- RED 明确暴露缺少目标类；GREEN 后 focused、adjacent、full backend verification 均 fresh 通过。
- `NoopOcrFallbackService` 保留直接实例化兼容，同时避免未来真实 provider 与 noop 都作为 `OcrFallbackService` bean 造成歧义。

## 3. What Didn't Go Well

- 初次等待架构专家超时，需要 interrupt 后才返回；后续对 subagent 产物应设置更窄的问题和更短输出格式。
- 测试初稿用了不适合双方法 SPI 的 lambda，写 RED 前修正了一次测试形态。
- 根目录仍不是 git repository，无法用 git diff/status 做范围证据。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| OCR provider 应通过 configurable selector 暴露为单一 `OcrFallbackService`，真实 provider 实现独立 SPI | Yes | `docs/skills/project-specific/rag-parser-boundary.md` |
| OCR/native/cloud 依赖不得混入配置边界切片，必须独立 dependency/security/runtime/privacy review | Yes | `docs/skills/project-specific/rag-parser-boundary.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | P3-2 OCR 仍包含“配置边界”和“真实能力”两个层次 | 继续拆小切片：config boundary、real provider、layout metadata 分开验收 |
| Testing | 已有 fake provider success/failure | 后续真实 OCR provider 增加 timeout、page limit、resource isolation、privacy leak tests |
| Documentation | Dependency review 明确无新增依赖 | 后续真实 OCR 必须新增 runtime/privacy review，不只记录 Maven dependency |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 设计真实 OCR provider 的依赖、隐私、超时、资源隔离策略 | Main Codex | Future P3-2 slice |
| 设计 OCR result 页码/置信度/质量状态 metadata | Main Codex | Future P3-2 slice |
| 继续 real VectorDB client 切片 | Main Codex | Future P3-2 slice |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] AGENT_RAG_MEMORY.md
- [x] CHANGELOG.md
- [x] backend-architecture-todolist.md
- [x] rag-parser-boundary skill
- [ ] ARCHITECTURE_BASELINE.md

未更新 `ARCHITECTURE_BASELINE.md`：本切片没有改变系统层级基线，只是在既有 `rag/parser` boundary 内新增配置与 provider selector。

