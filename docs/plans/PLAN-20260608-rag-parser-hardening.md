# PLAN - P3-2-C RAG 无依赖 Parser 加固

## 1. 追踪

- PRD：`docs/product/PRD-20260608-rag-parser-hardening.md`
- REQ：`docs/requirements/REQ-20260608-rag-parser-hardening.md`
- SPEC：`docs/specs/SPEC-20260608-rag-parser-hardening.md`

## 2. 实施阶段

| 阶段 | 说明 | 关联任务 | 状态 |
|---|---|---|---|
| 1 | 创建 workflow docs 与 Context Pack | TASK-01 | 已完成 |
| 2 | 按 TDD 添加 parser/index RED tests | TASK-02 | 已完成 |
| 3 | 实现 PDF/DOCX/TXT/Markdown parser hardening | TASK-03 | 已完成 |
| 4 | 运行 focused/adjacent/full verification | TASK-04 | 已完成 |
| 5 | Evidence/Acceptance/Memory/Changelog/Retro/Skill 更新 | TASK-05 | 已完成 |

## 3. 文件变更清单

| 文件 | 操作 | 阶段 | 负责人 |
|---|---|---|---|
| `docs/product/PRD-20260608-rag-parser-hardening.md` | 新增 | 1 | Main Codex |
| `docs/requirements/REQ-20260608-rag-parser-hardening.md` | 新增 | 1 | Main Codex |
| `docs/specs/SPEC-20260608-rag-parser-hardening.md` | 新增/更新 | 1/5 | Main Codex |
| `docs/plans/PLAN-20260608-rag-parser-hardening.md` | 新增/更新 | 1/5 | Main Codex |
| `docs/tasks/TASK-20260608-rag-parser-hardening.md` | 新增/更新 | 1/5 | Main Codex |
| `docs/context/CONTEXT-20260608-rag-parser-hardening.md` | 新增 | 1 | Main Codex |
| `docs/subagents/runs/RUN-20260608-rag-parser-hardening-*.md` | 新增/引用 | 1 | Main Codex/Subagents |
| `backend/src/main/java/com/learningos/rag/parser/DocumentParserService.java` | 修改 | 3 | Main Codex |
| `backend/src/test/java/com/learningos/rag/parser/DocumentParserServiceTest.java` | 修改 | 2 | Main Codex |
| `backend/src/test/java/com/learningos/rag/application/IndexServiceTest.java` | 修改 | 2 | Main Codex |
| `backend/src/test/java/com/learningos/rag/application/IndexServiceParserFailureTest.java` | 修改 | 2 | Main Codex |
| `docs/evidence/EVIDENCE-20260608-rag-parser-hardening.md` | 新增 | 5 | Main Codex |
| `docs/acceptance/ACCEPT-20260608-rag-parser-hardening.md` | 新增 | 5 | Main Codex |
| `docs/retrospectives/RETRO-20260608-rag-parser-hardening.md` | 新增 | 5 | Main Codex |
| `docs/changelog/CHANGELOG.md` | 更新 | 5 | Main Codex |
| `docs/memory/PROJECT_MEMORY.md` | 更新 | 5 | Main Codex |
| `docs/memory/BACKEND_MEMORY.md` | 更新 | 5 | Main Codex |
| `docs/memory/AGENT_RAG_MEMORY.md` | 更新 | 5 | Main Codex |
| `docs/skills/project-specific/rag-parser-boundary.md` | 更新 | 5 | Main Codex |
| `docs/skills/SKILL_REGISTRY.md` | 按需更新 | 5 | Main Codex |
| `docs/planning/backend-architecture-todolist.md` | 更新 | 5 | Main Codex |

## 4. 依赖

- 前置条件：P3-2 minimal parser adapter、chunk metadata、embedding/vector adapter boundary 已完成。
- 新增依赖：无。

## 5. 风险评估

| 风险 | 影响 | 缓解措施 |
---|---|---|
| 无依赖 PDF 解析覆盖有限 | 不能处理复杂 PDF/OCR | 明确非目标；无文本返回空，不索引 raw bytes |
| DOCX regex 解析不完整 | 复杂 DOCX 结构可能丢失 | 只做 paragraph/heading/page-break best-effort，不宣称完整 DOCX |
| 二进制判定误伤少量文本 | 可能使异常文件 parse failed | 阈值保守，覆盖 UTF-8 中文和常规标点 |
| 更改 DOCX `pageNum` 行为 | 现有测试需更新 | 在 REQ/SPEC 中明确 best-effort 从 1 开始 |

## 6. 回滚策略

- 代码回滚：只回滚 `DocumentParserService` 与相关测试。
- 行为回滚：PDF 可恢复旧 `Tj` 抽取但仍不建议恢复 raw fallback。
- 文档回滚：不删除已生成 evidence/acceptance，改为记录失败或撤销原因。

## 7. 测试策略

```powershell
cd backend
mvn --% -Dtest=DocumentParserServiceTest test
mvn --% -Dtest=IndexServiceTest,IndexServiceParserFailureTest test
mvn --% -Dtest=DocumentParserServiceTest,IndexServiceParserFailureTest,IndexServiceTest,IndexTaskWorkerSchedulerTest,SchemaConvergenceMigrationTest test
mvn --% dependency:tree -Dscope=compile
mvn test
```

## 8. Subagent 计划

| 专家 | 是否需要 | 职责 |
|---|---|---|
| Agent/RAG 专家 | 是 | parser/RAG 边界分析 |
| 安全与质量 | 是 | zip/PDF/binary/raw error/dependency drift 审查 |
| 集成评审员 | 是 | 汇总报告并形成最终执行边界 |

并行级别：L1 分析。实现模式：单 Codex。

## 9. Architecture Drift Check

实施前与实施后结果：

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | parser 逻辑保持在 `rag/parser`；`IndexService` 只消费 parser section |
| Frontend rules | PASS | 不触达前端 |
| Agent / RAG rules | PASS | 不改变 retrieval/citation/trace |
| Security | PASS | 不新增依赖，parser 输入按不可信数据处理；DOCX 不再隐式解压非目标 entry |
| API / Database | PASS | 不新增 endpoint/schema |

## 10. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-08 | 已完成 |
| Code Reviewer / Feynman | 2026-06-08 | APPROVE |

## 11. 完成记录

- Evidence：`docs/evidence/EVIDENCE-20260608-rag-parser-hardening.md`
- Acceptance：`docs/acceptance/ACCEPT-20260608-rag-parser-hardening.md`
- Retrospective：`docs/retrospectives/RETRO-20260608-rag-parser-hardening.md`
- 最终验证：`mvn test` 通过，298 tests, 0 failures, 0 errors, 1 skipped。
