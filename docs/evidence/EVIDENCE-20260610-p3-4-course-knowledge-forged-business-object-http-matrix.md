# EVIDENCE-20260610-p3-4-course-knowledge-forged-business-object-http-matrix

## 1. 任务

P3-4 子任务：Course/Knowledge forged business-object HTTP matrix

## 2. 变更摘要

测试-only 补强 Course/Knowledge 写路径 forged business-object HTTP 权限矩阵。

新增测试：

- `knowledgePointRejectsForgedForeignChapterIdForAuthorizedWritersWithoutSideEffects`
- `knowledgeDependencyRejectsCrossCourseForgedPrerequisiteWithoutSideEffects`

验证点：

- Bearer `TEACHER` 即使伪造 `X-User-Id: admin`，也不能使用 own `courseId` 拼接 foreign `chapterId` 创建知识点。
- Bearer `ADMIN` 也必须遵守 `chapterId` 属于请求 `courseId` 的业务一致性约束。
- Bearer `TEACHER` 即使伪造 `X-User-Id: admin`，也不能用本课程 `knowledgePointId` 拼接其他课程 `prerequisiteId` 创建依赖。
- 拒绝响应返回 `VALIDATION_ERROR` 且无 `data`，不泄露 forged ids、课程/章节/知识点标题或请求标题。
- 拒绝请求不新增 `KnowledgePoint` / `KnowledgeDependency`；dependency 失败后 course graph 依赖数量仍为 0。

## 3. 修改文件

- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
- `docs/tasks/TASK-20260610-p3-4-course-knowledge-forged-business-object-http-matrix.md`
- `docs/subagents/runs/RUN-20260610-p3-4-course-knowledge-forged-business-object-http-matrix.md`
- `docs/evidence/EVIDENCE-20260610-p3-4-course-knowledge-forged-business-object-http-matrix.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/AGENT_RAG_MEMORY.md`
- `docs/planning/backend-architecture-todolist.md`

## 4. Verification

### Focused

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest test
```

结果：

- `Tests run: 29, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Adjacent

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseKnowledgeControllerTest,CourseAccessServiceTest test
```

结果：

- `Tests run: 35, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Full backend

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- `Tests run: 574, Failures: 0, Errors: 0, Skipped: 1`
- `BUILD SUCCESS`

## 5. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 测试-only；生产权限仍在 Service 层执行。 |
| Frontend rules | PASS | 未改前端。 |
| Agent / RAG rules | PASS | 未改 Agent/RAG runtime。 |
| Security | PASS | 补强 Course/Knowledge forged business-object、防响应泄露、无副作用测试。 |
| API / Database | PASS | 未改 API contract 或 schema。 |

## 6. Acceptance

| Criteria | Verdict |
|---|---|
| `courseId` + foreign `chapterId` 创建知识点拒绝路径已覆盖 | PASS |
| 跨课程 `knowledgePointId` + `prerequisiteId` 创建依赖拒绝路径已覆盖 | PASS |
| 拒绝响应不泄露 forged ids / 标题 / 请求标题，且无 `data` | PASS |
| 拒绝请求无 `KnowledgePoint` / `KnowledgeDependency` 持久化副作用 | PASS |
| 专家 subagent 并行只读审查已集成 | PASS |
| focused / adjacent / full backend 验证完成 | PASS |

最终结论：PASS。P3-4 父项仍保持 open。
