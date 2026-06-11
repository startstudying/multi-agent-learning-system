# CONTEXT-20260610-p3-4-assessment-submit-foreign-questionid

## 1. Related Memory / Docs

- `AGENTS.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/skills/project-specific/object-scope-authorization.md`
- `docs/skills/project-specific/auth-context-boundary.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`
- `docs/subagents/runs/RUN-20260610-p3-4-assessment-submit-foreign-questionid-matrix.md`

## 2. Selected Skills

- `feature-development-workflow`
- `object-scope-authorization`
- `auth-context-boundary`
- `spring-boot-architecture`
- `test-driven-development`

## 3. Subagent Plan

用户要求专家 subagent 并行开发。本切片已由 Assessment security expert 补充 RED 测试并输出 run 报告。生产修复由主线 Codex 单线程完成，避免同文件冲突。

## 4. Allowed Files

- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `backend/src/test/java/com/learningos/assessment/application/AssessmentServiceTest.java`
- 本任务相关 `REQ/SPEC/PLAN/TASK/CONTEXT/EVIDENCE/ACCEPT`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 5. Disallowed Files

- `backend/pom.xml`
- `backend/src/main/resources/db/migration/**`
- `frontend/**`
- 非 assessment submit 生产代码

## 6. Test Commands

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest,AssessmentServiceTest test
mvn --% -Dtest=AssessmentControllerTest,AssessmentServiceTest,OrchestratorWorkflowControllerTest test
mvn test
```

## 7. Current Boundary

只修复 `POST /api/assessment/answers` 课程绑定 `questionId` 提交授权。若后续要引入正式题库表、question-course schema、admin 代提交、前端交互变化或新依赖，必须另开任务。
