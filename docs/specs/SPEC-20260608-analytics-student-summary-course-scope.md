# SPEC-20260608 学生分析摘要课程范围权限收口

## 1. Scope

本规格仅修改 analytics student summary 的授权与课程内聚合范围：

```http
GET /api/analytics/students/{learnerId}/summary?courseId={courseId}
```

## 2. API Behavior

### 2.1 Global Summary

当 `courseId` 缺省：

- student/普通用户：仅允许 `currentUserId == learnerId`。
- admin：允许读取任意 learner。
- teacher：返回 `VALIDATION_ERROR`，要求提供课程范围，避免教师读取学生跨课程全局画像、错因和 mastery。

### 2.2 Course-scoped Summary

当 `courseId` 非空：

- 调用课程授权逻辑确认 actor 是否可读该课程。
- teacher 必须是该 course 的 teacher，并且 learner 必须 active enrolled。
- student 必须是本人且 active enrolled。
- admin 可读取任意 existing course；missing course 返回 `NOT_FOUND`。

返回 DTO 仍使用现有 `StudentAnalyticsSummary`，但字段内容只来自该课程范围。

## 3. Service Contract

新增/调整：

```java
StudentAnalyticsSummary studentSummary(
    String currentUserId,
    boolean currentUserAdmin,
    boolean currentUserTeacher,
    String learnerId,
    String courseId
)
```

保留现有纯算法聚合 helper。旧 `studentSummary(String learnerId)` 可作为兼容 wrapper 或移除调用点后不再使用。

## 4. Course Filtering

当 `courseId` 非空：

```text
courseId
-> CourseAccessService.requireCourseRead(...)
-> KnowledgePointRepository.findByCourseIdOrderByCreatedAtAsc(courseId)
-> courseKnowledgePointIds
-> LearningPath.goalId == courseId
-> LearningPathNode.pathId in scopedPathIds AND knowledgePointId in courseKnowledgePointIds
-> MasteryRecord.knowledgePointId in courseKnowledgePointIds
-> WrongQuestion.knowledgePointId in courseKnowledgePointIds
```

注意：如果课程暂无 knowledge point，则 course-scoped mastery/wrong-question/path-node 聚合为空，不能退回全局数据。

## 5. Security Rules

- teacher missing `courseId` 不得返回全局学生摘要。
- teacher 对 missing course 与 foreign course 使用安全 `FORBIDDEN`。
- teacher 对未报名 learner 使用 `FORBIDDEN`，响应不包含 learner 或 course 细节。
- student 对 foreign learner 使用 `FORBIDDEN`。
- student 对未报名/missing course 使用 `FORBIDDEN`。
- admin 可收到 missing course 的 `NOT_FOUND`。

## 6. Test Plan

Focused tests:

- `studentSummaryTeacherRequiresCourseId`
- `teacherCanReadCourseScopedStudentSummaryForActiveEnrolledLearner`
- `teacherCannotReadStudentSummaryForForeignCourseOrUnenrolledLearner`
- `studentCourseScopedSummaryRejectsUnenrolledCourse`
- `adminCanReadGlobalAndCourseScopedStudentSummary`
- `courseScopedStudentSummaryDoesNotMixSignalsFromOtherCourses`

Adjacent regression:

```powershell
mvn --% -Dtest=AnalyticsControllerTest,CourseKnowledgeControllerTest,AssessmentControllerTest test
```

Full backend:

```powershell
mvn test
```

## 7. Architecture Drift Check

| Check | Expected |
|---|---|
| Controller delegates auth/business decision | PASS |
| Service owns object/course authorization | PASS |
| No frontend direct LLM/API key change | PASS |
| No schema drift | PASS |
| No dependency added | PASS |
