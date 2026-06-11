# RUN-20260609 P3-4-Q 只读测试设计分析报告

## 0. 元信息

- 角色：Test Engineer
- 范围：backend TODO P3-4 下一切片 P3-4-Q 测试设计
- 聚焦：legacy `CourseAccessService` 调用方、broader class/course、roles-first RBAC
- 执行方式：只读分析
- 文件修改：无
- 网络访问：无
- `node_repl`：未启动
- P3-4 完成状态：不声明完成

## 1. 证据摘要

### 1.1 剩余风险来自 legacy CourseAccessService 调用方

证据：

- `CourseAccessService` 旧 `requireCourseRead(String currentUserId, String courseId)` 仍从 `currentUserId` 推断 admin/teacher。
- `CourseAccessService` 旧 `requireCourseManage(String, Course)` 与 `requireLearnerEnrolledForExistingCourse(...)` 仍依赖 subject-name inference。
- `GradingEvaluationService` 仍用本地 `isAdmin(currentUserId)` / `isTeacherUser(currentUserId)`。
- `AnalyticsService` student summary course scope 仍调用旧 `requireCourseRead(currentUserId, courseId)`。
- `ResourceGenerationService` 和 `LearningWorkflowService` 也仍有旧签名调用，但副作用更重。

### 1.2 现有测试模式

- `AssessmentControllerTest` 和 `AnalyticsControllerTest` 均使用 `@SpringBootTest + @AutoConfigureMockMvc + @Transactional`。
- `AnalyticsControllerTest` 已配置 `learning-os.auth.jwt-secret` / `issuer`，具备 Bearer JWT helper。
- `CourseKnowledgeControllerTest` / `AnalyticsControllerTest` 已有 roles-first Bearer、spoofed header、subject-name role-confusion 测试命名模式。

## 2. 最适合 P3-4-Q 的最小可测范围

推荐 P3-4-Q 切片：

**Analytics Student Summary 的 legacy `CourseAccessService` caller roles-first RED 矩阵。**

最小范围：

1. `GET /api/analytics/students/{learnerId}/summary?courseId=...`
   - 覆盖 Bearer `TEACHER` 使用 no-prefix subject 读取 own-course active learner。
   - 覆盖 Bearer `USER sub=teacher_1` 不被 legacy subject 提权。
   - 覆盖 Bearer `ADMIN` + spoofed `X-User-Id` 读取 course-scoped summary。
   - 覆盖 non-admin missing/foreign course 仍返回 safe `FORBIDDEN`。

不建议 P3-4-Q 纳入：

- `ResourceGenerationService` admin detail/trace roles-first：涉及 Agent task、review gate、model gateway side effect，fixture 更重，适合作为 P3-4-S。
- 全量 `CourseAccessService` 旧签名删除：会跨 Assessment / Analytics / ResourceGeneration / RAG / Learning，blast radius 大。
- formal OAuth2/JWK/Spring Security：不是当前最小业务 RBAC RED。
- broader class/course domain 建模：当前没有明确 class entity/teacher-class membership 模型。

## 3. 需要新增/修改的测试文件与测试用例名建议

修改 `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java`。

建议新增：

- `studentSummaryUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`
- `bearerTeacherCanReadCourseScopedStudentSummaryForOwnCourseWithoutTeacherIdPrefix`
- `bearerUserSubjectTeacherPrefixCannotReadCourseScopedStudentSummaryAsTeacher`
- `bearerTeacherCannotDistinguishMissingCourseFromForbiddenStudentSummaryCourse`

## 4. 预期 RED 表现

- `studentSummaryUsesBearerAdminRoleAndIgnoresSpoofedUserIdHeader`：当前可能返回 `403 FORBIDDEN`，因为 service 内旧 `requireCourseRead(currentUserId, courseId)` 对 `ops_admin` 不认 admin。
- `bearerTeacherCanReadCourseScopedStudentSummaryForOwnCourseWithoutTeacherIdPrefix`：当前可能返回 `403 FORBIDDEN`，因为旧签名不认 `instructor_1` 的 `TEACHER` role。
- `bearerUserSubjectTeacherPrefixCannotReadCourseScopedStudentSummaryAsTeacher`：当前可能错误通过，或成为防回归用例，取决于 Controller role facts 是否先拒绝。

## 5. Verification 命令建议

Focused:

```powershell
cd backend
mvn --% -Dtest=AnalyticsControllerTest test
```

Adjacent:

```powershell
cd backend
mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,DevAuthFilterTest,CurrentUserServiceTest test
```

Full:

```powershell
cd backend
mvn test
```

## 6. 测试隔离与 fixture 风险

- 新增 no-prefix teacher 测试最好单独 seed `course_instructor_summary`，不要复用原固定 fixture。
- RED 测试必须断言响应体不泄漏 foreign/missing IDs：沿用 `doesNotContain(courseId)` / `jsonPath("$.data").doesNotExist()` 模式。
- 每个测试只验证一个 RBAC 行为，不要把 admin success、teacher success、role-confusion denial 混在一个 mega-test 中。
