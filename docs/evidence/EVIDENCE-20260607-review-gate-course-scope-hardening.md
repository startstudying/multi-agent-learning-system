# EVIDENCE - Review Gate 课程范围收口

## 1. 变更概述

本切片将资源审核权限从临时 `teacher/admin` 边界收口为：

- `admin`：可查看和处理全部 `ResourceReview`。
- `teacher`：仅可查看和处理 `ResourceGenerationTask.goalId -> Course.id -> Course.teacherId == 当前用户` 的审核记录。
- 其他用户：拒绝访问，且响应不返回 review/resource/task 明细。

未新增依赖、未修改 API 合同、未修改数据库 schema。

## 2. 关键实现证据

- `ReviewGovernanceService` 注入 `CourseRepository`，在 list 和 decision 路径中统一使用课程归属判断。
- `listResourceReviews(...)` 先执行 reviewer 门禁，再按课程范围过滤结果。
- `decide(...)` 在持久化审核决策前校验当前用户是否可审核该任务。
- `CourseRepository.existsByTeacherId(...)` 用于判断临时 teacher 身份是否至少拥有课程。
- `ResourceReviewControllerTest` 覆盖教师仅看自己课程、教师不能处理外部课程、管理员全局、学生拒绝。
- 代码审查发现的 reviewId 存在性 oracle 已补回归：教师对缺失 reviewId 也返回 `FORBIDDEN`，不再通过 `404/403` 区分 missing 和 foreign review。
- `ResourceGenerationControllerTest` 补齐 `goalId` 对应的课程归属测试夹具，避免新权限规则把既有 no-source 和 release 回归误判为无课程权限。

## 3. 测试命令

### 3.1 reviewId oracle RED/GREEN

```powershell
cd backend
mvn --% -Dtest=ResourceReviewControllerTest#teacherCannotDistinguishMissingReviewFromForbiddenReview test
```

RED 结果：

- 新增测试期望 `403 FORBIDDEN`
- 修复前实际返回 `404 NOT_FOUND`
- 失败原因：教师可通过缺失 reviewId 与外部 reviewId 的响应差异推断 reviewId 是否存在

GREEN 结果：

- Tests run: 1
- Failures: 0
- Errors: 0
- Skipped: 0
- BUILD SUCCESS

### 3.2 Review Gate 聚焦回归

```powershell
cd backend
mvn --% -Dtest=ReviewGovernanceServiceTest,ResourceReviewControllerTest,ResourceGenerationControllerTest test
```

结果：

- Tests run: 31
- Failures: 0
- Errors: 0
- Skipped: 0
- BUILD SUCCESS

### 3.3 相邻模块宽回归

```powershell
cd backend
mvn --% -Dtest=ResourceReviewControllerTest,ReviewGovernanceServiceTest,ResourceGenerationControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,AssessmentControllerTest,AgentTraceControllerTest,RagQueryServiceTest,DocumentControllerTest test
```

结果：

- Tests run: 82
- Failures: 0
- Errors: 0
- Skipped: 0
- BUILD SUCCESS

### 3.4 后端全量测试

```powershell
cd backend
mvn test
```

结果：

- Tests run: 238
- Failures: 0
- Errors: 0
- Skipped: 1
- BUILD SUCCESS

## 4. 安全与架构漂移检查

### MyBatis `${}` 扫描

```powershell
powershell -ExecutionPolicy Bypass -File C:/Users/wonderful/.codex/skills/java-security-review/scripts/scan-mybatis-dollar.ps1 -ScanDir backend
```

结果只命中 Spring 配置占位符：

- `IndexTaskRecoveryScheduler.java` 的 `@Scheduled(fixedDelayString = "${learning-os.rag.index-recovery.fixed-delay:5m}")`
- `IndexTaskWorkerScheduler.java` 的 `@Scheduled(fixedDelayString = "${learning-os.rag.index-worker.fixed-delay:5s}")`

未发现 MyBatis Mapper `${}` SQL 拼接风险。

### 硬编码密钥扫描

官方脚本执行失败：

```text
find-hardcoded-secrets.ps1: The string is missing the terminator: ".
```

补充执行 `rg` 关键字扫描，命中项为既有配置占位符、测试假密钥和脱敏断言；本切片未新增密钥、凭据或敏感配置。

### 架构漂移结论

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | Controller 仍只委托 Service；权限判断在 `ReviewGovernanceService` |
| Frontend rules | PASS | 未修改 frontend |
| Agent / RAG rules | PASS | 未改变 Agent/RAG 执行链路；生成资源仍受 Review Gate 控制 |
| Security | PASS | 权限在后端代码执行；未新增依赖；未写入秘密 |
| API / Database | PASS | 未修改 API 请求/响应合同；未新增迁移 |

## 5. 验证限制

- 当前身份仍基于 `X-User-Id` / 临时字符串用户，不是完整 JWT/RBAC。
- 本切片只覆盖 Review Gate 的课程归属收口，不覆盖 RAG KB、答题记录、学习资源详情等完整权限矩阵。
- `listResourceReviews(...)` 当前仍按 review 列表做服务层过滤，代码审查指出存在 N+1 查询和加载无关 review/task/course 的性能风险；本切片保留该实现，后续生产化应改为仓储层 scoped query 或批量加载。
- 硬编码密钥官方脚本本身存在解析错误，已用 `rg` 做补充扫描并记录限制。
