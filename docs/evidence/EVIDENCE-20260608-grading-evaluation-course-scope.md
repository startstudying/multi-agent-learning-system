# EVIDENCE - P3-4-G Grading Evaluation Course Scope

## 1. 追溯

- PRD：`docs/product/PRD-20260608-grading-evaluation-course-scope.md`
- REQ：`docs/requirements/REQ-20260608-grading-evaluation-course-scope.md`
- SPEC：`docs/specs/SPEC-20260608-grading-evaluation-course-scope.md`
- PLAN：`docs/plans/PLAN-20260608-grading-evaluation-course-scope.md`
- TASK：`docs/tasks/TASK-20260608-grading-evaluation-course-scope.md`
- Context Pack：`docs/context/CONTEXT-20260608-grading-evaluation-course-scope.md`
- 日期：2026-06-08

## 2. 实现内容

本切片将 `POST /api/assessment/grading-evaluations` 从 teacher/admin 角色门禁收口为 course-scoped evaluation：

- `GradingEvaluationRequest` 新增 `courseId`。
- HTTP service path 强制 teacher/admin 提供 `courseId`。
- student / 其他普通用户优先返回 `FORBIDDEN`，不进入 course/sample 细节校验。
- teacher 只能对 own course 运行 grading evaluation。
- teacher foreign/missing course 均返回 `FORBIDDEN`，避免 course 存在性探测。
- admin 可对任意 existing course 运行；admin missing course 返回 `NOT_FOUND`。
- `samples[].knowledgePointId` 非空时必须属于请求 `courseId`；foreign/missing KP 统一返回 `VALIDATION_ERROR`。
- 保留纯指标计算方法和响应指标 shape；未改 grading 公式。

未新增依赖、未新增 migration、未修改 frontend。

## 3. 变更文件

| 文件 | 操作 | 摘要 |
|---|---|---|
| `backend/src/main/java/com/learningos/assessment/dto/GradingEvaluationRequest.java` | 修改 | 增加 `courseId` 字段并保留既有构造器兼容纯指标测试。 |
| `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java` | 修改 | 注入 `CourseAccessService` / `KnowledgePointRepository`，实现 student-first deny、course gate 和 sample KP course consistency 校验。 |
| `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java` | 修改 | 覆盖 teacher/admin/student course scope 矩阵、sample KP outside course、legacy score array with course 等场景。 |
| `backend/src/test/java/com/learningos/assessment/application/GradingEvaluationServiceTest.java` | 修改 | 为 service 构造器注入 mock，保留纯指标计算测试。 |
| `docs/subagents/runs/RUN-20260608-grading-evaluation-course-scope-*.md` | 新增 | 固化 Backend / Security / Integration subagent 分析报告。 |

## 4. TDD RED 证据

新增 controller 权限矩阵测试后运行：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest test
```

结果：

- BUILD FAILURE
- Tests run: 27
- Failures: 5
- Errors: 0
- Skipped: 0
- 失败原因：当前接口忽略 `courseId`，teacher missing/foreign/missing course、admin missing course、sample KP outside course 均仍返回 200。

失败测试：

- `rejectsTeacherGradingEvaluationWithoutCourseId`
- `teacherCanOnlyRunGradingEvaluationForOwnCourse`
- `teacherCannotDistinguishMissingCourseFromForbiddenGradingEvaluationCourse`
- `adminMissingGradingEvaluationCourseReturnsNotFound`
- `rejectsGradingEvaluationSampleOutsideRequestCourse`

## 5. GREEN / 回归测试结果

### 5.1 聚焦测试

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest test
```

最终结果：

- BUILD SUCCESS
- Tests run: 29
- Failures: 0
- Errors: 0
- Skipped: 0
- Finished at: 2026-06-08T18:14:48+08:00

### 5.2 相邻回归

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest,GradingEvaluationServiceTest,CourseKnowledgeControllerTest,AnalyticsControllerTest test
```

结果：

- BUILD SUCCESS
- Tests run: 58
- Failures: 0
- Errors: 0
- Skipped: 0
- Finished at: 2026-06-08T18:15:45+08:00

### 5.3 后端全量测试

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- BUILD SUCCESS
- Tests run: 329
- Failures: 0
- Errors: 0
- Skipped: 1
- Finished at: 2026-06-08T18:17:53+08:00

## 6. 安全审查

### 6.1 Subagent 安全审查

Security & Quality subagent 结论：

- `courseId` 应强制必填，否则 teacher 仍可绕过 course scope。
- student 应优先 `FORBIDDEN`，避免通过 course/sample 错误差异探测对象存在性。
- teacher missing/foreign course 应统一 `FORBIDDEN`。
- sample `knowledgePointId` 只能作为一致性校验，不能作为授权锚点。

本实现采纳：

- `evaluate(currentUserId, request)` 中先拒绝非 teacher/admin，再校验 `courseId`。
- 复用 `CourseAccessService.requireCourseRead(...)`。
- sample 非空 `knowledgePointId` 必须出现在请求 course 的知识点集合中。
- 错误消息使用固定文案 `Sample knowledge points must belong to request course`，不返回 offending id。

### 6.2 自动扫描

MyBatis `${}` 扫描：

```powershell
powershell -File C:\Users\wonderful\.codex\skills\java-security-review\scripts\scan-mybatis-dollar.ps1 -ScanDir backend
```

结果：

- 未发现 MyBatis `${}` SQL 注入风险。
- 仅命中已有 Spring `@Scheduled(fixedDelayString = "${...}")` 配置占位符，非 SQL 拼接。

硬编码密钥扫描：

- skill 自带 `find-hardcoded-secrets.ps1` 因脚本文件自身字符串编码/引号错误无法执行。
- 对本切片改动文件执行 targeted scan：未命中 `sk-`、`apiKey`、`secret`、`password` 等敏感字符串。

### 6.3 Java 安全审查报告

**被审查文件**：

- `backend/src/main/java/com/learningos/assessment/dto/GradingEvaluationRequest.java`
- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java`
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
- `backend/src/test/java/com/learningos/assessment/application/GradingEvaluationServiceTest.java`

**审查时间**：2026-06-08

#### 高危问题

未发现。

#### 中危问题

未发现。

#### 低危问题

未发现。

#### 无问题项

- SQL 注入：通过。本切片无原生 SQL 拼接；course/KP 查询使用 Spring Data repository。
- 越权访问：通过。Service 层执行 role gate、course gate、sample course consistency。
- 对象枚举：通过。student 优先拒绝；teacher missing/foreign course 同类 `FORBIDDEN`；sample KP 归属失败统一 `VALIDATION_ERROR`。
- 敏感信息泄露：通过。错误响应不返回 `data`，不暴露 foreign course id、teacher id、sample id 或 offending KP。
- 硬编码密钥：通过。本切片改动文件 targeted scan 未命中密钥。
- SSRF、路径遍历、不安全反序列化：不适用。本切片无 URL、文件路径或多态反序列化输入。

## 7. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只委托 service；权限和校验在 `GradingEvaluationService`。 |
| Frontend rules | PASS | 未修改 frontend，未引入前端 LLM/API key。 |
| Agent / RAG rules | PASS | 未修改 Agent/RAG/model 调用链。 |
| Security | PASS | 权限在后端代码执行；未新增依赖；未写入秘密。 |
| API / Database | PASS | API request contract 已在 SPEC 更新；无 DB schema/migration 改动。 |

## 8. 验证限制

- 当前身份仍基于过渡 `X-User-Id` 字符串模型，不是正式 JWT/RBAC。
- grading evaluation 仍是请求内离线样本计算；未接入 evaluation set runner。
- sample `knowledgePointId` 空白仍按既有 `UNKNOWN` 分组，不作为 course consistency 校验输入。
