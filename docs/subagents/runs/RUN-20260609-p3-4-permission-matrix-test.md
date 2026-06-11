# RUN-20260609-p3-4 权限渗透测试矩阵补齐建议

## 结论

建议新增一个统一的轻量 matrix test：`backend/src/test/java/com/learningos/security/api/PermissionMatrixControllerTest.java`，不要把补齐用例继续分散到现有控制器测试类。

理由：

- 现有权限测试已经大量分散在 `CourseKnowledgeControllerTest`、`AssessmentControllerTest`、`DocumentControllerTest`、`ResourceGenerationControllerTest`、`ResourceReviewControllerTest`、`AnalyticsControllerTest` 中，继续追加会增加重复和定位成本。
- 现有类主要验证“单模块业务权限正确”，缺少一个跨模块的“同一攻击模型是否一致”的最小渗透矩阵。
- 新 matrix test 应只做薄片冒烟，不替代现有业务测试：每个端点只覆盖 1 个最关键越权/防枚举断言，避免 mega-test。

## 建议测试类定位

`PermissionMatrixControllerTest` 建议使用现有 Spring Boot + MockMvc + H2 `create-drop` 模式，沿用已有测试风格：

- `@SpringBootTest`
- `@AutoConfigureMockMvc`
- `@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")`
- `MockMvc`
- `jsonPath("$.code").value("FORBIDDEN")`
- `jsonPath("$.data").doesNotExist()`
- 响应 body 不包含 foreign/missing object id

## 建议新增测试方法清单

1. `studentCannotWriteTeacherManagedSurfaces()`
   - 场景：学生尝试创建课程、上传带 `courseId` 的课程 RAG 文档、提交资源审核决策。
   - 断言：全部返回 `403 FORBIDDEN`，`data` 不存在，不创建课程文档/审核副作用。
   - 目的：覆盖学生横向提权到教师写接口。

2. `teacherCannotAccessForeignCourseScopedSurfaces()`
   - 场景：`teacher_a` 访问 `teacher_b` 课程下的课程详情/knowledge graph、assessment list、student analytics、review decision。
   - 断言：全部返回 `403 FORBIDDEN`，响应不包含 foreign course/task/review id。
   - 目的：覆盖教师跨班级/跨课程 IDOR。

3. `studentCannotAccessForeignLearnerScopedSurfaces()`
   - 场景：`alice` 访问 `bob` 的 assessment answer detail、wrong-question detail、student summary、resource generation task/detail trace。
   - 断言：全部返回 `403 FORBIDDEN`，响应不包含 `bob`、foreign answer/task/trace id。
   - 目的：覆盖学生跨 learner IDOR。

4. `nonAdminMissingAndForeignObjectResponsesAreIndistinguishable()`
   - 场景：同一非管理员分别访问 foreign object 与 missing object：course detail、assessment answer detail、document detail、index task detail、resource generation task detail、agent task trace、review decision。
   - 断言：foreign 与 missing 都是 `403 FORBIDDEN`，`data` 不存在，body 不回显对象 id。
   - 目的：覆盖对象枚举 oracle。

5. `adminKeepsGlobalReadButMissingObjectsUseNotFoundSemantics()`
   - 场景：admin 访问已有 foreign course/answer/student summary/class summary 可成功；访问 missing course/answer/grading course 返回 `404 NOT_FOUND`。
   - 断言：已有对象 `200 OK`；missing object `404 NOT_FOUND`。
   - 目的：确保补齐非管理员防枚举时不破坏 admin 语义。

6. `productionAuthDoesNotTrustSpoofedUserIdHeader()`
   - 场景：生产模式下缺少 Bearer 但带 `X-User-Id: admin`，以及 Bearer student + spoofed `X-User-Id: admin`。
   - 断言：缺 Bearer 返回 `401 UNAUTHORIZED`；有效 Bearer 使用 token user/role，不使用 header。
   - 目的：保留 auth boundary 的渗透入口测试。

## 已存在用例，避免重复

- `CourseKnowledgeControllerTest` 已覆盖 create/list/detail/graph scope 与 missing-vs-foreign。
- `AssessmentControllerTest` 已覆盖 answer/wrong-question detail/list/grading evaluation scope。
- `DocumentControllerTest` 已覆盖 upload metadata scope、public KB read-but-not-write、document/index anti-enumeration。
- `ResourceGenerationControllerTest` 已覆盖 cross-user task/trace、review release、course-bound generation、mismatched learner。
- `ResourceReviewControllerTest` 已覆盖 student deny、admin global、teacher own-course、foreign/missing review deny。
- `AnalyticsControllerTest` 已覆盖 overview/ops alerts admin-only、student summary、teacher/admin class summary。
- `DevAuthFilterTest` / `CurrentUserServiceTest` 已覆盖 Bearer/header spoofing 和 roles-first RBAC。

## 最小 Maven 命令

Focused:

```bash
cd backend && mvn test -Dtest=PermissionMatrixControllerTest
```

Adjacent:

```bash
cd backend && mvn test -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,DocumentControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,AnalyticsControllerTest,DevAuthFilterTest,CurrentUserServiceTest,PermissionServiceTest
```

Full:

```bash
cd backend && mvn test
```

## RED 后拆修复建议

如果新增 matrix RED，不要一次性重构权限体系，按暴露面拆最小修复：

1. Auth RED：先修 `DevAuthFilter` / `CurrentUserService`，保证身份来源可信。
2. Course scope RED：修 knowledge/course read-manage scope helper，避免 controller 层散落判断。
3. Learner scope RED：修 assessment/resource generation 的 owner/enrollment/course scope 判断。
4. Review scope RED：修 review decision 前置权限检查。
5. Analytics scope RED：修 query service 的 course/enrollment 过滤。
6. Anti-enumeration RED：统一非 admin 的 missing/foreign 响应为 `403 FORBIDDEN + no data + no id echo`；admin missing 保持 `404 NOT_FOUND`。

只读分析完成，未修改文件，也未运行 Maven 测试。
