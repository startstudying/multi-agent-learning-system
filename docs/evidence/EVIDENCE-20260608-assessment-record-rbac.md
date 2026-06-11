# EVIDENCE - P3-4-E Assessment Record RBAC Matrix

## 1. 追溯

- PRD：`docs/product/PRD-20260608-assessment-record-rbac.md`
- REQ：`docs/requirements/REQ-20260608-assessment-record-rbac.md`
- SPEC：`docs/specs/SPEC-20260608-assessment-record-rbac.md`
- PLAN：`docs/plans/PLAN-20260608-assessment-record-rbac.md`
- TASK：`docs/tasks/TASK-20260608-assessment-record-rbac.md`
- Context Pack：`docs/context/CONTEXT-20260608-assessment-record-rbac.md`
- 日期：2026-06-08

## 2. 实现内容

本切片新增 assessment record 最小只读详情权限矩阵：

- `GET /api/assessment/answers/{answerId}`
  - student 只能读取自己的 answer。
  - teacher 只能读取自己课程 active enrollment learner 的课程相关 answer。
  - admin 可读取任意已存在 answer；missing 返回 `NOT_FOUND`。
  - 非 admin missing / foreign 返回同形 `FORBIDDEN` 且无 `data`。
- `GET /api/assessment/wrong-questions/{wrongQuestionId}`
  - 复用 assessment record 授权语义。
  - wrong question 优先使用已持久化 `knowledgePointId` 推导课程。
- 响应使用白名单 DTO，不返回 `requestId`、`requestHash`、`responseJson`、`payloadJson`。

未新增依赖、未新增 migration、未修改 frontend。

## 3. 变更文件

| 文件 | 操作 | 摘要 |
|---|---|---|
| `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java` | 修改 | 新增 answer detail 和 wrong-question detail 两个 GET 端点，Controller 只传当前用户与 path variable。 |
| `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java` | 修改 | 新增详情查询和对象级授权辅助；teacher 通过 `questionId/knowledgePointId -> courseId -> CourseAccessService + active enrollment` 判断。 |
| `backend/src/main/java/com/learningos/assessment/dto/AssessmentRecordDetailResponse.java` | 新增 | answer 详情白名单 DTO。 |
| `backend/src/main/java/com/learningos/assessment/dto/WrongQuestionDetailResponse.java` | 新增 | wrong question 详情白名单 DTO。 |
| `backend/src/main/java/com/learningos/assessment/repository/GradingResultRepository.java` | 修改 | 增加按 answerId 查询最新 grading result 的方法。 |
| `backend/src/main/java/com/learningos/assessment/repository/WrongQuestionRepository.java` | 修改 | 增加按 answerId 查询最新 wrong question 的方法。 |
| `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java` | 修改 | 覆盖 student/teacher/admin answer detail 矩阵、missing/foreign 防枚举、wrong-question detail 授权复用和敏感字段不暴露。 |
| `backend/src/test/java/com/learningos/assessment/application/AssessmentServiceTest.java` | 修改 | 补齐新增 service 依赖的测试构造参数。 |

## 4. 测试结果

### 4.1 聚焦测试

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest test
```

结果：

- BUILD SUCCESS
- Tests run: 13
- Failures: 0
- Errors: 0
- Skipped: 0
- Finished at: 2026-06-08T16:45:18+08:00

### 4.2 相邻回归

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=AssessmentControllerTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,LearningWorkflowControllerTest test
```

结果：

- BUILD SUCCESS
- Tests run: 53
- Failures: 0
- Errors: 0
- Skipped: 0
- Finished at: 2026-06-08T16:46:19+08:00

### 4.3 后端全量测试

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

- BUILD SUCCESS
- Tests run: 316
- Failures: 0
- Errors: 0
- Skipped: 1
- Finished at: 2026-06-08T16:48:14+08:00

## 5. 安全审查

### 5.1 自动扫描

```powershell
powershell -File C:\Users\wonderful\.codex\skills\java-security-review\scripts\scan-mybatis-dollar.ps1 -ScanDir backend
```

结果：

- 未发现 MyBatis Mapper `${}` 拼接 SQL。
- 扫描输出的 `${learning-os.rag...}` 位于 `@Scheduled(fixedDelayString = "...")` 配置占位符，不是 SQL 拼接风险。

```powershell
powershell -File C:\Users\wonderful\.codex\skills\java-security-review\scripts\find-hardcoded-secrets.ps1 -ScanDir backend
```

结果：

- 脚本自身编码异常导致 PowerShell 解析失败，未作为通过证据。

兜底扫描：

```powershell
rg -n "(?i)(password\s*=|secret\s*=|api[_-]?key\s*=|token\s*=|sk-[A-Za-z0-9])" backend\src\main backend\src\test backend\src\main\resources backend\src\test\resources
```

结果：

- 命中项均为既有测试夹具中的假 secret / redaction 断言，或 `application.yml` 的环境变量占位符。
- 本切片未新增生产硬编码密钥。

### 5.2 Java 安全审查报告

**被审查文件**：

- `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java`
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/main/java/com/learningos/assessment/dto/AssessmentRecordDetailResponse.java`
- `backend/src/main/java/com/learningos/assessment/dto/WrongQuestionDetailResponse.java`

**审查时间**：2026-06-08

#### 高危问题

未发现。

#### 中危问题

未发现。

#### 低危问题

未发现。

#### 无问题项

- SQL 注入：通过。新增查询使用 Spring Data JPA repository 方法，无原生 SQL 拼接。
- 越权访问：通过。详情返回前在 Service 层执行 student owner、teacher own-course + active enrollment、admin 全局矩阵。
- 敏感信息泄露：通过。详情响应使用 DTO 白名单，测试断言不返回 `requestId`、`requestHash`、`responseJson`、`payloadJson`。
- 硬编码密钥：通过。本切片未新增密钥；兜底扫描未发现新增生产密钥。
- 身份认证与会话安全：本切片仍使用项目现有过渡 `X-User-Id`，真实 JWT/RBAC 保持后续 P3-4 open。
- SSRF：不适用。本切片无外部 URL 请求。
- 弱加密与随机数：不适用。本切片未新增加密/随机安全逻辑。
- 不安全反序列化：通过。未新增外部不可信多态反序列化。
- 路径遍历：不适用。本切片无文件路径输入。
- CSRF：不适用。本切片未修改 Spring Security / session 行为。

#### 总结

| 级别 | 数量 |
|---|---:|
| 高危 | 0 |
| 中危 | 0 |
| 低危 | 0 |

结论：通过。真实 JWT/RBAC 与列表接口权限仍为后续工作。

## 6. 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 只处理 HTTP/current user，授权在 `AssessmentService`。 |
| Frontend rules | PASS | 未修改 frontend，未引入前端 LLM/API key。 |
| Agent / RAG rules | PASS | 未修改 Agent/RAG 执行链路。 |
| Security | PASS | 对象权限在后端代码执行；非 admin missing/foreign 收敛为 `FORBIDDEN`；未新增依赖；未写入秘密。 |
| API / Database | PASS | SPEC 已记录新增详情 API；未修改 schema/migration。 |

## 7. 验证限制

- 当前身份仍基于过渡 `X-User-Id` 字符串模型，不是正式 JWT/RBAC。
- 本切片只实现详情端点，不实现 answer / wrong-question list 与分页。
- teacher 授权通过 `questionId -> knowledgePointId -> courseId` 推导；后续若 assessment 记录冗余 `courseId`，可将授权查询下沉到更直接的 course-scope 查询。
- `POST /api/assessment/grading-evaluations` 仍只有 teacher/admin gate，尚无 course-scoped grading evaluation。
