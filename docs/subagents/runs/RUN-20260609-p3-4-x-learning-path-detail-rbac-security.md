# RUN-20260609 P3-4-X LearningPath Detail Roles-First RBAC Security Review

## 0. 审计范围与结论

- 审计对象：`GET /api/learning-paths/{pathId}` 学习路径详情读取。
- 主要代码：
  - `backend/src/main/java/com/learningos/learning/api/LearningPathController.java`
  - `backend/src/main/java/com/learningos/learning/application/LearningWorkflowService.java`
  - `backend/src/main/java/com/learningos/common/auth/DevAuthFilter.java`
  - `backend/src/main/java/com/learningos/common/auth/CurrentUserService.java`
  - `backend/src/test/java/com/learningos/learning/api/LearningWorkflowControllerTest.java`
- 相关技能：`security-review`、`java-security-review`、`object-scope-authorization`、`auth-context-boundary`。
- 总体风险等级：HIGH。

结论：当前 `GET /api/learning-paths/{pathId}` 存在明确的角色混淆风险。Controller 的 GET 分支只传入 `currentUserService.currentUserId()`，Service 在 `getPathForUser(String currentUserId, String pathId)` 内继续调用 `isAdmin(currentUserId)`，而 `isAdmin` 仅判断 `userId == "admin"`。因此一个已签名 Bearer JWT 只要携带 `sub="admin"`、`roles=["USER"]`，就会在该 GET 详情接口中获得 admin detail 语义：可读取他人学习路径，并可对 missing path 得到 admin-only `NOT_FOUND`。这与 P3-4 系列 roles-first RBAC 目标冲突。

现有 non-admin missing vs foreign 防枚举测试对普通 `X-User-Id=bob` 路径有效，但未覆盖 Bearer `USER sub=admin`。因此当前防枚举语义在真实 Bearer 场景下不完整。

## 1. 漏洞/风险模型

### 1.1 确认漏洞：Bearer USER sub=admin 被当作 admin

**Severity:** HIGH  
**OWASP:** A01 Broken Access Control / Identification and Authentication boundary confusion  
**Exploitability:** 远程、已认证；需要能获得或伪造一个签名有效、`sub=admin`、但无 `ADMIN` role 的 Bearer token。  
**Blast Radius:** 任意学习路径详情读取；响应包含 learnerId、goalId、summary、节点、profileSnapshot、traceId 等学习画像/路径信息。还会暴露 admin-only missing 语义，形成 pathId 存在性 oracle。

证据：

- `LearningPathController.get` 只传 `currentUserId()`：

```java
@GetMapping("/{pathId}")
public ApiResponse<LearningPathResponse> get(@PathVariable String pathId) {
    return ApiResponse.success(learningWorkflowService.getPathForUser(currentUserService.currentUserId(), pathId));
}
```

- `LearningWorkflowService.getPathForUser` 对缺失和归属校验都调用 legacy `isAdmin(currentUserId)`：

```java
public LearningPathResponse getPathForUser(String currentUserId, String pathId) {
    LearningPathResponse response;
    try {
        response = getPath(pathId);
    } catch (ApiException exception) {
        if (exception.getErrorCode() == ErrorCode.NOT_FOUND && !isAdmin(currentUserId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Learning path access denied");
        }
        throw exception;
    }
    if (!isAdmin(currentUserId) && !currentUserId.equals(response.learnerId())) {
        throw new ApiException(ErrorCode.FORBIDDEN, "Learning path access denied");
    }
    return response;
}

private boolean isAdmin(String userId) {
    return "admin".equals(userId);
}
```

- `DevAuthFilter` 对 Bearer JWT 会把 `sub` 作为 `UserContext.userId`，roles 单独来自 `roles` claim。即 `sub=admin, roles=["USER"]` 会建立 `userId=admin, roles=USER` 的上下文；在 roles-first 语义下它不应获得 admin 权限。

```java
String subject = stringClaim(payload.get("sub"));
return new UserContext(subject, displayName == null ? subject : displayName, roles(payload.get("roles")));
```

影响路径：

1. 攻击者携带有效 Bearer：`sub=admin`, `roles=["USER"]`。
2. 访问 `GET /api/learning-paths/{alicePathId}`。
3. Controller 传入 `currentUserId="admin"`，未传 roles。
4. Service `isAdmin("admin") == true`，跳过 owner 检查。
5. 返回 Alice 的学习路径详情。

### 1.2 防枚举语义评估

当前目标语义本身合理：

- Admin：对 missing path 返回真实 `NOT_FOUND`，便于运维定位。
- Non-admin：对 missing path 和 foreign path 都返回 `FORBIDDEN`，且无 `data`，响应 body 不包含目标 pathId，避免对象存在性枚举。

现有测试覆盖了普通非管理员：

```java
mockMvc.perform(get("/api/learning-paths/{pathId}", pathId).header("X-User-Id", "bob"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"))
        .andExpect(jsonPath("$.data").doesNotExist());

mockMvc.perform(get("/api/learning-paths/{pathId}", "path_missing_object_scope").header("X-User-Id", "bob"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"))
        .andExpect(jsonPath("$.data").doesNotExist());
```

缺口：Bearer `USER sub=admin` 在当前实现中不属于真实 admin，但会走 admin branch：

- foreign path：错误地返回 `200 OK` + data。
- missing path：错误地返回 `404 NOT_FOUND`，而不是 non-admin 统一 `403 FORBIDDEN`。

因此语义设计合理，实现不满足 roles-first 要求。

### 1.3 API/DTO/schema 变化风险

本次修复不需要 API path、请求 DTO、响应 DTO、数据库 schema 或前端变化。权限事实应作为服务层内部参数传递：

- Controller 从 `CurrentUserService.currentUser()` 获取 `UserContext`。
- Controller 派生 `currentUserAdmin = hasRole(currentUser, "ADMIN")`。
- Service 新增/使用 roles-first overload：`getPathForUser(String currentUserId, boolean currentUserAdmin, String pathId)`。
- 保留旧方法只作为 dev/test 兼容或纯单测兼容时，应避免 HTTP GET 再调用旧方法。

## 2. 推荐测试矩阵

优先在 `LearningWorkflowControllerTest` 增加 HTTP 级集成测试，使用现有 `jwt(sub, name, roles)` helper，避免真实 secret。

| 优先级 | 场景 | 身份 | 目标 path | 预期 |
|---|---|---|---|---|
| P0 | Bearer subject-admin role confusion 读取 foreign path | `Authorization: Bearer jwt("admin", ..., ["USER"])` | Alice path | `403 FORBIDDEN`，无 `data`，body 不含 pathId、learnerId、goalId、summary |
| P0 | Bearer subject-admin role confusion 读取 missing path | `Authorization: Bearer jwt("admin", ..., ["USER"])` | `path_missing_subject_admin` | `403 FORBIDDEN`，无 `data`，body 不含 missing id |
| P0 | Bearer explicit admin 可读 foreign path | `Authorization: Bearer jwt("ops_admin", ..., ["ADMIN"])` + spoofed `X-User-Id=alice` | Bob path | `200 OK`，读取目标 path，证明 roles-first admin 仍工作 |
| P0 | Bearer explicit admin 读取 missing path | `Authorization: Bearer jwt("ops_admin", ..., ["ADMIN"])` | missing path | `404 NOT_FOUND`，无 `data` |
| P1 | Bearer student spoofed admin header | `Authorization: Bearer jwt("alice", ..., ["STUDENT"])` + `X-User-Id=admin` | Bob path | `403 FORBIDDEN`，无 `data` |
| P1 | Bearer owner 读取 own path | `Authorization: Bearer jwt("alice", ..., ["STUDENT"])` | Alice path | `200 OK` |
| P1 | Dev/test legacy header admin 兼容 | `X-User-Id=admin`，无 Bearer | foreign path / missing path | 视当前过渡策略决定：若保留 dev 兼容，可继续 admin 语义；若本切片只处理 Bearer，则至少不要影响已有测试 |
| P2 | Response anti-enumeration 对比 | 同一非管理员访问 foreign 与 missing | foreign / missing | status/code/data 一致，body 不含对象 id 或目标 learner 信息 |

建议测试名：

- `learningPathGetRejectsBearerUserSubjectAdminRoleConfusionForForeignPath`
- `learningPathGetTreatsBearerUserSubjectAdminMissingAsForbidden`
- `learningPathGetBearerAdminRoleCanReadForeignPathAndSeesMissingAsNotFound`
- `learningPathGetBearerStudentSpoofedAdminHeaderCannotReadForeignPath`

## 3. 最小修复策略

目标：只修改内部授权参数传递，不改变 REST API、DTO、schema、前端和依赖。

### 3.1 Controller 改为 roles-first 调用

```java
// GOOD
@GetMapping("/{pathId}")
public ApiResponse<LearningPathResponse> get(@PathVariable String pathId) {
    UserContext currentUser = currentUserService.currentUser();
    return ApiResponse.success(learningWorkflowService.getPathForUser(
            currentUser.userId(),
            hasRole(currentUser, "ADMIN"),
            pathId
    ));
}
```

### 3.2 Service 新增 roles-first overload

```java
// BAD
public LearningPathResponse getPathForUser(String currentUserId, String pathId) {
    LearningPathResponse response;
    try {
        response = getPath(pathId);
    } catch (ApiException exception) {
        if (exception.getErrorCode() == ErrorCode.NOT_FOUND && !isAdmin(currentUserId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Learning path access denied");
        }
        throw exception;
    }
    if (!isAdmin(currentUserId) && !currentUserId.equals(response.learnerId())) {
        throw new ApiException(ErrorCode.FORBIDDEN, "Learning path access denied");
    }
    return response;
}
```

```java
// GOOD
public LearningPathResponse getPathForUser(String currentUserId, boolean currentUserAdmin, String pathId) {
    LearningPathResponse response;
    try {
        response = getPath(pathId);
    } catch (ApiException exception) {
        if (exception.getErrorCode() == ErrorCode.NOT_FOUND && !currentUserAdmin) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Learning path access denied");
        }
        throw exception;
    }
    if (!currentUserAdmin && !currentUserId.equals(response.learnerId())) {
        throw new ApiException(ErrorCode.FORBIDDEN, "Learning path access denied");
    }
    return response;
}
```

旧 overload 可短期保留：

```java
public LearningPathResponse getPathForUser(String currentUserId, String pathId) {
    return getPathForUser(currentUserId, isAdmin(currentUserId), pathId);
}
```

但验收应要求 HTTP Controller 不再调用旧 overload。若本项目要继续清理 legacy subject-name 权限，后续可删除旧 overload 或限制为测试兼容路径，并增加反射测试防止新增 HTTP 调用继续使用 subject-name admin。

### 3.3 不建议的修复

- 不建议把 `CurrentUserService.currentUserId()` 改为返回带 role 的特殊字符串。
- 不建议在 DTO 中加入 role/admin 字段。
- 不建议改变 `LearningPathResponse` 或数据库表结构。
- 不建议把权限判断移动到前端或 Prompt。
- 不建议对 pathId 使用格式校验代替对象级授权；格式校验不能解决 IDOR。

## 4. 剩余安全风险

1. `LearningWorkflowService.createPathForUser(String currentUserId, CreateLearningPathRequest request)` 仍调用 `isAdmin(currentUserId)`。当前 HTTP POST 已走 roles-first overload，但旧方法若被 Orchestrator、单测或未来新 Controller 误用，仍可能重引入 subject-name admin 语义。建议后续清理或标记为 legacy，并用测试约束 HTTP path 不得调用。
2. `LearningWorkflowService.isAdmin(String userId)` 是局部 legacy helper。只要存在，就可能被未来新增方法误用。建议后续统一迁移到显式 `currentUserAdmin` 参数或 `UserContext` 参数。
3. 正式 OAuth2/JWK/Spring Security 仍未完成。当前 HS256 Bearer 是过渡实现，验收只能证明 roles-first 业务授权正确，不能替代生产级资源服务器审计。
4. `X-User-Id` dev/test fallback 在非生产环境仍可赋予 `admin` legacy roles。只要 dev/test 环境暴露公网，就会成为高风险配置问题；生产/预发必须继续强制 missing Bearer 为 `UNAUTHORIZED`。
5. 本轮未执行代码修复，只做审计与报告；漏洞仍存在于当前工作区代码。

## 5. 验收建议

### 5.1 必须满足

- `GET /api/learning-paths/{pathId}` Controller 使用 `currentUserService.currentUser()`，从 `UserContext.roles()` 派生 admin 事实。
- Service 的详情授权使用显式 `currentUserAdmin`，不得在 HTTP GET path 上通过 `currentUserId == "admin"` 推断管理员。
- Bearer `USER sub=admin`：
  - 访问 foreign path 返回 `403 FORBIDDEN`。
  - 访问 missing path 返回 `403 FORBIDDEN`。
  - 响应无 `data`，body 不包含目标 pathId、foreign learnerId、goalId、summary、nodes、profileSnapshot、traceId。
- Bearer `ADMIN sub=ops_admin`：
  - 可读取 foreign existing path。
  - 对 missing path 返回 `404 NOT_FOUND`。
  - spoofed `X-User-Id` 不影响 Bearer 身份。
- Owner 读取 own path 保持 `200 OK`。
- 不改变 API path、请求 DTO、响应 DTO、schema、frontend 或依赖。

### 5.2 推荐验证命令

```powershell
cd backend
mvn --% -Dtest=LearningWorkflowControllerTest test
mvn --% -Dtest=LearningWorkflowControllerTest,CurrentUserServiceTest,DevAuthFilterTest test
```

如修复触及共享授权 helper，再补：

```powershell
cd backend
mvn test
```

### 5.3 本次审计辅助检查状态

- Secrets scan：已运行 `rg` 扫描 `backend/src/main`、`frontend/src`、`docs`，未发现真实 API key/private key；命中项主要为文档安全规则、测试假 secret、Docker Compose 默认口令、token/cost 业务字段。默认口令仍应仅限本地开发，不得用于生产。
- Git history secrets scan：未完成，当前 `D:\多元agent` 不是 Git 仓库，`git log -p` 返回 `not a git repository`。
- Dependency audit：尝试运行 `mvn -q org.owasp:dependency-check-maven:check -DskipTests`，因 `raw.githubusercontent.com` DNS 失败导致 RetireJS / vulnerability data 初始化失败，未形成有效 CVE 结论。`mvn -q dependency:tree -Dscope=runtime` 可执行但不等价于漏洞审计。

## 6. OWASP 覆盖表

| 类别 | 本接口适用性 | 结论 |
|---|---|---|
| A01 Broken Access Control | 高 | 发现 HIGH：Bearer `USER sub=admin` 可被当作 admin。 |
| A02 Cryptographic Failures | 中 | Bearer 签名校验在 auth filter；本问题不是签名失败，而是业务授权忽略 roles。 |
| A03 Injection | 低 | `pathId` 通过 JPA repository 查询，未见拼接 SQL；本轮未发现注入证据。 |
| A04 Insecure Design | 高 | Service 接口只接收 userId 导致角色事实丢失，是设计层权限边界缺陷。 |
| A05 Security Misconfiguration | 中 | dev/test header fallback 需保证不暴露公网；本接口修复不改变配置。 |
| A06 Vulnerable Components | 未定 | Dependency-Check 因网络/DNS 失败未完成。 |
| A07 Identification and Authentication Failures | 中 | Auth filter 能建立 Bearer roles，但业务层未使用 roles。 |
| A08 Software/Data Integrity Failures | 低 | 本接口只读，未发现供应链/完整性写入风险；依赖审计未完成。 |
| A09 Security Logging and Monitoring Failures | 中 | 当前 forbidden/not_found 会进入结构化请求日志；建议确保日志不记录 foreign path 详情。 |
| A10 SSRF | 不适用 | 本接口无 URL 抓取或外部请求。 |

