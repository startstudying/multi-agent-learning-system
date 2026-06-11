# EVIDENCE - P3-4-W CourseAccessService legacy overload cleanup

## 1. Scope

本证据覆盖 `CourseAccessService` legacy authorization API 面收口：

- 删除 subject-name role inference public overload：
  - `requireCourseRead(String currentUserId, String courseId)`
  - `requireCourseManage(String currentUserId, Course course)`
  - `requireLearnerEnrolledForExistingCourse(String currentUserId, String learnerId, String courseId)`
  - `listCoursesForUser(String currentUserId)`
- 删除 subject-name helper：
  - `scopedCourseMissing(String currentUserId)`
  - `isAdmin(String currentUserId)`
  - `isTeacherUser(String currentUserId)`
- 保留并验证 roles-first overload。

## 2. RED Evidence

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseAccessServiceTest test
```

结果：

```text
Tests run: 4, Failures: 2, Errors: 0, Skipped: 0
```

关键失败形态：

- reflection 发现 `requireCourseRead(String, String)` 仍存在。
- reflection 发现 `scopedCourseMissing(String)` 仍存在。

## 3. Implementation Evidence

改动摘要：

- `CourseAccessService` 删除 4 个 legacy public overload。
- `CourseAccessService` 删除 `scopedCourseMissing(String)`、`isAdmin(String)`、`isTeacherUser(String)`。
- 新增 `CourseAccessServiceTest`：
  - 用 reflection 锁定 legacy overload/helper 不存在。
  - 验证 `currentUserId = "admin"` 但 `currentUserAdmin = false` 不获得 admin 语义。
  - 验证 `currentUserId = "teacher_1"` 但 `currentUserTeacher = false` 不获得 teacher manage 语义。

## 4. Verification Evidence

### Focused GREEN

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseAccessServiceTest test
```

结果：

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Compile Guard

命令：

```powershell
cd D:\多元agent\backend
mvn --% -DskipTests compile
```

结果：

```text
BUILD SUCCESS
```

### Adjacent Regression

命令：

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=CourseAccessServiceTest,CourseKnowledgeControllerTest,AnalyticsControllerTest,AssessmentControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,OrchestratorWorkflowControllerTest,DocumentControllerTest test
```

结果：

```text
Tests run: 183, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Full Backend

命令：

```powershell
cd D:\多元agent\backend
mvn test
```

结果：

```text
Tests run: 467, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

### Static Guard

命令：

```powershell
rg -n "public .*requireCourseRead\(String currentUserId, String courseId\)|public .*requireCourseManage\(String currentUserId, Course course\)|public .*requireLearnerEnrolledForExistingCourse\(String currentUserId, String learnerId, String courseId\)|public .*listCoursesForUser\(String currentUserId\)|private .*isAdmin\(|private .*isTeacherUser\(|scopedCourseMissing\(String currentUserId\)" "D:\多元agent\backend\src\main\java\com\learningos\knowledge\application\CourseAccessService.java"
```

结果：

```text
No matches.
```

调用点检查：

```powershell
rg -n "courseAccessService\.(requireCourseRead|requireCourseManage|requireLearnerEnrolledForExistingCourse|listCoursesForUser)\(" "D:\多元agent\backend\src\main\java" "D:\多元agent\backend\src\test\java"
```

结果摘要：

- 现有生产调用均为 roles-first 参数形态。
- 新增测试中的调用也是 roles-first 参数形态。

## 5. Limitations

- 未执行真实 MySQL migration smoke；本切片无 DB migration。
- 未执行 frontend build；本切片无 frontend 变更。
- 未引入 formal OAuth2/JWK/Spring Security；仍属后续 P3-4 工作。
- 未清理其他 service 的 legacy overload；本切片只收口 `CourseAccessService`。
