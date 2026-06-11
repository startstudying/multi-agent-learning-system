# RUN-20260608 P3 下一步 Security & Quality / RBAC 子专家报告

## 0. 审查范围与结论

本报告只读审查 `docs/planning/backend-architecture-todolist.md` 中 P3-4 剩余项，聚焦真实 JWT/RBAC、broader class/course matrix、教师端/学生端剩余授权与完整渗透测试。未修改生产代码。

结论：P3-4-A..H 已经把多个高风险 IDOR 面切成可验证的小闭环，但当前系统仍不是生产级认证授权。最高风险仍是 `X-User-Id` 可伪造身份、字符串角色推断分散、以及完整教师/学生/管理员权限矩阵缺少统一渗透测试入口。下一步不应直接大改所有业务服务；建议先做“认证上下文替换 + 兼容测试矩阵”的最小安全切片。

## 1. 当前临时 X-User-Id / CurrentUserService / 权限服务现状

### 1.1 身份入口

- `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java`
  - 读取请求头 `X-User-Id`。
  - 缺失时默认 `dev_user`。
  - 写入 `UserContext(userId, userId, List.of("USER"))`。
  - 文件注释明确它是 Phase-1 local authentication substitute，后续应由 Sa-Token 或 JWT 替换。
- `backend/src/main/java/com/learningos/common/auth/UserContextHolder.java`
  - 使用 `ThreadLocal<UserContext>` 保存当前用户。
  - 请求结束后由过滤器清理。
- `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java`
  - `currentUserId()` 从 `UserContextHolder` 取 userId，缺失时回退 `dev_user`。
  - `isAdmin()` 仅判断 `currentUserId == "admin"`。
  - `isTeacherUser()` 仅判断 `teacher` 或 `teacher_` 前缀。

安全判断：当前任意客户端都可伪造 `X-User-Id: admin` 或 `X-User-Id: teacher_xxx`。这适合本地开发和 TDD，但不能作为生产认证。`UserContext.roles` 当前基本没有参与授权，角色真实来源也未被签名 token 或服务端会话保护。

### 1.2 当前集中权限服务

- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`
  - `requireCourseRead(currentUserId, courseId)`：admin 全局读；teacher 只能读 `Course.teacherId == currentUserId`；student 通过 `course_enrollment.status == ACTIVE` 读。
  - `requireCourseManage(currentUserId, course)`：admin 全局写；teacher 只能写自己课程；student 拒绝。
  - `requireLearnerEnrolledForExistingCourse(...)`：course 存在时要求 learner active enrollment，admin bypass。
  - `listCoursesForUser(...)`：admin 全部、teacher 自己课程、student active enrollment 课程。
  - 非 admin missing/foreign course 收敛为 `FORBIDDEN`，admin missing 保留 `NOT_FOUND`。

安全判断：`CourseAccessService` 是当前最适合作为后续 RBAC/JWT 落点的服务，但它仍以裸字符串 `currentUserId` 和本地角色推断为输入。下一步应优先替换“身份可信度”，而不是重写业务授权矩阵。

### 1.3 权限判断分散点

只读扫描显示，以下服务仍有局部 `isAdmin/isTeacherUser` 或硬编码角色判断：

- `AnalyticsController` / `AnalyticsService`
- `AssessmentService` / `GradingEvaluationService`
- `KnowledgeCatalogService`
- `ReviewGovernanceService`
- `ResourceGenerationService`
- `EvaluationSetService` / `EvaluationRunService`
- `AgentTraceGovernanceService`
- `LearningWorkflowService`

风险：即使 JWT 落地，如果角色仍通过 userId 字符串推断，攻击者只要拿到普通 token 但 userId 命名碰撞为 `teacher_*` 或迁移时映射错误，就可能形成越权。应引入 `AuthenticatedUser` 之类的上下文对象，显式包含 `userId`、`roles`、`tenant/schoolId` 可选字段，并由 `CurrentUserService` 提供 `currentUser()` 给服务层。

### 1.4 依赖与秘密扫描

- `backend/pom.xml` 当前未包含 `spring-boot-starter-security`、JWT 或 OAuth2 resource server 依赖。
- 执行 `mvn -q dependency:tree "-Dincludes=org.springframework.security,io.jsonwebtoken,com.auth0,org.springframework.boot:spring-boot-starter-security"` 无相关依赖输出。
- targeted secrets scan 命中：
  - `backend/docker-compose.yml` 的本地默认 `MYSQL_PASSWORD` / `MYSQL_ROOT_PASSWORD` / `MINIO_ROOT_PASSWORD`。
  - `application.yml` 的空环境变量占位 `DB_PASSWORD` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY`。
  - 多个测试中的 `apiKey=sk-test`、`password=secret` 脱敏验证样例。
- 未发现本次范围内真实生产密钥；本地默认值仍需在生产部署中由环境变量或 secret manager 覆盖。

## 2. 推荐下一最小可验收安全切片

建议下一切片命名：`P3-4-I 真实认证上下文与 RBAC 兼容层`。

### 2.1 目标

在不重写既有业务授权的前提下，把“可信身份来源”从可伪造 `X-User-Id` 迁移到后端验证过的认证上下文，并保持 P3-4-C..H 的授权测试矩阵不回退。

### 2.2 最小范围

1. 新增真实认证依赖审查文档。
   - 如果选择 Spring Security OAuth2 Resource Server：审查 `spring-boot-starter-security` 与 `spring-boot-starter-oauth2-resource-server`。
   - 如果选择轻量 JWT 库：审查库的维护状态、算法支持、JWK/密钥轮换支持。
2. 新增 `AuthenticatedUser`。
   - 字段：`userId`、`roles`、可选 `displayName`。
   - 角色枚举建议：`ADMIN`、`TEACHER`、`STUDENT`。
3. 改造 `CurrentUserService`，保留兼容方法。
   - 新增 `currentUser()`。
   - `currentUserId()` 从 `AuthenticatedUser.userId` 读取。
   - `isAdmin()` / `isTeacherUser()` 优先读 roles，临时兼容旧 userId 规则仅在 dev profile。
4. 引入安全过滤器。
   - 生产 profile：只接受 `Authorization: Bearer <jwt>` 或后续统一身份方案。
   - dev/test profile：保留 `DevAuthFilter`，但应明确只在非生产 profile 生效。
5. 建立 RBAC 兼容测试。
   - 证明没有 token 的生产请求被拒绝。
   - 证明普通 student token 不能通过伪造 `X-User-Id: admin` 越权。
   - 证明 admin/teacher/student token 下，P3-4-C..H 的既有行为保持一致。

### 2.3 可验收标准

- `CurrentUserService` 不再在生产 profile 从 `X-User-Id` 建立身份。
- `X-User-Id` 在生产 profile 不影响授权结果。
- 所有现有 P3-4-C..H 关键授权测试在 dev/test 兼容模式下继续通过。
- 新增至少一组生产 profile 或 filter 单元测试覆盖：
  - missing token -> 401 或统一认证错误。
  - invalid token -> 401。
  - valid student token + `X-User-Id: admin` -> 仍按 student 授权。
  - valid teacher token 只能访问 own course。
  - valid admin token 保留全局能力。

## 3. 需避免的大范围重构

- 不要在同一切片内同时重写所有 Controller 为 `@PreAuthorize`。当前项目授权规则依赖对象归属链，Service 层检查更贴合现有架构。
- 不要把 `CourseAccessService` 拆散到各 Controller。下一步应增强身份输入，不应削弱集中授权点。
- 不要一次性重建 user/class/course/tenant 全量模型。`course_enrollment` 已经足够支撑下一轮验证，class/school/tenant 可后续独立切片。
- 不要让前端直接决定角色或传角色字段。角色只能来自后端验证后的 token/session。
- 不要把 missing/foreign 响应语义改回普通 `404`。P3-4-C..H 已经建立非 admin 防枚举语义，应保持。
- 不要引入新 JWT/安全依赖而不写 `docs/security/` 依赖审查。
- 不要在认证切片中顺手改 RAG、Agent、Assessment 业务响应 DTO；这会扩大验收面。

## 4. 允许修改文件建议

下一切片建议 Context Pack 允许修改：

- `backend/pom.xml`
- `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java`
- `backend/src/main/java/com/learningos/common/auth/UserContext.java`
- `backend/src/main/java/com/learningos/common/auth/UserContextHolder.java`
- `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java`
- 新增：`backend/src/main/java/com/learningos/common/auth/AuthenticatedUser.java`
- 新增：`backend/src/main/java/com/learningos/common/auth/JwtAuthFilter.java` 或 Spring Security 配置等价文件
- 新增：`backend/src/main/java/com/learningos/config/SecurityConfig.java`
- `backend/src/test/java/com/learningos/common/auth/*`
- 重点回归测试：
  - `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`
  - `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java`
  - `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java`
  - `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java`
  - `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java`
  - `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`
- 文档：
  - `docs/security/DEPENDENCY-20260608-jwt-rbac.md`
  - `docs/specs/SPEC-20260608-real-auth-rbac-context.md`
  - `docs/plans/PLAN-20260608-real-auth-rbac-context.md`
  - `docs/tasks/TASK-20260608-real-auth-rbac-context.md`
  - `docs/context/CONTEXT-20260608-real-auth-rbac-context.md`
  - `docs/evidence/EVIDENCE-20260608-real-auth-rbac-context.md`
  - `docs/acceptance/ACCEPT-20260608-real-auth-rbac-context.md`

不建议本切片修改：

- `AssessmentService` 的业务查询矩阵，除非只为适配 `AuthenticatedUser` 签名。
- `ResourceGenerationService`、`DocumentService`、`ReviewGovernanceService` 的业务授权逻辑。
- 数据库 schema，除非明确选择新增 `user_role` 等真实 RBAC 表；该方向应另开切片。
- 前端登录页、刷新 token、用户管理后台。

## 5. 测试矩阵

### 5.1 认证层矩阵

| 场景 | 期望 |
|---|---|
| 生产 profile 无 `Authorization` | 401 或统一认证失败 |
| 生产 profile invalid token | 401 |
| 生产 profile valid student token + `X-User-Id: admin` | 按 student 授权，不能访问 admin/teacher 数据 |
| dev/test profile 仅 `X-User-Id: alice` | 兼容现有测试 |
| token roles = `ADMIN` | `CurrentUserService.isAdmin()` 为 true |
| token roles = `TEACHER` | `isTeacherUser()` 为 true，但不能因 userId 前缀伪造 |
| token roles = `STUDENT` | 只能 owner/enrollment scoped |

### 5.2 P3-4-C..H 回归矩阵

| 模块 | admin | teacher own course | teacher foreign/missing | enrolled student | unenrolled/foreign student |
|---|---|---|---|---|---|
| `GET /api/courses` | 全量 | own only | 不含 foreign | enrolled only | 不含 foreign |
| `GET /api/courses/{id}` | existing 200 / missing `NOT_FOUND` | own 200 | `FORBIDDEN` | enrolled 200 | `FORBIDDEN` |
| `GET /api/courses/{id}/knowledge-graph` | 同课程详情 | own 200 | `FORBIDDEN` | enrolled 200 | `FORBIDDEN` |
| path/resource create with course goal | 可管理/兼容 | 视业务需求，不建议教师伪造 learner | `FORBIDDEN` | enrolled 200 | `FORBIDDEN` 且不落库 |
| answer detail/list | 全局 | own-course active learner only | `FORBIDDEN` 或空 page | owner only | `FORBIDDEN` |
| wrong-question detail/list | 全局 | own-course active learner only | `FORBIDDEN` 或空 page | owner only | `FORBIDDEN` |
| grading evaluation | existing course 200 / missing `NOT_FOUND` | own course 200 | `FORBIDDEN` | `FORBIDDEN` | `FORBIDDEN` |
| RAG document upload metadata | existing course 200 / missing `NOT_FOUND` | own course 200 | `FORBIDDEN` 且不落库 | `FORBIDDEN` | `FORBIDDEN` |

### 5.3 渗透测试断言

- 非 admin missing 与 foreign 对象响应同形：`FORBIDDEN` 且无 `data`。
- 越权响应 body 不包含 foreign object id、courseId、teacherId、learnerId、taskId、traceId、title、markdownContent。
- 被拒绝的写请求不产生副作用：
  - 不创建 `learning_path`。
  - 不创建 `resource_generation_task` / resource / review。
  - 不创建 `kb_document` / `kb_index_task`。
  - 不写入 answer/list 查询副作用。
- list 接口只返回 scoped rows，不通过加载全量后泄露总数。
- SSE/stream 接口不能因浏览器无法发送自定义 header 而回退到 `dev_user` 读取私有数据；生产应使用 cookie/session 或 signed stream token。

## 6. 与已有 P3-4-A..H 的关系

- P3-4-A：最小高风险权限收口已经覆盖 profile owner、learning path owner、analytics overview admin-only、health redaction、strict RAG `kbIds`。下一切片应保护这些规则不被 `X-User-Id` 伪造绕过。
- P3-4-B：对象详情防枚举已覆盖 path/task/trace/document/index-task 等 detail。下一切片不应改变非 admin missing/foreign 同形 `FORBIDDEN`。
- P3-4-C：course read/graph 与 grading evaluation 初步门禁已完成。下一切片应让 `admin/teacher/student` 来自可信 roles，而不是 userId 字符串。
- P3-4-D：`course_enrollment` 与 `CourseAccessService` 已落地，是 broader class/course matrix 的当前基础。下一步应复用它，不要重建。
- P3-4-E：assessment answer/wrong-question detail RBAC 已完成。下一切片只做认证上下文替换和回归，不重写 detail 矩阵。
- P3-4-F：assessment answer/wrong-question list/pagination RBAC 已完成。下一切片应加入 token roles 回归和防枚举断言。
- P3-4-G：grading evaluation course scope 已完成。下一切片应证明 student token 无法探测 course/sample，teacher token 仍只限 own course。
- P3-4-H：RAG document course/chapter metadata scope 已完成。下一切片应证明 student token + spoofed `X-User-Id` 不能绕过 course metadata 写权限。

## 7. OWASP 对照

- A01 Broken Access Control：当前最高风险。P3-4-C..H 已降低 IDOR，但真实认证未落地仍可被 header 伪造绕过。
- A02 Cryptographic Failures：尚未使用 JWT 签名/验证；下一切片必须明确算法、密钥来源、过期时间和密钥轮换。
- A03 Injection：本报告未发现本范围新增 SQL/命令注入证据；JPA Repository 为主。
- A04 Insecure Design：角色由 userId 命名推断属于设计债，应以 roles claim/server-side role 替换。
- A05 Security Misconfiguration：dev auth filter 作为组件默认启用，生产 profile 必须禁用。
- A06 Vulnerable and Outdated Components：当前未引入安全/JWT 依赖；新增前需 dependency review。
- A07 Identification and Authentication Failures：真实认证缺失，是下一切片核心。
- A08 Software and Data Integrity Failures：token 验签/JWK 来源需固定并验证，避免信任未验证 claim。
- A09 Security Logging and Monitoring Failures：已有 structured logging；认证失败应增加安全事件但不得记录 token。
- A10 SSRF：本次范围未涉及外部 URL fetch 新入口。

## 8. 风险等级

整体风险等级：HIGH。

理由：业务对象级授权已有明显改善，但认证根仍是可伪造 header。只要部署到不可信网络，攻击者可以通过 `X-User-Id: admin` 或 `teacher_*` 直接尝试绕过多个已经完成的服务层矩阵。下一步应优先做真实认证上下文，而不是继续扩大业务矩阵。
