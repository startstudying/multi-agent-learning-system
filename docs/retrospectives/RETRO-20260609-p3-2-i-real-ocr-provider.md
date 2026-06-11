# Retrospective - P3-2-I Real OCR Provider

## 1. Feature Summary

完成 process-based real OCR provider boundary。`ProcessOcrFallbackProvider` 通过外部命令把 stdout 作为 OCR 文本返回，默认关闭，command 缺失 / 失败 / 超时会安全收敛，image-only PDF fallback 可走通。

## 2. What Went Well

- P3-2-F/P3-2-G/P3-2-H 让本切片只需要改 parser boundary 与配置，不必碰 `IndexService`。
- RED / GREEN / verification 路径清晰：先补测试，再补实现，再跑 focused、adjacent、compile、full backend。
- 没有引入 OCR SDK 依赖，避免把本切片变成依赖审批项目。
- Spring Boot 3.5 下 record 配置绑定和 provider 构造器歧义都被测试及时暴露，修复后全量上下文继续稳定。

## 3. What Didn't Go Well

- `RagParserOcrProperties` 里加入重载构造器后，Spring Boot 3.5 在某些测试上下文里会尝试 default constructor 路径，需要显式绑定注解才能稳定。
- `ProcessOcrFallbackProvider` 同时有测试友好的重载构造器和 Spring 注入构造器时，必须显式标注 `@Autowired`，否则全量上下文会走错路径。
- 这类 boundary slice 的验证链路长，尤其是 full backend test 需要一次性跑完才能确认没有上下文级回归。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| process-based OCR provider 应始终通过单一 `OcrFallbackService` 暴露，命令必须用 `ProcessBuilder(List<String>)` | Yes | `docs/skills/project-specific/rag-parser-boundary.md` |
| `@ConfigurationProperties` record + overload constructor 的 Spring Boot 3.5 绑定歧义需要显式 constructor binding | Yes | `docs/skills/project-specific/rag-parser-boundary.md` |
| 外部 OCR runtime command 必须做 timeout / stdout limit / stderr drain / no path leak / no shell | Yes | `docs/skills/project-specific/rag-parser-boundary.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Testing | 已有 fake provider + process provider success/failure/timeout 覆盖 | 真实 OCR provider 再补一组 resource-isolation 与 large-output 压力测试 |
| Configuration | record 配置对象 + 重载构造器可用 | 后续若继续加字段，优先保持单一 canonical path，减少 Spring binding 歧义 |
| Documentation | dependency review 已证明无新增 OCR SDK | 真实 OCR provider 切片必须另起 runtime / privacy review，不和 boundary slice 混写 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 继续真实 OCR provider 的 runtime / privacy / resource isolation 设计 | Main Codex | Future P3-2 slice |
| 继续工业级 PDF/DOCX layout/table/TOC 切片 | Main Codex | Future P3-2 slice |
| 继续 real VectorDB client 切片 | Main Codex | Future P3-2 slice |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] AGENT_RAG_MEMORY.md
- [x] CHANGELOG.md
- [x] backend-architecture-todolist.md
- [x] rag-parser-boundary skill
- [ ] ARCHITECTURE_BASELINE.md

未更新 `ARCHITECTURE_BASELINE.md`：本切片没有改变系统层级基线，只是在既有 `rag/parser` boundary 内新增 process OCR provider。
