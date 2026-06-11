# P3-2 子任务：DOCX table/TOC reading-order provider Retro

日期：2026-06-10

## 做得有效的点

- 先用专家 subagent 并行确认范围，避免把本切片扩大成完整 DOCX layout engine。
- 先写 RED 测试，明确暴露了两个真实问题：TOC 被索引、table 被丢失或拆成普通 TEXT cell。
- 沿用已有 metadata contract，没有改 `IndexService`，降低了架构漂移风险。

## 关键决策

- table 作为 `TABLE_TEXT` 独立 section/chunk 进入索引。
- TOC-like paragraph 默认 skip，而不是作为 `TOC_TEXT` 索引。
- `pageNumSource` 继续使用 `PARSER_INFERRED`，不伪装真实渲染页码。
- `layoutConfidence` 不赋值，避免制造不可解释的 layout confidence。

## 后续建议

- 若要支持目录本身作为可见导航 metadata，应另开子任务，不要混入 RAG 正文 chunk。
- 若要支持合并单元格、嵌套表格、页眉页脚、脚注尾注，需要单独设计结构化 table/layout 模型。
- 后续 PDF layout/table/TOC provider 不应复用 DOCX 的表格文本规则作为完整 layout 结论。
