# EVIDENCE - P3-4-C 权限矩阵安全前置

## 1. 追溯

- TASK：`docs/tasks/TASK-20260608-rbac-course-class-answer-matrix.md`
- SPEC：`docs/specs/SPEC-20260608-rbac-course-class-answer-matrix.md`
- 总控 TASK：`docs/tasks/TASK-20260608-backend-p3-productionization-integration.md`
- 日期：2026-06-08

## 2. 实现内容

本切片收口课程读取、知识图谱读取和评分评估接口权限：

- `GET /api/courses`
  - `admin` 返回全部课程。
  - `teacher` 仅返回 `teacherId == currentUserId` 的课程。
  - 普通 `student` 暂返回空列表，等待后续 class/enrollment 模型。
- `GET /api/courses/{courseId}`
  - `admin` 可读取任意存在课程，missing 返回 `NOT_FOUND`。
  - 非 admin 对 missing / foreign course 均返回 `FORBIDDEN`，响应无 `data`。
- `GET /api/courses/{courseId}/knowledge-graph`
  - 复用同一课程读取授权。
- `POST /api/assessment/grading-evaluations`
  - 仅 `admin` / `teacher` 可执行，普通 student 返回 `FORBIDDEN`。

未新增依赖、未修改数据库 schema、未修改前端。

## 3. 变更文件

| 文件 | 操作 | 摘要 |
|---|---|---|
| `backend/src/main/java/com/learningos/knowledge/api/CourseController.java` | 修改 | Controller 只传入 `currentUserId`，课程读取和图谱读取委托 Service 授权。 |
| `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java` | 修改 | 增加 scoped course list/detail/graph 读取规则，非 admin missing/foreign 收敛为 `FORBIDDEN`。 |
| `backend/src/main/java/com/learningos/knowledge/repository/CourseRepository.java` | 修改 | 增加按 `teacherId` 查询课程列表的方法。 |
| `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java` | 修改 | 评分评估接口传入当前用户。 |
| `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java` | 修改 | 增加 teacher/admin gate。 |
| `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java` | 修改 | 覆盖课程列表、详情、知识图谱 missing/foreign/role scope。 |
| `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java` | 修改 | 覆盖 student 禁止执行 grading evaluation。 |
| `docs/subagents/runs/RUN-20260608-backend-p3-productionization-model-gateway.md` | 新增 | 补齐 Model Gateway Expert 只读分析报告。 |
| `docs/acceptance/ACCEPT-20260608-rbac-course-class-answer-matrix.md` | 新增 | 记录验收结论、测试摘要和遗留问题。 |
| `docs/tasks/TASK-20260608-rbac-course-class-answer-matrix.md` | 修改 | 标记 Done Criteria 和验证结果。 |
| `docs/plans/PLAN-20260608-rbac-course-class-answer-matrix.md` | 修改 | 标记执行步骤完成并记录验证结果。 |
| `docs/planning/backend-architecture-todolist.md` | 修改 | 标记 P3-4-C 课程读取/评分评估安全前置完成。 |
| `docs/memory/PROJECT_MEMORY.md` | 修改 | 更新项目功能表与近期计划记录。 |
| `docs/memory/BACKEND_MEMORY.md` | 修改 | 更新后端权限记忆和测试记录。 |
| `docs/memory/API_MEMORY.md` | 修改 | 更新课程读取和 grading evaluation 权限语义。 |
| `docs/changelog/CHANGELOG.md` | 修改 | 记录 P3-4-C 变更。 |
| `docs/retrospectives/RETRO-20260608-rbac-course-class-answer-matrix.md` | 新增 | 记录复盘和后续 action items。 |
| `docs/skills/project-specific/object-scope-authorization.md` | 修改 | 补充 course list scope 和无 courseId 评估接口 gate 模式。 |
| `docs/skills/SKILL_REGISTRY.md` | 修改 | 更新 `object-scope-authorization` 描述。 |

## 4. 测试结果

### 4.1 聚焦测试

```powershell
cd backend
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest test
```

结果：

- BUILD SUCCESS
- Tests run: 19
- Failures: 0
- Errors: 0
- Skipped: 0
- Finished at: 2026-06-08T12:30:38+08:00

### 4.2 相邻回归

```powershell
cd backend
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,DocumentControllerTest test
```

结果：

- BUILD SUCCESS
- Tests run: 71
- Failures: 0
- Errors: 0
- Skipped: 0
- Finished at: 2026-06-08T12:31:54+08:00

### 4.3 后端全量测试

```powershell
cd backend
mvn test
```

结果：

- BUILD SUCCESS
- Tests run: 302
- Failures: 0
- Errors: 0
- Skipped: 1
- Finished at: 2026-06-08T12:34:04+08:00

## 5. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只处理 HTTP/current user，权限判断在 `KnowledgeCatalogService` / `GradingEvaluationService`。 |
| Frontend rules | PASS | 未修改 frontend，未引入前端 LLM/API key。 |
| Agent / RAG rules | PASS | 未修改 Agent/RAG 执行链路；知识图谱读取仅增加后端权限门禁。 |
| Security | PASS | 权限在后端代码执行；非 admin missing/foreign course 返回同类 `FORBIDDEN`；未新增依赖；未写入秘密。 |
| API / Database | PASS | 未修改 API path 或 schema；响应错误语义与 SPEC 一致。 |

## 6. 验证限制

- 当前身份仍基于过渡 `X-User-Id` 字符串模型，不是正式 JWT/RBAC。
- 当前没有 class/enrollment schema，因此 student course list 暂返回空列表；学生 enrolled course 读取将在后续 class/course 矩阵切片实现。
- `GradingEvaluationRequest` 当前不携带 `courseId`，本切片只能做 teacher/admin gate，不能做 course teacher scope。
- 本切片不新增答题记录详情 API，也不覆盖 answer record 独立查询矩阵。
