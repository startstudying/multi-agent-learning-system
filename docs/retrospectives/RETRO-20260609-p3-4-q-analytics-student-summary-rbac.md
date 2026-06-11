# RETRO - P3-4-Q Analytics student summary roles-first RBAC

## 1. 做得好的地方

- 切片范围控制得很窄，只改一个 Service 调用点和一个 Controller 测试类。
- TDD RED 明确命中 `ADMIN sub=ops_admin` 与 `TEACHER sub=instructor_1` 被 legacy caller 拒绝的问题。
- 新增测试同时覆盖 spoofed header、teacher no-prefix、`USER sub=teacher_1` role-confusion 和 missing/foreign anti-enumeration。
- 无 API、DB、依赖、frontend drift。

## 2. 可以改进的地方

- `AnalyticsController.studentSummary(...)` 仍使用 `CurrentUserService.isAdmin()` / `isTeacherUser()` helper；当前 helper 已优先使用 roles，但与 P3-4-P 的严格 `UserContext.roles()` 模式相比仍有 dev/test legacy 兼容语义。后续如做统一 roles-first controller cleanup，可单独切片处理。
- 其他 legacy `CourseAccessService` 调用方仍存在，需要继续按小切片迁移，避免一次跨 Assessment / Learning / ResourceGeneration。

## 3. 经验沉淀

- P3-4-M 新增 role-aware overload 后，后续修复应优先搜索旧签名调用方，而不是扩展权限模型。
- 对 legacy subject inference 的回归测试最好同时包含：
  - Bearer admin 非 `admin` subject。
  - Bearer teacher 非 `teacher_` subject。
  - Bearer ordinary user 使用 `admin` / `teacher_*` subject。
  - non-admin missing/foreign response equivalence。

## 4. Skill Extraction

不创建新 skill。本次模式已被 `auth-context-boundary` 和 `object-scope-authorization` 覆盖。

## 5. 后续任务

- P3-4-R：Assessment / GradingEvaluation roles-first caller migration。
- P3-4-S：LearningPath / ResourceGeneration course-bound create roles-first enrollment migration。
- Formal OAuth2/JWK/Spring Security 仍保留为独立生产化任务。
