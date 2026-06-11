# P3-4 权限与安全加固并行专家分析（Security & Quality）

审查时间：2026-06-07  
审查范围：`backend` 当前仓库权威状态，围绕 `docs/planning/backend-architecture-todolist.md` 未完成的 P3-4 权限与安全加固；重点覆盖 `CurrentUserService`、Controller、Service 权限检查、RAG KB/course/learning path/resource/answer record、teacher/student/admin RBAC 测试。  
执行约束：只做安全分析，不修改代码。  

## 1. 已有权限证据

### 1.1 当前身份上下文仍是开发期替代方案

- `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java:24` 定义默认用户 `dev_user`。
- `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java:29-34` 直接信任请求头 `X-User-Id`，并把 `userId` 同时作为用户名写入 `UserContextHolder`，角色固定为 `USER`。
- `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java:10-13` 若上下文为空回退到 `dev_user`。
- `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java:16-22` 通过字符串判断 `admin`、`teacher` 或 `teacher_` 前缀，而非认证令牌中的可信角色。

结论：现有权限检查已经有“当前用户”入口，但仍属于开发期身份模拟，不是生产级认证/RBAC。

### 1.2 RAG KB / 文档 / 检索权限已有基础

- `backend/src/main/java/com/learningos/rag/application/PermissionService.java:37-45` `listAccessibleKnowledgeBases` 只返回可读 KB。
- `backend/src/main/java/com/learningos/rag/application/PermissionService.java:66-82` `requireReadableKbIds` 对请求的 `kbIds` 做 strict 校验，混入不可读 KB 时抛 `FORBIDDEN`，不做静默过滤。
- `backend/src/main/java/com/learningos/rag/application/PermissionService.java:110-117` 读权限覆盖 owner、PUBLIC、OWNER/READ/WRITE 授权。
- `backend/src/main/java/com/learningos/rag/application/PermissionService.java:119-126` 写权限覆盖 owner、OWNER/WRITE 授权，不包含 PUBLIC。
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java:72` 上传文档前调用 `ensureCanWrite`。
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java:136` 按 `documentId` 读取文档前回查文档所属 `kbId` 并调用 `ensureCanRead`。
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java:149` 重建索引前调用 `ensureCanWrite`。
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java:159` 按 `taskId` 查询索引任务前调用 `ensureCanRead`。
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java:160-167` 普通 RAG 查询先做内容安全检查，再调用 `requireReadableKbIds`；空或不可读 KB 返回 `FORBIDDEN`。
- `backend/src/main/java/com/learningos/rag/application/RagQueryService.java:270-286` 幂等 replay 路径同样先调用 `requireReadableKbIds`，避免用旧 `requestId` 绕过 KB 权限。

测试证据：

- `backend/src/test/java/com/learningos/rag/application/RagQueryServiceTest.java:376-387` 覆盖 mixed allowed/forbidden KB strict 拒绝。
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java:351-364` 覆盖他人读取/重建私有文档返回 `FORBIDDEN`。
- `backend/src/test/java/com/learningos/rag/api/DocumentControllerTest.java:397-410` 覆盖他人读取 index task detail 返回 `FORBIDDEN`，且不泄露 owner 文档名。

### 1.3 Course / Knowledge Catalog 写路径已有教师课程范围控制

- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java:57-58` 创建课程时解析 teacher owner。
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java:169-179` 非 admin 只能作为 teacher 用户创建自己的课程，不能指定其他 teacher。
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java:79-81` 创建 chapter 前要求课程 teacher 或 admin。
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java:90-92` 创建 knowledge point 前要求课程 teacher 或 admin。
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java:103-116` 创建 dependency 前校验同课程，再要求课程 teacher 或 admin。
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java:190-195` `requireCourseTeacherOrAdmin` 是写路径核心授权点。

测试证据：

- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java:207-219` 覆盖 teacher 不能替其他 teacher 创建课程。
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java:239-285` 覆盖 foreign teacher 不能管理其他课程 graph。
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java:290-328` 覆盖 admin 可管理任意课程 graph。

### 1.4 Learning profile / learning path owner 检查已有最小闭环

- `backend/src/main/java/com/learningos/learning/api/ProfileController.java:28-33` profile extract 要求 `currentUserId == request.learnerId`。
- `backend/src/main/java/com/learningos/learning/api/LearningPathController.java:30-35` 创建 path 要求 `currentUserId == request.learnerId`。
- `backend/src/main/java/com/learningos/learning/api/LearningPathController.java:38-43` 按 `pathId` 查询后校验返回对象 `learnerId`，不允许非 owner 读取。

测试证据：

- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java:144-163` 覆盖 profile/path 创建 owner mismatch 返回 `FORBIDDEN`。
- `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java:173-192` 覆盖非 owner 查询 path 返回 `FORBIDDEN` 且不暴露 path 数据。

### 1.5 Resource / Review Gate 权限已有较强证据

- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java:92`、`:129` 创建资源生成任务要求 learner owner。
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java:238` 查询 generation task 前调用 `ensureTaskOwner`。
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java:247-250` learner resources 先 owner 检查，再要求 `canReleaseToLearner(taskId)`。
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java:620-628` `ensureLearnerOwner` 和 `ensureTaskOwner` 均以 `learnerId` 为授权依据。
- `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java:48-56` review list 先要求 reviewer 访问权，再按 review 可访问性过滤。
- `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java:59-76` review decision 先校验 reviewer，再加载 review/task，并对非 admin 的缺失/越权返回 `FORBIDDEN`，降低 reviewId 枚举价值。
- `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java:117-138` teacher 只能 review `ResourceGenerationTask.goalId -> Course.teacherId` 匹配自己的任务；admin 全局。
- `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java:140-160` learner release 要求 task/resource/review 全部发布/批准，且无 `NO_SOURCE`。

测试证据：

- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java:445-448` 覆盖 task/trace owner mismatch 返回 forbidden。
- `backend/src/test/java/com/learningos/agent/api/ResourceGenerationControllerTest.java:475-476` 覆盖未审核资源 learner read 返回 `FORBIDDEN`。
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java:57-92` 覆盖 student 不能 list/decision review。
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java:195-211` 覆盖 teacher 只能列出自己课程 review。
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java:215-240` 覆盖 teacher 不能处理外部课程 review，响应不泄露外部 task/resource id。
- `backend/src/test/java/com/learningos/agent/api/ResourceReviewControllerTest.java:244-257` 覆盖 teacher 对 missing review 与 forbidden review 均得到 `FORBIDDEN`，降低 IDOR 枚举差异。

### 1.6 Answer record / analytics 已有部分 owner 与 RBAC

- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java:89-92` 提交答案要求 `userId == request.learnerId`。
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java:122-125` answer replay 同样要求 learner owner。
- `backend/src/test/java/com/learningos/assessment/api/AssessmentControllerTest.java:243` 覆盖 learner mismatch forbidden。
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java:201-205` teacher class summary 要求 admin 或课程 teacher。
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java:256-268` token budget governance 要求 admin。
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java:355-366` 覆盖 foreign teacher / student 不能读 class summary。
- `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java:370` 覆盖 admin 可读 class summary。

### 1.7 安全扫描与依赖基线

- Secret 扫描：对 `backend/src/main`、`backend/src/test`、相关 P3-4 spec 执行关键字扫描，生产配置使用环境变量占位，例如 `backend/src/main/resources/application.yml:11` 的 `${DB_PASSWORD:}` 与 `:42` 的 `${MINIO_SECRET_KEY:}`；命中的 `sk-live-secret`、`raw-token`、`password=secret` 位于测试用例中，用于验证脱敏，不属于生产硬编码凭据。
- Git 历史扫描：当前工作目录不是 Git 仓库，`git -C D:/多元agent status` 返回 `fatal: not a git repository`，无法完成历史 secret 扫描。
- 依赖审计：执行 `mvn -f backend/pom.xml dependency:tree -DskipTests` 成功。主要依赖包括 Spring Boot `3.5.7`、Spring `6.2.12`、Tomcat `10.1.48`、Hibernate `6.6.33.Final`、MinIO `8.5.17`、OkHttp `4.12.0`、BouncyCastle `1.78.1`。本地未发现 `org.owasp:dependency-check` / `osv-scanner` 配置，未能产出 CVE 级审计报告。

## 2. 未完成 / 薄弱 IDOR 面

### 高风险：生产认证与角色可信边界缺失

位置：

- `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java:29-34`
- `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java:16-22`

问题：

- 任何调用方只要设置 `X-User-Id: admin` 就会被 `CurrentUserService.isAdmin()` 识别为 admin。
- `teacher` / `teacher_*` 的角色判断来自 userId 字符串或是否拥有课程，而不是可信 token claim / session / user-role 表。
- 当前 Service 层多数授权逻辑依赖 userId，因此生产环境若继续暴露 `DevAuthFilter`，所有 P3-4 RBAC 都可被请求头绕过。

影响：

- 未认证攻击者可伪造 admin/teacher/任意 learner 身份，读取或操作跨用户资源、review、analytics、KB 文档、学习路径、答题记录派生数据。

### 高风险：Course / Knowledge Graph 读路径尚未收口

位置：

- `backend/src/main/java/com/learningos/knowledge/api/CourseController.java:40-47`
- `backend/src/main/java/com/learningos/knowledge/api/CourseController.java:63-65`
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java:68-75`
- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java:127-145`

问题：

- `GET /api/courses/{courseId}`、`GET /api/courses`、`GET /api/courses/{courseId}/knowledge-graph` 未传入 `currentUserId`，Service 读路径无 teacher/student/admin 范围判断。
- 若 course 或 knowledge graph 包含教师课程私有内容、班级课程结构、未发布知识点，存在 courseId 枚举和跨教师课程读取风险。

影响：

- 学生或外部 teacher 可通过 courseId 枚举读取其他课程目录和知识图谱。

### 高风险：RAG KB 与 course 绑定缺少生产级一致性校验

位置：

- `backend/src/main/java/com/learningos/rag/application/KnowledgeBaseService.java:24-31`
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java:59-72`
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java:102-112`

问题：

- KB 创建以 owner/visibility 为主，未看到 `courseId -> teacherId` 或 enrollment 的绑定。
- 文档上传参数 `courseId` / `chapterId` 仅被保存到 `KbDocument`，上传前只校验 `kbId` 写权限，未验证传入 `courseId/chapterId` 与当前用户课程权限、与 KB 所属课程一致、chapter 属于 course。
- 结果是有 KB 写权限的用户可把文档标记到不属于自己的 courseId/chapterId，污染课程 RAG 元数据与后续 analytics / citation 关联。

影响：

- 数据污染、跨课程引用、教师侧课程资料边界不清；如果后续按 `courseId` 聚合 RAG 证据，会形成间接越权。

### 中高风险：answer record 没有读取接口，但派生数据读权限仍需矩阵补齐

位置：

- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java:89-92`
- `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java:201-205`

现状：

- 直接答题提交与 replay 已校验 learner owner。
- 当前未发现 `GET /api/assessment/answers/{answerId}` 之类直接读取接口。

薄弱点：

- answer record 派生到 grading、wrong question、mastery、learning event、class analytics。teacher 读 class summary 仅基于 `course.teacherId`，需要继续证明聚合数据只来自该 course / class 授权范围。
- 目前未看到“teacher 不能通过 courseId 读取非本课程 answer 派生数据”的完整矩阵覆盖到 answer / wrong cause / mastery 的边界。

影响：

- 如果后续新增 answer detail / wrong-question API，极易出现按 `answerId` / `learnerId` 直接读取的 IDOR。

### 中高风险：resource detail 的 teacher/admin 管理视图与 learner 视图边界不完整

位置：

- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java:238-250`
- `backend/src/main/java/com/learningos/agent/application/ReviewGovernanceService.java:48-76`

现状：

- learner 查询 generation task / learner resources 已 owner 检查。
- review queue/decision 已按 teacher course scope 控制。

薄弱点：

- `GET /api/resources/generation-tasks/{taskId}` 当前只允许 learner owner；teacher/admin 若需要教学管理视图，需要新增显式 service 方法并按 `task.goalId -> Course.teacherId` 或 admin 授权，不应复用 learner owner 逻辑。
- `review` summary 不返回 `markdownContent`，但 teacher/admin 未来如果需要资源详情，必须有独立“review detail”权限边界和脱敏策略。

影响：

- 当前是功能缺口；后续为 teacher/admin 打开 task/resource detail 时存在高概率 IDOR。

### 中风险：权限判断分散在 Controller 与 Service，容易产生新接口漏检

位置示例：

- `backend/src/main/java/com/learningos/learning/api/ProfileController.java:28-33`
- `backend/src/main/java/com/learningos/learning/api/LearningPathController.java:30-43`
- `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java:33-45`
- `backend/src/main/java/com/learningos/agent/application/ResourceGenerationService.java:620-634`
- `backend/src/main/java/com/learningos/rag/application/DocumentService.java:168-176`

问题：

- 部分 owner 检查在 Controller，部分在 Service；Service 方法如 `LearningWorkflowService.getPath(pathId)` 本身不携带 principal。
- 新增调用方如果绕过 Controller 直接复用 Service，可能漏掉 owner 检查。

影响：

- 对外接口扩展时产生回归风险；尤其 orchestrator / admin API / teacher API 复用学习路径、profile、资源服务时。

### 中风险：公共 KB 与课程授权模型尚未统一

位置：

- `backend/src/main/java/com/learningos/rag/application/PermissionService.java:110-117`

问题：

- `Visibility.PUBLIC` 对所有 userId 可读。对通用公开知识库是合理的，但对课程 KB 若 visibility 被误设为 PUBLIC，就绕过 teacher/student/course enrollment 控制。

影响：

- KB 可见性与课程可见性发生漂移，形成“课程私有、KB 公开”的配置型越权。

### 中风险：依赖 CVE 审计未完成到生产级

证据：

- `mvn dependency:tree` 成功，但这不是漏洞数据库审计。
- 当前未发现 dependency-check / osv-scanner / Snyk 等本地配置。
- 无 Git 仓库，无法执行 `git log -p` 历史 secret 扫描。

影响：

- 不能给出“依赖无已知高危 CVE”的生产结论。

## 3. 最小生产级补齐方案

### 3.1 先替换 DevAuthFilter，建立可信 Principal 与角色模型

目标：

- 生产 profile 禁用 `DevAuthFilter`。
- `CurrentUserService` 只能从可信认证上下文读取 userId / roles。
- admin/teacher/student 角色来自 token claim 或数据库用户角色，不允许由 `X-User-Id` 或 userId 前缀推导。

建议最小代码形态：

```java
// GOOD: CurrentUserService 只读可信认证上下文，不做 dev fallback / 字符串角色推断
@Service
public class CurrentUserService {
    public CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Authentication required");
        }
        String userId = authentication.getName();
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        return new CurrentUser(userId, roles);
    }

    public String currentUserId() {
        return currentUser().userId();
    }

    public boolean hasRole(String role) {
        return currentUser().roles().contains("ROLE_" + role);
    }
}
```

### 3.2 建立统一 AuthorizationService，Service 层强制收口

目标：

- 把 owner / teacher course / admin / KB read-write / answer owner 统一成一个 `AuthorizationService`。
- Controller 只负责传入 `currentUser`，真正决策在 Service 层。
- 所有按 id 查询的资源必须采用“加载资源 -> 校验关系 -> 返回”的顺序；对非 admin 缺失与 forbidden 的响应策略要统一，避免枚举 oracle。

建议最小接口：

```java
@Service
public class AuthorizationService {
    public void requireAdmin(CurrentUser user) { ... }
    public void requireLearnerSelf(CurrentUser user, String learnerId) { ... }
    public void requireCourseTeacherOrAdmin(CurrentUser user, String courseId) { ... }
    public void requireCourseReadable(CurrentUser user, String courseId) { ... }
    public void requireKbReadable(CurrentUser user, String kbId) { ... }
    public void requireKbWritable(CurrentUser user, String kbId) { ... }
    public void requireAnswerReadable(CurrentUser user, String answerId) { ... }
    public void requireResourceTaskReadable(CurrentUser user, String taskId) { ... }
}
```

### 3.3 Course / Knowledge Graph 读路径收口

最小策略：

- admin：可读全部。
- teacher：可读自己 `Course.teacherId == currentUserId` 的课程与 graph。
- student：只读已授权/已选课/已发布课程；如果当前没有 enrollment 模型，先只允许公开课程或明确 `Course.status == PUBLISHED` 的最小读。

建议代码形态：

```java
@Transactional(readOnly = true)
public KnowledgeGraphResponse getKnowledgeGraph(CurrentUser user, String courseId) {
    Course course = getCourse(courseId);
    authorizationService.requireCourseReadable(user, course.getId());
    ...
}
```

### 3.4 RAG KB 与 course/chapter 绑定校验

最小策略：

- KB 新增或复用 `courseId` 归属字段；course KB 默认继承 course 读写策略。
- 文档上传时：
  1. `requireKbWritable(user, kbId)`
  2. 若传 `courseId`，`requireCourseTeacherOrAdmin(user, courseId)` 或校验 KB 绑定课程一致
  3. 若传 `chapterId`，校验 chapter 属于 course
  4. 禁止把文档写入与 KB 绑定不一致的 course/chapter

建议代码形态：

```java
private void validateDocumentCourseScope(CurrentUser user, KnowledgeBase kb, String courseId, String chapterId) {
    if (courseId == null || courseId.isBlank()) {
        return;
    }
    authorizationService.requireCourseTeacherOrAdmin(user, courseId);
    if (kb.getCourseId() != null && !kb.getCourseId().equals(courseId)) {
        throw new ApiException(ErrorCode.FORBIDDEN, "Knowledge base course scope mismatch");
    }
    if (chapterId != null && !chapterRepository.existsByIdAndCourseId(chapterId, courseId)) {
        throw new ApiException(ErrorCode.VALIDATION_ERROR, "Chapter does not belong to course");
    }
}
```

### 3.5 Answer record / wrong question / mastery 读模型预先定界

最小策略：

- learner：只能读自己的 answer / grading / wrong question / mastery。
- teacher：只能读自己课程下学生的聚合数据；若没有 class enrollment，不允许按 learnerId 读个人答题明细。
- admin：可读全部，但需要审计日志。

建议先补 `AuthorizationService.requireAnswerReadable(user, answerId)`，即使当前没有 GET answer detail，也作为未来 API 的准入工具。

### 3.6 Resource 管理视图显式分离

最小策略：

- learner API：只返回 owner 且已 release 的资源内容。
- teacher review API：按 `task.goalId -> Course.teacherId` 授权，可看 review 必需的摘要和必要内容；避免从 learner endpoint 开洞。
- admin API：可全局读，但输出仍遵守敏感字段脱敏。

建议：

- 新增 `getReviewResourceDetail(CurrentUser user, reviewId)`，不要让 teacher/admin 调 `getTask(userId, taskId)` 绕过 learner owner 语义。

### 3.7 把测试与依赖审计纳入 P3-4 Definition of Done

最小生产门禁：

- 新增 Maven profile 或 CI 步骤运行 OWASP Dependency-Check / OSV Scanner。
- 增加历史 secret 扫描前提：项目需在 Git 仓库中运行，或 CI 对实际 repo 执行 `gitleaks detect`。
- 禁止生产 profile 注册 `DevAuthFilter`，用测试断言防止误开。

## 4. 建议测试矩阵

### 4.1 身份与角色基础

| 场景 | student | teacher_own | teacher_foreign | admin | 期望 |
|---|---:|---:|---:|---:|---|
| 无认证访问受保护 API | 401 | 401 | 401 | 401 | 生产 profile 不得 fallback `dev_user` |
| 伪造 `X-User-Id: admin` | 403/401 | 403/401 | 403/401 | N/A | 请求头不能授予 admin |
| userId 为 `teacher_x` 但无 teacher role | 403 | 403 | 403 | N/A | 不因前缀获得 teacher 权限 |
| 有 teacher role 但非课程 teacher | 403 | 200 | 403 | 200 | 角色 + 资源关系同时满足 |

### 4.2 Course / Knowledge Graph

| API | student | teacher_own | teacher_foreign | admin | 补测重点 |
|---|---:|---:|---:|---:|---|
| `GET /api/courses` | 只见可读/已发布 | 只见自己课程 | 不见他人私有课 | 全部 | 防课程列表越权 |
| `GET /api/courses/{courseId}` | 未授权 403/404 | 200 | 403/404 | 200 | 防 courseId 枚举 |
| `GET /api/courses/{courseId}/knowledge-graph` | 未授权 403/404 | 200 | 403/404 | 200 | 防 graph 泄露 |
| `POST /api/courses/{courseId}/chapters` | 403 | 200 | 403 | 200 | 已有，保留回归 |
| `POST /api/knowledge-points` | 403 | 200 | 403 | 200 | 已有，保留回归 |
| `POST /api/knowledge-dependencies` | 403 | 200 | 403 | 200 | 已有，保留回归 |

### 4.3 RAG KB / Document / Query

| API | owner | user with READ | user with WRITE | public user | foreign user | 期望 |
|---|---:|---:|---:|---:|---:|---|
| `GET /api/knowledge-bases` | 可见 | 可见 | 可见 | 仅 public | 不见 private | 列表过滤 |
| `POST /api/knowledge-bases/{kbId}/documents` | 200 | 403 | 200 | 403 | 403 | 写权限严格 |
| upload with foreign `courseId` | 403 | 403 | 403 | 403 | 403 | 防 course metadata 污染 |
| `GET /api/documents/{documentId}` | 200 | 200 | 200 | public only | 403 | documentId IDOR |
| `POST /api/documents/{documentId}/reindex` | 200 | 403 | 200 | 403 | 403 | write-only |
| `GET /api/index-tasks/{taskId}` | 200 | 200 | 200 | public only | 403 且不泄露文件名 | taskId IDOR |
| `POST /api/rag/query` mixed allowed + forbidden `kbIds` | 403 | 403 | 403 | 403 | 403 | strict reject，不落 query/citation |
| `GET /api/rag/query` | 同 POST | 同 POST | 同 POST | 同 POST | 同 POST | GET handler 复用 strict |
| `GET /api/chat/sessions/{sessionId}/stream` | 同 query | 同 query | 同 query | 同 query | 同 query | SSE 错误不泄露内部异常 |
| `POST /api/tutor/ask` | 同 query | 同 query | 同 query | 同 query | 同 query | tutor 复用 RAG 授权 |

### 4.4 Learning Path / Profile / Resource

| API | learner owner | other student | teacher | admin | 期望 |
|---|---:|---:|---:|---:|---|
| `POST /api/profile/dialogue/extract` | 200 | 403 | 403（除非有显式 teacher note API） | 可选 200/403，需定义 | 防 learnerId 篡改 |
| `POST /api/learning-paths` | 200 | 403 | 403 | 可选 200/403，需定义 | 防替他人建 path |
| `GET /api/learning-paths/{pathId}` | 200 | 403 且不泄露 path | teacher 需课程关系才可读或 403 | 200 | pathId IDOR |
| `POST /api/resources/generation-tasks` | 200 | 403 | 403 | 可选，需定义 | 防替他人生成 |
| `GET /api/resources/generation-tasks/{taskId}` | 200 | 403 | 如需 review detail 用独立 API | 200/独立 API | 语义分离 |
| `GET /api/agent/resource-generation/tasks/{taskId}/learner-resources` | release 后 200 | 403 | 403 | 可选 | 未审核资源不可读 |

### 4.5 Answer / Assessment / Analytics

| API / 数据 | learner owner | other student | teacher_own | teacher_foreign | admin | 期望 |
|---|---:|---:|---:|---:|---:|---|
| `POST /api/assessment/answers` | 200 | 403 | 403 | 403 | 可选，需定义 | 防 learnerId 篡改 |
| replay same `requestId` | 200 | 403 | 403 | 403 | 可选 | replay 不绕过 owner |
| future `GET /api/assessment/answers/{answerId}` | 200 | 403/404 | 默认 403，除非课程授权 | 403/404 | 200 | 预留测试 |
| `GET /api/analytics/students/{learnerId}/summary` | 200 | 403 | 403 或课程授权后有限读 | 403 | 200/需定义 | 当前 owner 最小片已覆盖 |
| `GET /api/analytics/classes/{courseId}/summary` | 403 | 403 | 200 | 403 | 200 | 已有，补 answer 派生范围断言 |
| `GET /api/analytics/overview` | 403 | 403 | 403 | 403 | 200 | 已有 |

### 4.6 Review Gate RBAC

| API | student | teacher_own | teacher_foreign | admin | 期望 |
|---|---:|---:|---:|---:|---|
| `GET /api/reviews/resources` | 403 | 只列自己课程 | 不列外部课程 | 全部 | 已有，继续回归 |
| `POST /api/reviews/resources/{reviewId}/decision` own | 403 | 200 | 403 | 200 | 已有，继续回归 |
| missing reviewId vs forbidden reviewId | 403/统一响应 | 403/统一响应 | 403/统一响应 | admin 可 404 | 防枚举 |
| approve `NO_SOURCE` resource | 409 | 409 | 409 | 409 | 不能绕过 citation gate |

## 5. 风险

### 总体风险等级：HIGH

理由：

- 虽然 P3-4 最小切片已经覆盖 profile/path/RAG strict/review gate/analytics 的关键高风险面，但生产认证仍是开发期 `X-User-Id` 模拟，属于 OWASP A01 Broken Access Control 的根风险。
- Course / Knowledge Graph 读路径没有授权收口，存在可直接枚举的 IDOR 面。
- RAG KB 与 course/chapter 归属未绑定，存在跨课程数据污染和后续间接越权风险。
- 依赖漏洞审计和 Git 历史 secret 扫描未达到生产级闭环。

### 优先级排序

1. **P0 / 必须先做**：生产 profile 禁用 `DevAuthFilter`，接入真实认证，`CurrentUserService` 不再信任 `X-User-Id` 或 userId 前缀。
2. **P0 / 必须先做**：Course / Knowledge Graph 读路径加 `currentUser` 与 `requireCourseReadable`。
3. **P1**：RAG KB 与 course/chapter 绑定，并在 document upload / query / list 中统一校验课程范围。
4. **P1**：抽出统一 `AuthorizationService`，把 Controller 层 owner 检查下沉到 Service，减少新接口漏检。
5. **P1**：补齐 teacher/student/admin RBAC 测试矩阵，尤其 `courseId`、`taskId/resourceId`、`answerId`、`kbId/documentId/indexTaskId`。
6. **P2**：接入 CVE 依赖审计与 Git 历史 secret 扫描到 CI。

### 可上线判断

- 当前状态适合继续作为开发/演示环境。
- 不建议作为生产权限边界上线；至少需要完成“真实认证 + Course 读路径授权 + RAG course 绑定校验 + RBAC 矩阵测试”后再进入生产候选。
