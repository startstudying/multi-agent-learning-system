# PLAN - RAG Parser Adapter 最小生产切片

## Skill Selection Report

## Task Type

后端 RAG parser boundary 抽取与测试加固。

## Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 必须按 PRD / REQ / SPEC / PLAN / TASK / Context Pack 流程推进。 |
| `educational-rag-pipeline` | 本切片属于 RAG 文档解析与索引链路。 |
| `test-driven-development` | 需要先写 parser 边界 RED 测试，再抽实现。 |
| `confidence-check` | 先确认没有重复实现、架构边界清晰。 |
| `verification-before-completion` | 完成前必须用新鲜测试证据收口。 |
| `security-review` | 解析边界涉及文件内容、错误脱敏和未来依赖门禁。 |

## Missing Skills

无。

## GitHub Research Needed

否。本切片不新增依赖，当前实现模式可由现有代码库完成。

## New Project-Specific Skill To Create

暂不需要。

## Subagent Decision

Use Subagents: Yes
Reason: RAG、测试、安全三条线都要并行分析，且 parser 边界是后续 OCR/VectorDB 的前置设计。
Parallelism Level: L1
Selected Subagents: RAG/后端架构、Security & Quality、Test Strategy、Integration Reviewer
Implementation Mode: 并行分析 + 主 Codex 单线程实现

## Confidence Check

| Check | Result |
|---|---|
| No duplicate implementation | PASS - 解析逻辑仍内嵌在 `IndexService`，尚未拆出独立 parser 层。 |
| Architecture compliance | PASS - 继续使用 Java 21 / Spring Boot / 现有 RAG 结构，不引入新依赖。 |
| Official docs verified | PASS - 使用现有 Spring/JPA/Java 标准库模式即可。 |
| Working OSS reference | PASS - 本切片不依赖外部新库。 |
| Root cause identified | PASS - parser 边界未抽取，复杂解析与未来 OCR/VectorDB 扩展难以治理。 |

Confidence: 0.92

## 实施步骤

1. 先写 RED 测试，覆盖 parser selection、Markdown/TXT/PDF/DOCX 输出和失败脱敏。
2. 抽出 `rag/parser/**` 边界，让 `IndexService` 只做索引编排。
3. 保持 worker/manual 路径共用 parser 边界。
4. 跑聚焦测试，再跑全量后端测试。
5. 生成 Evidence / Acceptance / Retro，并更新 Memory / Changelog / TODO。

## 风险

- 不新增依赖意味着 PDF/DOCX/OCR 仍是轻量实现，只能先把边界抽出来。
- 若解析失败码不够稳定，可能污染索引任务可观测性。
- 本切片不能解决现有 dependency-check 的 CRITICAL/HIGH，需要后续独立安全治理。

## 完成状态

已完成。实现范围保持为无新依赖、无 schema/API 变更的最小 parser adapter 切片；Evidence、Acceptance、Retrospective、Changelog、Memory、TODO 和项目 skill 提取已补齐。
