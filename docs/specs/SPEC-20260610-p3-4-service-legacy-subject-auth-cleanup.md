# SPEC-20260610 P3-4 子任务：Service legacy subject-name authorization cleanup

## 1. Scope

本规格覆盖 P3-4 中一个 M 级安全清理子任务：移除服务层遗留 subject-name role inference 入口。

覆盖文件：

- `backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java`
- `backend/src/main/java/com/learningos/assessment/application/AssessmentService.java`
- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java`

测试覆盖文件：

- `backend/src/test/java/com/learningos/knowledge/application/CourseAccessServiceTest.java`
- `backend/src/test/java/com/learningos/assessment/application/AssessmentServiceTest.java`
- `backend/src/test/java/com/learningos/assessment/application/GradingEvaluationServiceTest.java`
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`（仅在需要补充写路径矩阵时修改）

## 2. Current Gap

当前 HTTP Controller 已经在核心路径中读取 `CurrentUserService.currentUser()`，并从 `UserContext.roles()` 派生：

- `currentUserAdmin`
- `currentUserTeacher`

但部分 Service 仍保留 legacy overload，例如：

```java
createCourse(String currentUserId, CreateCourseRequest request)
answerDetail(String currentUserId, String answerId)
evaluate(String currentUserId, GradingEvaluationRequest request)
```

这些 overload 内部继续调用 `isAdmin(currentUserId)` / `isTeacherUser(currentUserId)`，把 subject 字符串当成角色事实。

## 3. Target Contract

Service 授权入口必须显式接收 role facts：

```java
method(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, ...)
```

授权语义：

- `currentUserAdmin == true` 才具有 admin 语义。
- `currentUserTeacher == true` 才具有 teacher 语义。
- `currentUserId = "admin"` 不自动拥有 admin 权限。
- `currentUserId = "teacher"` 或 `teacher_*` 不自动拥有 teacher 权限。

## 4. Method Removal Contract

### 4.1 KnowledgeCatalogService

移除：

```java
Course createCourse(String currentUserId, CreateCourseRequest request)
Course getCourseForUser(String currentUserId, String courseId)
List<Course> listCoursesForUser(String currentUserId)
Chapter createChapter(String currentUserId, String courseId, CreateChapterRequest request)
KnowledgePoint createKnowledgePoint(String currentUserId, CreateKnowledgePointRequest request)
KnowledgeDependency createDependency(String currentUserId, CreateKnowledgeDependencyRequest request)
KnowledgeGraphResponse getKnowledgeGraphForUser(String currentUserId, String courseId)
```

保留：

```java
Course createCourse(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, CreateCourseRequest request)
Course getCourseForUser(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, String courseId)
List<Course> listCoursesForUser(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher)
Chapter createChapter(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, String courseId, CreateChapterRequest request)
KnowledgePoint createKnowledgePoint(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, CreateKnowledgePointRequest request)
KnowledgeDependency createDependency(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, CreateKnowledgeDependencyRequest request)
KnowledgeGraphResponse getKnowledgeGraphForUser(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, String courseId)
```

### 4.2 AssessmentService

移除：

```java
AssessmentPageResponse<AssessmentRecordSummaryResponse> listAnswers(String currentUserId, String learnerId, String courseId, int page, int size)
AssessmentPageResponse<WrongQuestionSummaryResponse> listWrongQuestions(String currentUserId, String learnerId, String courseId, int page, int size)
AssessmentRecordDetailResponse answerDetail(String currentUserId, String answerId)
WrongQuestionDetailResponse wrongQuestionDetail(String currentUserId, String wrongQuestionId)
```

保留：

```java
AssessmentPageResponse<AssessmentRecordSummaryResponse> listAnswers(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, String learnerId, String courseId, int page, int size)
AssessmentPageResponse<WrongQuestionSummaryResponse> listWrongQuestions(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, String learnerId, String courseId, int page, int size)
AssessmentRecordDetailResponse answerDetail(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, String answerId)
WrongQuestionDetailResponse wrongQuestionDetail(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, String wrongQuestionId)
```

### 4.3 GradingEvaluationService

移除：

```java
GradingEvaluationSummary evaluate(String currentUserId, GradingEvaluationRequest request)
```

保留：

```java
GradingEvaluationSummary evaluate(GradingEvaluationRequest request)
GradingEvaluationSummary evaluate(String currentUserId, boolean currentUserAdmin, boolean currentUserTeacher, GradingEvaluationRequest request)
GradingEvaluationSummary evaluate(List<Double> humanScores, List<Double> aiScores, double agreementThreshold)
```

## 5. Architecture Drift Check

| Check | Expected |
|---|---|
| Controller only handles HTTP/current user extraction | PASS |
| Service owns object authorization | PASS |
| Permission in backend code, not Prompt | PASS |
| Runtime role semantics come from explicit role facts | PASS |
| No frontend change | PASS |
| No new dependency | PASS |
| No schema drift | PASS |
| No API contract drift | PASS |

## 6. Test Matrix

| Test | Expected |
|---|---|
| `KnowledgeCatalogService` reflection guard | legacy overload/helper absent |
| `AssessmentService` reflection guard | legacy overload/helper absent |
| `GradingEvaluationService` reflection guard | legacy overload/helper absent |
| Course/Knowledge controller adjacent RBAC | Bearer roles-first behavior unchanged |
| Assessment controller/service adjacent RBAC | answer/wrong-question/grading scope unchanged |
| Full backend | no compile/runtime regression |

## 7. Out of Scope

- 前端生产 SSE client / sensitive URL cleanup。
- dev/test header fallback 全仓清理。
- class/course/answer record 全矩阵新增产品语义。
- DB schema normalization。
- 新 dependency 或正式 IdP discovery 增强。

## 8. 实施状态

已完成并通过验证。

- Compile guard: `mvn --% -q -DskipTests compile`
- Focused: `22 run, 0 failures, 0 errors, 0 skipped`
- Adjacent: `197 run, 0 failures, 0 errors, 0 skipped`
- Full backend: `536 run, 0 failures, 0 errors, 1 skipped`
