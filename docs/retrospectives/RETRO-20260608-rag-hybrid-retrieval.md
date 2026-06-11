# Retrospective - P3-2 RAG Hybrid Retrieval / RRF / Reranker Fallback

## 1. Feature Summary

完成 RAG 在线检索最小 hybrid-ready 切片：allowed KB 内 keyword branch、recency fallback branch、RRF fusion、reranker timeout/error fallback，以及 `sourcesJson` 白名单 metadata。

## 2. What Went Well

- 专家集成评审先明确“不新增依赖、不改 schema、不接真实 VectorDB/reranker provider”，避免范围膨胀。
- TDD RED 先暴露缺失的 `RrfRanker` 与 `RerankerService` 状态化 API，再实施最小算法。
- `retrieval.strategy` 与 `rerankerStatus` 语义拆清：前者保留业务路由，后者改为稳定 reranker 枚举。
- metadata 脱敏约束前置，timeout/error fallback 没有把 raw provider error、secret 或 candidate full text 写入 `sourcesJson`。
- 聚焦、相邻、全量后端测试均通过。

## 3. What Didn't Go Well

- Agent/RAG 专家报告未能落盘，只能由集成评审引用其通知结论；后续并行专家流程应确保每个专家产出都进入 `docs/subagents/runs/`。
- 当前 `RerankerService` provider hook 是扩展边界，不是真实 provider 接入；文档必须持续强调真实 provider 仍是后续切片。
- 既有 query log 中 `question` / `responseJson` replay snapshot 是存量敏感面，本切片不能顺手治理，只能保证新增 metadata 不扩大泄露。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| allowed KB first、keyword/recency/RRF、vector disabled metadata、reranker fallback status、安全 `sourcesJson` | Yes | `docs/skills/project-specific/rag-hybrid-retrieval.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Subagent evidence | 个别专家结论只在通知中返回 | 并行专家必须统一写 `docs/subagents/runs/RUN-*.md`，否则集成评审要显式标记缺口 |
| RAG P3-2 范围 | hybrid retrieval 容易和 VectorDB/embedding 混淆 | TODO 与 Acceptance 中持续区分“无依赖 hybrid-ready”与“真实 VectorDB” |
| Query log 安全 | 新增 metadata 已脱敏，但存量 replay snapshot 仍含业务内容 | 后续单独做 query log retention / audit split / 脱敏治理切片 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 设计 P3-2-B embedding service 和可选 VectorDB adapter，含 dependency / schema review | 后续开发者 | 新建 PRD/REQ/SPEC/PLAN/TASK |
| 设计真实 reranker provider 接入与 provider timeout 配置治理 | 后续开发者 | 新建独立切片 |
| 设计 query log retention / replay snapshot 与 audit log 分离 | 后续开发者 | 新建安全治理切片 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] AGENT_RAG_MEMORY.md
- [x] CHANGELOG.md
- [x] SKILL_REGISTRY.md
- [ ] ARCHITECTURE_BASELINE.md（本切片无基线规则变更）
