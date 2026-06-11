# Retrospective - P3-2-G Real PDF/DOCX Parser SDK Provider

## 1. Feature Summary

完成真实 Apache PDFBox / Apache POI DOCX provider 的最小接入。`DocumentParserService` 在 Spring 上下文中可使用真实 PDF/DOCX provider 覆盖默认 lightweight provider，同时无参构造继续保留 lightweight 测试兼容行为。

## 2. What Went Well

- P3-2-F provider boundary 让本切片可以只在 `rag/parser` 内接入 SDK，不触碰 `IndexService`。
- RED 明确暴露缺少 PDFBox/POI/provider/resource limit，测试驱动边界清楚。
- focused、adjacent、dependency tree、full backend verification 都完成 fresh run。
- `commons-logging` transitive dependency 被排除，避免与 Spring JCL 冲突。

## 3. What Didn't Go Well

- 收尾阶段额外启动的两个只读复核 subagent 因 stream 断线未返回结果，不能作为验收证据。
- 当前工作区根目录不是 git repository，无法用 `git status/diff` 做范围证明，只能依赖文件清单和命令输出。
- PDF/DOCX 真实 SDK 接入后仍不是工业级 layout/OCR 能力，文档中必须持续避免夸大。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Real parser SDK provider 只能接在 `rag/parser` provider boundary 后面 | Yes | 更新 `docs/skills/project-specific/rag-parser-boundary.md` |
| 新 parser SDK 必须配 dependency review、资源限制、safe error mapping、focused/adjacent/full verification | Yes | 复用并扩展 `rag-parser-boundary` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | P3-2 仍有多个大项混在同一 TODO bullet | 继续用 P3-2-G/H/I 小切片推进，TODO bullet 保持 open 并标明已完成子能力 |
| Testing | 真实 provider 已有 focused tests | 后续 OCR/layout 切片增加更多 malformed/large/timeout 文件样本与隔离策略 |
| Documentation | Dependency review 已记录官方来源和风险 | 后续如引入 OCR/native/service dependency，必须单独做 runtime/privacy/security review |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 设计真实 OCR fallback 的依赖、隐私和运行时隔离策略 | Main Codex | Future P3-2 slice |
| 设计工业级 PDF/DOCX layout/page/section/table 识别增强 | Main Codex | Future P3-2 slice |
| 如接入 real VectorDB client，单独进行 dependency/security/schema/API 评审 | Main Codex | Future P3-2 slice |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] AGENT_RAG_MEMORY.md
- [x] CHANGELOG.md
- [x] backend-architecture-todolist.md
- [x] rag-parser-boundary skill
- [ ] ARCHITECTURE_BASELINE.md

未更新 `ARCHITECTURE_BASELINE.md`：本切片没有改变系统层级基线，只是在既有 `rag/parser` boundary 内新增真实 SDK provider。
