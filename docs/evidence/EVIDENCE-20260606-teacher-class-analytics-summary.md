# 教师端班级学习分析证据

## 1. 追踪

- TASK：`docs/tasks/TASK-20260606-teacher-class-analytics-summary.md`
- SPEC：`docs/specs/SPEC-20260606-teacher-class-analytics-summary.md`
- 日期：2026-06-06

## 2. 实现内容

本次新增 `GET /api/analytics/classes/{courseId}/summary`，用于教师端查看课程维度学习分析。接口返回课程学习者数量、薄弱知识点、错因分布、资源完成率和待审核资源元数据。权限由后端检查：`admin` 可查看全部课程，课程教师只能查看 `Course.teacherId` 匹配的课程，学生和其他教师返回 `403 FORBIDDEN`。

## 3. 变更文件

| 文件 | 操作 | 摘要 |
|---|---|---|
| `backend/src/main/java/com/learningos/analytics/api/AnalyticsController.java` | 修改 | 新增 class summary endpoint |
| `backend/src/main/java/com/learningos/analytics/application/AnalyticsService.java` | 修改 | 新增教师课程 summary 聚合、权限校验和响应 record |
| `backend/src/test/java/com/learningos/analytics/api/AnalyticsControllerTest.java` | 修改 | 新增教师、学生、admin、空数据、缺失课程测试 |
| `docs/product/PRD-20260606-teacher-class-analytics-summary.md` | 新增 | 产品目标和非目标 |
| `docs/requirements/REQ-20260606-teacher-class-analytics-summary.md` | 新增 | 功能、权限、数据边界需求 |
| `docs/specs/SPEC-20260606-teacher-class-analytics-summary.md` | 新增 | API 和聚合规则 |
| `docs/plans/PLAN-20260606-teacher-class-analytics-summary.md` | 新增 | 执行计划和风险 |
| `docs/tasks/TASK-20260606-teacher-class-analytics-summary.md` | 新增 | 任务拆解 |
| `docs/context/CONTEXT-20260606-teacher-class-analytics-summary.md` | 新增 | 修改边界和测试命令 |
| `docs/subagents/runs/RUN-20260606-teacher-class-analytics-summary.md` | 新增 | 只读 subagent 分析集成记录 |

## 4. 测试结果

### 执行的命令

```powershell
cd backend
mvn "-Dtest=AnalyticsControllerTest" test
```

RED 结果：失败，4 个新增测试触发 `/api/analytics/classes/{courseId}/summary` 未实现，响应为 `500 INTERNAL_ERROR`，handler 为 `ResourceHttpRequestHandler`。

```powershell
cd backend
mvn "-Dtest=AnalyticsControllerTest" test
```

GREEN 结果：`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。

```powershell
cd backend
mvn "-Dtest=AnalyticsControllerTest#teacherClassSummaryReturnsNotFoundForMissingCourse" test
```

Mutation check：临时把缺失课程分支从 `NOT_FOUND` 改为 `FORBIDDEN` 后，测试失败，实际状态 `403`，预期 `404`。随后已恢复生产代码。

```powershell
cd backend
mvn "-Dtest=AnalyticsControllerTest,ResourceReviewControllerTest,ReviewGovernanceServiceTest" test
```

最终结果：`Tests run: 23, Failures: 0, Errors: 0, Skipped: 0`。

## 5. 架构漂移检查

- [x] Controller 只处理 HTTP 路由和当前用户传递。
- [x] Service 承担课程存在性、权限和聚合逻辑。
- [x] 没有新增数据库 migration。
- [x] 没有新增依赖。
- [x] 没有新增 LLM 或 Agent 执行链路。
- [x] 待审核资源只返回元数据，不返回 `markdownContent`。

## 6. 截图 / 日志

无前端变更。测试日志保留在 Maven 输出和 `backend/target/surefire-reports`。

## 7. 已知限制

- 当前没有真实 `Class`、`Enrollment` 或 `ClassMember` 表，课程学习者集合暂由 `LearningPath.goalId == courseId` 推断。
- MVP 聚合使用现有 repository `findAll()` 后内存过滤；后续数据量增大时应增加专用 repository 查询。
- 临时权限模型仍基于 `X-User-Id` 和 `Course.teacherId`，完整 RBAC 留给 P3 权限任务。

## 8. 审计备注

本切片不处理全局教师审核权限问题，只给 analytics class summary 增加课程级访问控制。
