# PLAN-20260608 P3-4-H RAG Document Course/Chapter Metadata Scope

## 1. Skill Selection Report

### Task Type

Security hardening / RAG metadata authorization / bug fix.

### Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 项目强制 Spec-First 流程。 |
| object-scope-authorization | course/chapter scope、IDOR、missing-vs-foreign 语义。 |
| rag-project-review | RAG 文档上传和索引元数据边界。 |
| security-review | 权限绕过、元数据污染和响应泄露审查。 |
| spring-boot-architecture | Spring Controller/Service/Repository 分层。 |
| test-driven-development | 先写 RED 权限矩阵测试。 |
| verification-before-completion | 完成前用真实测试输出证明。 |
| Confidence Check | 实现前确认重复、架构、根因。 |

### Missing Skills

无。

### GitHub Research Needed

No。本切片不新增依赖、不接外部 API，不需要 GitHub reference。

### New Project-Specific Skill To Create

不新建；完成后扩展 `object-scope-authorization` 或 `rag-parser-boundary` 中的 RAG metadata scope 规则。

## 2. Subagent Decision

- Use Subagents: Yes
- Reason: 涉及 RAG pipeline + Security + Backend service 边界，且用户要求专家 subagent 并行开发。
- Parallelism Level: L1 Parallel Analysis
- Selected Subagents:
  - Backend/RAG Expert：分析当前上传链路和最小实现。
  - Security & Quality：分析权限矩阵、IDOR、响应语义和测试矩阵。
  - Integration Reviewer：由 Main Codex 合并报告，固化最终 PLAN/TASK。
- Implementation Mode: Main Codex 单线程实现。

## 3. Confidence Check

| Check | Status | Notes |
|---|---|---|
| No duplicate implementation | PASS | 当前 `DocumentService.upload` 保存 `courseId/chapterId`，但未执行 course/chapter scope 校验。 |
| Architecture compliance | PASS | 复用 `CourseAccessService`、`ChapterRepository`、`DocumentService`。 |
| Official docs needed | PASS | 无外部 SDK/API；使用既有 Spring Data JPA。 |
| OSS references needed | PASS | 不需要。 |
| Root cause identified | PASS | KB 写权限和课程元数据写权限不是同一边界；当前只校验前者。 |

Confidence: 0.94

## 4. Implementation Steps

1. [x] 创建 PRD / REQ / SPEC / PLAN / TASK / Context Pack。
2. [x] 并行专家分析并写入 subagent reports。
3. [x] RED：在 `DocumentControllerTest` 增加 course/chapter scope 失败与成功用例。
4. [x] GREEN：`DocumentService` 注入 `CourseAccessService` / `ChapterRepository`，在存储文件前校验 metadata scope。
5. [x] GREEN：补 test helper 创建 course/chapter；调整旧测试中随意 course/chapter 参数。
6. [x] 运行 focused、adjacent、full backend tests。
7. [x] 更新 Evidence / Acceptance / Changelog / Memory / TODO / Retrospective / Skill。

## 7. Integration Review

- Backend/RAG Expert report: `docs/subagents/runs/RUN-20260608-rag-document-course-scope-backend.md`
- Security & Quality report: `docs/subagents/runs/RUN-20260608-rag-document-course-scope-security.md`
- Integration report: `docs/subagents/runs/RUN-20260608-rag-document-course-scope-integration.md`

Integrated decisions:

- 校验点放在 `storageService.store(...)` 前。
- `courseId` 非空时同时要求 course read 与 course manage。
- `chapterId` 非空但无 `courseId` 使用 `VALIDATION_ERROR`。
- missing/foreign chapter 使用固定通用 `VALIDATION_ERROR`。
- 不新增 schema、依赖、响应字段或 frontend 变更。

## 5. Risks

| Risk | Mitigation |
|---|---|
| 旧测试随意传 `course_java/sql_join` 被拒绝 | 修改测试 seed 合法 course/chapter；无 metadata 路径保留兼容。 |
| 缺少 `KnowledgeBase.courseId` 不能校验 KB 与 course 绑定 | 本切片记录为后续 schema 任务，不伪造完成。 |
| chapter missing vs foreign 形成枚举 | 统一 `VALIDATION_ERROR` 固定文案，不返回 offending id。 |
| 失败请求已写对象存储 | 校验放在 `storageService.store(...)` 之前。 |

## 6. Test Commands

```powershell
cd backend
mvn --% -Dtest=DocumentControllerTest test
mvn --% -Dtest=DocumentControllerTest,CourseKnowledgeControllerTest,RagQueryServiceTest,IndexServiceTest test
mvn test
```
