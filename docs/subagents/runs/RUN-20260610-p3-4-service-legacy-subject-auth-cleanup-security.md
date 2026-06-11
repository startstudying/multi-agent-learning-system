# RUN-20260610 P3-4 Service legacy subject auth cleanup - Security Review

## Scope

只读审查：

- `KnowledgeCatalogService`
- `AssessmentService`
- `GradingEvaluationService`

## Risk Level

MEDIUM

## Conclusion

可作为 M 级单任务推进。

当前 HTTP Controller 已传入 `UserContext.roles()` 派生的显式角色事实，主要剩余风险是 Service 层 legacy overload 仍可被未来内部调用误用，重新引入 `sub=admin` / `sub=teacher_1` 提权。

## 必须移除

### KnowledgeCatalogService

- `createCourse(String currentUserId, CreateCourseRequest request)`
- `getCourseForUser(String currentUserId, String courseId)`
- `listCoursesForUser(String currentUserId)`
- `createChapter(String currentUserId, String courseId, CreateChapterRequest request)`
- `createKnowledgePoint(String currentUserId, CreateKnowledgePointRequest request)`
- `createDependency(String currentUserId, CreateKnowledgeDependencyRequest request)`
- `getKnowledgeGraphForUser(String currentUserId, String courseId)`
- `resolveCourseTeacherId(String, String)`
- `requireCourseTeacherOrAdmin(String, Course)`
- `requireCourseManageAccess(String, Course)`
- `requireCourseReadAccess(String, Course)`
- `scopedCourseMissing(String)`
- `isAdmin(String)`
- `isTeacherUser(String)`

### AssessmentService

- `listAnswers(String currentUserId, String learnerId, String courseId, int page, int size)`
- `listWrongQuestions(String currentUserId, String learnerId, String courseId, int page, int size)`
- `answerDetail(String currentUserId, String answerId)`
- `wrongQuestionDetail(String currentUserId, String wrongQuestionId)`
- `isAdmin(String)`
- `isTeacherUser(String)`

### GradingEvaluationService

- `evaluate(String currentUserId, GradingEvaluationRequest request)`
- `isAdmin(String)`
- `isTeacherUser(String)`

## 必须保留

- 所有带显式 `boolean currentUserAdmin, boolean currentUserTeacher` 的 Service overload。
- `AssessmentService.submitAnswer(...)`、`submitAnswerWithTraceId(...)`、`replayAnswerIfPresent(...)`。
- `GradingEvaluationService.evaluate(GradingEvaluationRequest)` 与 `evaluate(List<Double>, List<Double>, double)`。
- `KnowledgeCatalogService.getCourse(...)` 与 `listCourses(...)`，本任务不收口 raw read API。

## 安全边界

主要风险属于 Broken Access Control：legacy overload 会把 `currentUserId == "admin"`、`currentUserId == "teacher"`、`currentUserId.startsWith("teacher_")` 当成权限事实。若未来 Orchestrator、scheduler、测试工具或新 Controller 误调这些入口，Bearer `roles=["USER"]` 但 `sub="admin"` / `sub="teacher_1"` 的用户可能获得管理员或教师能力。

本任务不改 REST API、DTO、DB schema、依赖、前端，也不改变 dev/test auth fallback 全局策略。

## 建议验证

```powershell
rg -n "isAdmin\(String|isTeacherUser\(String|startsWith\(\"" backend/src/main/java/com/learningos/knowledge/application/KnowledgeCatalogService.java backend/src/main/java/com/learningos/assessment/application/AssessmentService.java backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java
mvn -f backend/pom.xml "-Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,GradingEvaluationServiceTest,AssessmentServiceTest" test
mvn -f backend/pom.xml test
```

## M 级判断

可以作为 M 级单任务推进。理由：涉及 3 个后端 Service 和相关测试，属于安全边界清理；不需要 API/DB/依赖/前端变更；实现风险集中在内部调用编译失败和测试更新。
