# 教师端班级学习分析需求

## 功能需求

1. 后端提供 `GET /api/analytics/classes/{courseId}/summary`。
2. 请求用户来自 `CurrentUserService.currentUserId()`。
3. 当 `courseId` 不存在时返回 `NOT_FOUND`。
4. 当当前用户不是 `admin` 且不是 `Course.teacherId` 时返回 `FORBIDDEN`。
5. 响应包含：
   - `courseId`
   - `teacherId`
   - `learnerCount`
   - `weakKnowledgePoints`
   - `wrongCauseDistribution`
   - `resourceCompletion`
   - `pendingReviews`
6. 弱知识点按平均掌握度升序、错题数降序输出。
7. 错因分布按出现次数降序输出。
8. 资源完成率基于课程学习者相关 `ResourceGenerationTask` 聚合。
9. 待审核资源只返回 review/resource/task 元数据，不返回 `markdownContent`。

## 权限需求

- `admin` 可以查看任意课程 summary。
- 课程教师可以查看自己负责课程 summary。
- 学生或其他教师不能查看该 summary。

## 数据边界

- 课程范围来自 `Course` 与 `KnowledgePoint.courseId`。
- 学习者集合来自 `LearningPath.goalId == courseId`。
- 弱知识点来自课程学习者的 `LearningPathNode`。
- 错因来自课程学习者的 `WrongQuestion`。
- 资源完成来自课程学习者且 `goalId == courseId` 的 `ResourceGenerationTask`。
- 待审核资源来自上述任务关联的 `ResourceReview` 和 `LearningResource`。

## 验收需求

- 教师查询自己课程返回 200 和聚合数据。
- 其他教师查询返回 403。
- 学生查询返回 403。
- `admin` 查询返回 200。
- 空课程数据返回空数组和 0 值，不抛出 500。
