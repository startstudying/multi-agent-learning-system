# RUN-20260610 P3-4 Course/Knowledge forged business-object HTTP matrix

## 范围

P3-4 子任务：Course/Knowledge forged business-object HTTP matrix。

本轮专家 subagent 负责并行只读审查，不修改文件；主线程负责唯一测试文件实现和最终验收。

## Subagent 调度

- 新建专家失败原因：当前 subagent 线程达到上限。
- 复用专家线程：`019eb022-0a2c-7ac0-8d64-0ca3ebd2a81e`。
- 并行级别：L1 Parallel Analysis。
- 输出要求：确认测试断言、错误码、无副作用断言方式；如发现生产行为不满足，标 RED 风险。

## 主线程初步结论

已读 `KnowledgeCatalogService` 和 `KnowledgePointController`：

- `createKnowledgePoint(...)` 会先执行 `validateCourseAndChapter(...)`，当请求 `courseId` 与 `chapterId.courseId` 不一致时抛出 `VALIDATION_ERROR`，消息为 `Chapter does not belong to course`。
- `createDependency(...)` 会加载目标知识点与前置知识点；当二者 `courseId` 均存在且不一致时抛出 `VALIDATION_ERROR`，消息为 `Knowledge dependency must stay within one course`。
- `KnowledgePointController` 已从 `UserContext.roles()` 显式传递 `ADMIN` / `TEACHER` role facts，符合 Bearer 优先和 header spoofing 防护要求。

## 建议测试

1. `knowledgePointRejectsForgedForeignChapterIdForAuthorizedWritersWithoutSideEffects`
   - 场景：已授权教师或管理员提交 own `courseId` + foreign `chapterId` 创建知识点。
   - 预期：HTTP 400，`code = VALIDATION_ERROR`，无 `data`。
   - 去敏断言：body 不包含 own/foreign course id、foreign chapter id、foreign course/chapter title、请求知识点标题。
   - 副作用断言：`KnowledgePointRepository.count()` 不变。

2. `knowledgeDependencyRejectsCrossCourseForgedPrerequisiteWithoutSideEffects`
   - 场景：已授权教师提交本课程 `knowledgePointId` + 其他课程 `prerequisiteId` 创建依赖。
   - 预期：HTTP 400，`code = VALIDATION_ERROR`，无 `data`。
   - 去敏断言：body 不包含两边 knowledge point id、course id、知识点标题。
   - 副作用断言：`KnowledgeDependencyRepository.count()` 不变；可附加 graph `dependencies.length() == 0`。

## 专家反馈

复用专家线程已返回，只读审查结论如下：

- 两个测试设计成立，适合作为 S Fast Lane 的 test-only HTTP 回归矩阵。
- 现有生产代码在持久化前已有校验，未发现必须升级 M 的 RED 风险。
- `own courseId + forged foreign chapterId` 创建 knowledge point 预期 `400 BAD_REQUEST` / `VALIDATION_ERROR` / 无 `data`。
- 跨课程 forged `knowledgePointId + prerequisiteId` 创建 dependency 预期 `400 BAD_REQUEST` / `VALIDATION_ERROR` / 无 `data`。
- 建议注入 `KnowledgePointRepository` / `KnowledgeDependencyRepository` 做 `count()` 无副作用断言。

## 主线程集成结果

新增测试：

- `knowledgePointRejectsForgedForeignChapterIdForAuthorizedWritersWithoutSideEffects`
- `knowledgeDependencyRejectsCrossCourseForgedPrerequisiteWithoutSideEffects`

主线程按专家建议集成了 repository 副作用断言，并额外对 dependency 失败后的 course graph 依赖数量做了 HTTP 验证。

## 验证

- Focused：`mvn --% -Dtest=CourseKnowledgeControllerTest test`，`29 run, 0 failures, 0 errors, 0 skipped`。
- Adjacent：`mvn --% -Dtest=CourseKnowledgeControllerTest,CourseAccessServiceTest test`，`35 run, 0 failures, 0 errors, 0 skipped`。
- Full backend：`mvn test`，`574 run, 0 failures, 0 errors, 1 skipped`。

## 结论

PASS。Course/Knowledge forged business-object HTTP matrix 已补齐；无生产代码、API、DTO、DB schema、依赖、部署或前端变更。P3-4 父项继续 open。
