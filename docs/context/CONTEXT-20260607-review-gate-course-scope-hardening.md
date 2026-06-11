# CONTEXT - Review Gate 课程范围收口

## 1. 相关记忆与文档

- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/specs/SPEC-20260606-review-gate-state-model.md`
- `docs/specs/SPEC-20260606-review-gate-authorization-hardening.md`
- `docs/specs/SPEC-20260606-permission-security-hardening.md`
- `docs/subagents/runs/RUN-20260607-p3-permission-security-analysis.md`
- `docs/subagents/runs/RUN-20260607-p3-integration-review.md`

## 2. 选用技能

- `feature-development-workflow`
- `security-review`
- `test-driven-development`
- `verification-before-completion`
- `agent-trace-governance`

## 3. 子代理结论

- 安全分析建议先做 Review + Course Scope 收口，避免直接跳到完整 RBAC。
- 集成评审将其列为 P3 下一个最小实施切片。
- 该切片不需要新增依赖或数据库迁移。

## 4. 允许修改的文件

- `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java`
- `backend/src/main/java/com/learningos/agent/api/ResourceReviewController.java`
- `backend/src/main/java/com/learningos/knowledge/repository/CourseRepository.java`（仅如需新增查询方法，优先避免）
- `backend/src/test/java/com/learningos/agent/application/ReviewGovernanceServiceTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java`
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`（仅用于补齐 Review Gate 回归测试所需课程归属夹具）
- `docs/product/PRD-20260607-review-gate-course-scope-hardening.md`
- `docs/requirements/REQ-20260607-review-gate-course-scope-hardening.md`
- `docs/specs/SPEC-20260607-review-gate-course-scope-hardening.md`
- `docs/plans/PLAN-20260607-review-gate-course-scope-hardening.md`
- `docs/tasks/TASK-20260607-review-gate-course-scope-hardening.md`
- `docs/context/CONTEXT-20260607-review-gate-course-scope-hardening.md`
- `docs/evidence/EVIDENCE-20260607-review-gate-course-scope-hardening.md`
- `docs/acceptance/ACCEPT-20260607-review-gate-course-scope-hardening.md`
- `docs/retrospectives/RETRO-20260607-review-gate-course-scope-hardening.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/memory/API_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 5. 不允许修改的文件

- `docs/superpowers/**`
- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- 模型接入、Embedding、VectorDB、OCR 相关文件

## 6. 测试命令

```bash
cd backend && mvn "-Dtest=ReviewGovernanceServiceTest,ResourceReviewControllerTest" test
cd backend && mvn test
```

## 7. 边界

本切片只处理 Review Gate 的课程教师范围收口，不扩展知识目录、RAG、Assessment 的完整 RBAC，也不引入认证体系改造。
