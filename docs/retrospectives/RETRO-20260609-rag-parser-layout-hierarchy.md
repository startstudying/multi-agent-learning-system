# RETRO - P3-2-E RAG Parser Layout / Page Hierarchy

## 1. Feature Summary

完成 RAG parser layout/page hierarchy 的无新增依赖最小增强：

- PDF simple `/Type /Page` best-effort 分页。
- DOCX 同段 page break 拆 section。
- DOCX `w:tab` / 非分页 `w:br` 文本分隔。
- Index chunk page metadata 回归验证。

## 2. What Went Well

- RED 测试清楚复现了 PDF/DOCX page boundary 丢失问题。
- 实现保持在 `DocumentParserService` 内，未扩大到 API、DB、frontend 或依赖层。
- Full backend Maven 测试通过，说明 parser 改动未破坏 RAG adjacent 行为。

## 3. What Didn't Go Well

- 新开 gpt-5.5 子代理时命中 agent thread limit，未能再启动额外并行代码审查专家。
- 本切片仍受正则 parser 能力限制，不能替代真实 PDF/DOCX SDK 或 OCR。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| 无新增依赖 parser 增强必须先限定 best-effort 与非目标，避免误勾完整 parser/OCR TODO | Yes | `docs/skills/project-specific/rag-parser-boundary.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Workflow | 已按 Spec-first 和 Context Pack 执行 | 后续大 parser/OCR/VectorDB 必须独立 dependency review |
| Testing | focused -> adjacent -> full | 复杂 parser SDK 切片增加真实样本文档 fixture |
| Documentation | Evidence/Acceptance 明确非目标 | TODO 中继续拆分 best-effort 与工业级能力 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 评估真实 PDF/DOCX parser SDK 与 OCR fallback 依赖 | Main Codex / Dependency Expert | 后续 P3-2 parser SDK/OCR 切片 |
| 评估真实 VectorDB adapter | Main Codex / Dependency Expert | 后续 P3-2 VectorDB 切片 |
| 扩展 P3-4 权限渗透测试矩阵 | Main Codex / Security Expert | 后续 P3-4-K |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory file
- [ ] SKILL_REGISTRY.md（本切片未新增 skill）
- [ ] ARCHITECTURE_BASELINE.md（无架构漂移，不更新）
