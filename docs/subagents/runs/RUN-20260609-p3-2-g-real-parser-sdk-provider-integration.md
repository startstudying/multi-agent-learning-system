# Subagent Run - P3-2-G Real Parser SDK Provider Integration Review

## 1. 集成决策

采用单 Codex 串行实现，不启用并行代码修改。

## 2. 冲突解决

- Architecture 专家建议 provider 覆盖默认 lightweight parser；采纳。
- Dependency 评审建议只引入 PDFBox core 与 POI `poi-ooxml`；采纳。
- OCR 不纳入本切片；采纳。
- `DocumentParserService` 注册机制已满足覆盖需求，原则上不修改或只做极小调整。

## 3. 文件边界

允许修改：

- `backend/pom.xml`
- `backend/src/main/java/com/learningos/rag/parser/**`
- `backend/src/test/java/com/learningos/rag/parser/**`
- 本切片 workflow / evidence / acceptance / memory / changelog / retrospective / planning 文档

禁止修改：

- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- `docs/superpowers/**`
- `backend/src/main/java/com/learningos/rag/application/IndexService.java`
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java`
- `backend/src/main/java/com/learningos/rag/application/EmbeddingService.java`
- `backend/src/main/java/com/learningos/rag/application/VectorIndexAdapter.java`

## 4. 集成验收

- 真实 PDF/DOCX provider 在 Spring 上下文中覆盖默认 provider。
- 默认无参 `DocumentParserService` 仍可用于现有 lightweight 单元测试。
- 不新增 API/DB/frontend 变更。
- full backend Maven verification 通过后再更新 TODO 状态。

