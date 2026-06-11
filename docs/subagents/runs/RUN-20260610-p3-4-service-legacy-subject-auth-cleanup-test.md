# RUN-20260610 P3-4 Service legacy subject auth cleanup - Test Review

## Scope

只读审查，未改文件，未运行测试。

## Task Gate

- 任务类型：M 级服务层授权 cleanup 的测试覆盖审查
- 选用技能：`auth-context-boundary`、`object-scope-authorization`、`test-generator`、`architecture-drift-check`
- GitHub research：不需要，项目内 P3-4 已有 roles-first RBAC 测试模式

## 最小必需回归测试 / 反射守卫

建议新增或扩展 focused 反射守卫，覆盖三个目标服务。

### KnowledgeCatalogService

断言不再声明 legacy public overload：

- `createCourse(String, CreateCourseRequest)`
- `getCourseForUser(String, String)`
- `listCoursesForUser(String)`
- `createChapter(String, String, CreateChapterRequest)`
- `createKnowledgePoint(String, CreateKnowledgePointRequest)`
- `createDependency(String, CreateKnowledgeDependencyRequest)`
- `getKnowledgeGraphForUser(String, String)`

断言不再声明 subject-name helper：

- `isAdmin(String)`
- `isTeacherUser(String)`
- `scopedCourseMissing(String)`
- `resolveCourseTeacherId(String, String)`
- `requireCourseTeacherOrAdmin(String, Course)`
- `requireCourseManageAccess(String, Course)`
- `requireCourseReadAccess(String, Course)`

### AssessmentService

断言不再声明 legacy public overload：

- `listAnswers(String, String, String, int, int)`
- `listWrongQuestions(String, String, String, int, int)`
- `answerDetail(String, String)`
- `wrongQuestionDetail(String, String)`

断言不再声明：

- `isAdmin(String)`
- `isTeacherUser(String)`

### GradingEvaluationService

断言不再声明 authorization legacy overload：

- `evaluate(String, GradingEvaluationRequest)`

保留算法纯函数：

- `evaluate(GradingEvaluationRequest)`
- `evaluate(List<Double>, List<Double>, double)`

断言不再声明：

- `isAdmin(String)`
- `isTeacherUser(String)`

## 邻接测试矩阵

| 层级 | 测试类 | 必跑原因 |
|---|---|---|
| Focused reflection | service cleanup guard | 防止 legacy overload/helper 重新出现 |
| Knowledge HTTP | `CourseKnowledgeControllerTest` | Bearer admin、teacher no-prefix、spoofed header、`USER sub=admin/teacher_1` role confusion |
| Assessment HTTP | `AssessmentControllerTest` | answer/wrong-question detail/list roles-first 与 role-confusion denial |
| Grading HTTP | `AssessmentControllerTest` | grading evaluation admin/teacher roles-first、subject-name denial、course scope |
| Pure grading metrics | `GradingEvaluationServiceTest` | 删除 auth overload 不影响离线指标算法 |
| Assessment idempotency | `AssessmentServiceTest` | submit/replay 路径不应受 list/detail overload cleanup 影响 |
| CourseAccess baseline | `CourseAccessServiceTest` | 已有反射守卫模式，可复用其实现风格 |

## 可能受影响点

- `CourseKnowledgeControllerTest` 的 dev/test header 兼容用例仍使用 `X-User-Id=admin` / `teacher_1`；删除 service legacy overload 不应破坏 controller roles-first 调用。
- `AssessmentControllerTest` 中 answer/wrong-question list/detail 已有 Bearer roles-first 用例；删除旧签名后主要风险是 controller 或测试 helper 仍调用 legacy 方法。
- `GradingEvaluationServiceTest` 当前只测纯算法；cleanup 只应删除 `evaluate(String, request)` 授权旧入口。
- `AssessmentServiceTest` 当前不覆盖 read/list RBAC，只覆盖提交幂等；一般不应受影响，除非构造器或依赖 mock 被顺手改动。

## Commands

Focused：

```powershell
cd backend
mvn test -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,GradingEvaluationServiceTest,AssessmentServiceTest,CourseAccessServiceTest
```

Compile guard：

```powershell
cd backend
mvn -q -DskipTests compile
```

Full backend：

```powershell
cd backend
mvn test
```

前端/API/DB/依赖不在本子任务范围内，不建议跑 frontend 或 migration smoke，除非实现时实际触碰了合同、schema 或依赖。
