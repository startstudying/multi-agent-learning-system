# AI QA 无资料降级回答验收

## 验收结论

Accepted。

## 验收标准

| 标准 | 结果 |
|---|---|
| AI QA no-source 响应返回 `GENERAL_FALLBACK` 和 `NO_COURSE_SOURCE_FALLBACK` | PASS |
| AI QA no-source 响应不展示为 ERROR，不展示旧安全拒答状态 | PASS |
| AI QA 有来源响应返回 `COURSE_GROUNDED` 和引用 | PASS |
| RAG no-source 测试仍保持 `NO_SOURCE_REFUSAL` | PASS |
| 前端测试和构建通过 | PASS |

## 证据

- `docs/evidence/EVIDENCE-20260611-ai-qa-no-source-fallback.md`

## 剩余风险

- 通用回答仍是规则化占位文本，不是真实模型回答；需要后续 model gateway slice 接入真实普通问答。
- `/api/rag/query` 严格 no-source 语义保留，调用方需要按接口语义选择 AI QA 或纯 RAG。
