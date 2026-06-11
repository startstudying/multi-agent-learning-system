# Prompt Version 质量对比 Context Pack

## 目标

完成 `docs/planning/backend-architecture-todolist.md` P2-1 第 143 行：支持按 prompt version 对比质量指标。

## 允许修改范围

- `backend/src/main/java/com/learningos/evaluation/**`
- `backend/src/main/resources/db/migration/V14__evaluation_run_quality_metrics.sql`
- `backend/src/test/java/com/learningos/evaluation/**`
- `backend/src/test/java/com/learningos/migration/SchemaConvergenceMigrationTest.java`
- `docs/product/PRD-20260606-prompt-version-quality-comparison.md`
- `docs/requirements/REQ-20260606-prompt-version-quality-comparison.md`
- `docs/specs/SPEC-20260606-prompt-version-quality-comparison.md`
- `docs/plans/PLAN-20260606-prompt-version-quality-comparison.md`
- `docs/tasks/TASK-20260606-prompt-version-quality-comparison.md`
- `docs/evidence/EVIDENCE-20260606-prompt-version-quality-comparison.md`
- `docs/acceptance/ACCEPT-20260606-prompt-version-quality-comparison.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/*.md`

## 不做范围

- 不改现有 RAG query 业务链路。
- 不改现有 answer submission 业务链路。
- 不做真实模型 A/B 执行。
- 不新增依赖。
- 不保存 raw prompt、raw model output、学生原始答案全文。

## 并行子任务记录

- Architect: 已返回，建议 `evaluation_run` + `evaluation_run_metric`，comparison 放在 `evaluation` 模块。
- Test Engineer: 已返回，建议新增 service/controller/migration 测试，重点覆盖权限和 run 不足。
- Security Reviewer: 已返回，要求先按 evaluation set 鉴权，禁止返回 promptText、answerText、inputJson、outputJson、rawOutput 等敏感字段。
