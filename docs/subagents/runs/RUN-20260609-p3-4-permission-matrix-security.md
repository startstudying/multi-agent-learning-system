# P3-4-K 权限渗透测试矩阵补齐安全报告

## Scope

只读分析范围：

- `AGENTS.md`
- `docs/planning/backend-architecture-todolist.md`
- `docs/memory/BACKEND_MEMORY.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `backend/src/test/java` 下权限相关测试类

任务类型：Security & Quality / OWASP A01 Broken Access Control 测试矩阵设计。

选用技能：

- `security-review`：权限、安全、敏感数据基线审查。
- `object-scope-authorization`：对象级权限、IDOR、防枚举、teacher/student/admin 矩阵。
- `auth-context-boundary`：Bearer JWT、`X-User-Id` fallback、header spoofing、roles-first RBAC。

GitHub 研究：不需要。当前任务是基于现有项目规则和测试补齐矩阵，不引入新安全框架或依赖。

依赖结论：本切片建议只新增/调整测试，不新增依赖，不触发 dependency review。

模型说明：本专家为第一个 P3-4-K 安全专家，按用户要求使用 `gpt-5.5`。

## 1. 现有测试已覆盖的 RBAC 场景

### Course / Knowledge Graph

覆盖测试类：`CourseKnowledgeControllerTest`

已覆盖：

- 学生不能创建课程。
- 教师不能为其他教师创建课程。
- 外部教师不能管理他人课程图谱。
- 管理员可以管理任意课程图谱。
- `/api/courses` 列表按 admin / teacher / student scope 收敛。
- 学生读取课程详情和知识图谱必须有 active enrollment。
- 非 admin 访问 foreign / missing course detail 或 graph 统一返回 `FORBIDDEN`。
- admin 访问 missing course detail / graph 返回 `NOT_FOUND`。

### Assessment Answer / Wrong Question / Grading Evaluation

覆盖测试类：`AssessmentControllerTest`

已覆盖：

- 答题提交禁止 `X-User-Id` 与 `learnerId` 不一致。
- answer detail：student owner-only；teacher own-course + active enrolled learner；admin global。
- 非 admin foreign/missing answer 和 wrong-question detail 统一 `FORBIDDEN` 且不包含对象 ID。
- answer list / wrong-question list：student owner-only；teacher 必须提供 `courseId` 且仅 own-course active learner；admin global/filter。
- 分页边界已覆盖，summary DTO 不暴露 answer text、request snapshot、payload 等敏感字段。
- grading evaluation：teacher/admin 绑定 `courseId`，teacher own-course，student 先拒绝，admin missing 返回 `NOT_FOUND`。

### RAG Document / Index Task

覆盖测试类：`DocumentControllerTest`

已覆盖：

- 文档上传支持 course/chapter metadata，并验证成功持久化。
- teacher 不能用他人 courseId 或 missing courseId 上传课程文档，且响应不回显 ID。
- student 即使有 KB，也不能 spoof course metadata。
- admin 上传 missing course 返回 `NOT_FOUND`。
- `chapterId` 不能脱离 `courseId` 使用。
- foreign/missing chapter 返回泛化 `VALIDATION_ERROR`，不回显 chapterId。
- document detail、reindex、index-task detail 对 foreign/missing 非 owner 返回 `FORBIDDEN`，且不泄露对象 ID。
- public/private KB 文档读写边界有基础覆盖。

### Resource Generation / Review Gate / Agent Trace

覆盖测试类：`ResourceGenerationControllerTest`、`ResourceReviewControllerTest`

已覆盖：

- resource generation 创建禁止当前用户替他人创建任务。
- foreign task / trace 访问返回 `FORBIDDEN`。
- task detail、trace detail 对非 admin missing/foreign 统一 `FORBIDDEN`，响应不包含 taskId/traceId。
- learner resources 在 Review Gate 通过前不可释放。
- course-bound resource generation 要求 active enrollment。
- Review Gate：student 不能 list/decision；admin global；teacher only own-course；teacher foreign/missing review decision 安全拒绝。

### Analytics / Ops

覆盖测试类：`AnalyticsControllerTest`

已覆盖：

- `/api/analytics/overview` admin-only。
- `/api/analytics/ops/alerts` admin-only。
- ops alerts 响应不泄露 raw question、`apiKey=sk-test`、JDBC secret 等敏感内容。
- student summary：student owner-only；teacher 必须 `courseId` + own-course active learner；admin global/course scoped。
- teacher class summary：teacher own-course；foreign teacher/student 拒绝；admin 可读。

### Auth Context / Header Spoofing

覆盖测试类：`DevAuthFilterTest`、`CurrentUserServiceTest`

已覆盖：

- dev/test 无 Bearer 时可用 `X-User-Id` fallback。
- valid Bearer token 覆盖 spoofed `X-User-Id`。
- invalid Bearer token 不 fallback 到 `X-User-Id`，返回 `UNAUTHORIZED`。
- production 缺少 Bearer 时即使带 `X-User-Id` 也返回 `UNAUTHORIZED`。
- production valid Bearer token 忽略 spoofed `X-User-Id`。
- `CurrentUserService` 优先用 roles 判断 admin/teacher。

## 2. 缺失但高价值的矩阵用例

### P0 - 建议纳入 P3-4-K 最小切片

1. **Bearer roles 贯穿真实 Controller 的集成矩阵**
   - Bearer `ADMIN` + spoofed `X-User-Id=alice` 可访问 admin-only overview。
   - Bearer `STUDENT` + spoofed `X-User-Id=admin` 不可访问 admin-only overview。
   - Bearer `TEACHER` + spoofed `X-User-Id=other_teacher` 只能访问 token subject 自有课程/审核数据。

2. **Course graph 写入的 student negative matrix**
   - enrolled student 不能创建章节、知识点、依赖关系。

3. **Course list 对 dropped / inactive enrollment 的负向断言**
   - student `/api/courses` 不包含 `DROPPED` / non-active enrollment course。

4. **Resource generation task detail 的 admin / teacher 矩阵**
   - admin 可读任意 existing generation task detail。
   - admin missing task 返回 `NOT_FOUND`。
   - teacher task detail 策略必须显式：own-course 允许或拒绝都要在 SPEC 中固化；foreign course 必须 `FORBIDDEN`。

5. **Agent trace cancel / mutation 权限矩阵**
   - owner 可 cancel 自己可取消任务。
   - foreign student 不能 cancel 他人 task。
   - non-admin missing/foreign cancel 不泄露 taskId。

6. **Analytics token-budget governance admin-only**
   - teacher/student 访问 `/api/analytics/token-budget/governance` 返回 `FORBIDDEN`。
   - 响应不包含其他用户 token/cost/model task 信息。

### P1 - 后续或同批扩展

- Teacher class summary missing vs foreign course 语义复核：当前可能存在 `NOT_FOUND` vs `FORBIDDEN` courseId oracle，应在后续安全修复切片处理，不建议混入“不改业务代码”的最小测试切片。
- RAG KB list / create 权限矩阵。
- RAG query mixed `kbIds` Controller 级复验。
- PromptVersion / Evaluation endpoints 权限矩阵。

## 3. 建议最小切片排序

1. `DevAuthFilterTest`：补 staging auth fallback。
2. `AnalyticsControllerTest`：补 Bearer roles + token-budget admin-only。
3. `CourseKnowledgeControllerTest`：补 enrolled student 不能写图谱、dropped enrollment 不进列表。
4. `AssessmentControllerTest`：补 Bearer roles + spoofed header 的 answer detail。
5. `DocumentControllerTest`：补 Bearer roles + public KB course metadata spoof。
6. `ResourceGenerationControllerTest`：补 admin detail、teacher detail policy、foreign cancel。
7. `ResourceReviewControllerTest`：补 Bearer teacher role + spoofed header review decision。

## 4. 本切片明确不做

- 不迁移 OAuth2 Resource Server。
- 不接入 JWK / JWKS。
- 不引入 Spring Security 配置重构。
- 不替换当前 `DevAuthFilter` / HS256 JWT 过渡兼容层。
- 不新增认证/授权依赖。
- 不设计正式 class domain / 班级实体 / 班级成员模型。
- 不改数据库 schema。
- 不调整业务权限策略，只为现有策略补足渗透测试。
- 不覆盖前端鉴权显示逻辑。

## 5. 风险优先级

### HIGH

1. Controller 层缺少 Bearer roles 集成矩阵。
2. Resource generation task detail / cancel 权限矩阵不足。
3. Course graph student 写入负向矩阵不足。

### MEDIUM

1. Token budget governance admin-only 缺少负向测试。
2. Teacher class summary missing vs foreign 语义可能形成 courseId existence oracle。

### LOW

1. staging 环境 auth fallback 缺少显式单测。

## 验收标准

- 不新增依赖。
- 不修改业务代码，除非新增测试揭示已确认必须修复的安全缺陷。
- 所有新增测试均围绕现有 P3-4 策略。
- 非 admin foreign/missing 响应不包含对象 ID、父资源 ID、traceId、resourceId、courseId。
- Bearer token 身份优先于 `X-User-Id` 的规则在业务 Controller 中被验证。
