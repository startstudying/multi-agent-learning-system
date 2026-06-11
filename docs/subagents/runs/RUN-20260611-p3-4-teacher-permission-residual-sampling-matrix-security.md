# RUN-20260611-p3-4-teacher-permission-residual-sampling-matrix-security

## 角色

Security Expert，只读分析。

## 结论

P3-4 当前不是“权限基础缺失”，而是“剩余业务抽样矩阵继续补齐”。只读证据显示课程访问主边界已集中到 `CourseAccessService`，RAG KB、Document、Analytics、Assessment、LearningPath、Resource/Review 已有大量高价值 RBAC / 防枚举 / spoofed header 覆盖。

当前不建议先改生产代码，建议先补最小 MockMvc 抽样测试；若补测失败，再按失败点改 Service 层授权或响应脱敏。

## 已覆盖重点

- 课程访问核心边界已收口：admin 全局；teacher 只能读/管 `teacherId == currentUserId` 的课程；student 只能读 ACTIVE enrollment。
- Analytics 已覆盖教师查看学生摘要必须带 `courseId`、必须是教师自己的课程、且 learner 必须 ACTIVE。
- RAG KB BOUND 权限通过 `CourseAccessService` 做 read/manage；已有 Bearer admin、teacher、subject-name role-confusion 相关覆盖。
- Document upload 已覆盖 teacher foreign/missing course、student spoof course metadata、无副作用、响应不回显对象 id。
- ResourceGeneration 与 Review 已覆盖 course-bound create、DROPPED/no enrollment、teacher 代学生创建拒绝、Review own-course redaction。
- Assessment 已覆盖 foreign `questionId` 提交拒绝、无持久化副作用、响应不泄漏 foreign course/question/requestId。

## 推荐残余抽样

1. `KnowledgeBaseControllerTest`：Bearer `TEACHER` list 只能看到自己可读的 course-bound KB，不看到 foreign teacher 的 BOUND/PUBLIC KB。
2. `DocumentControllerTest`：Bearer `TEACHER` reindex foreign course-bound document 被拒绝，且无 index task 副作用。
3. `AnalyticsControllerTest`：class summary pending reviews 不泄漏 foreign review/resource/task/title。
4. 可后续补：learner-resources foreign/missing safe forbidden。
5. 可后续补：grading evaluation foreign-course + spoofed admin header。

## 是否需要生产代码

只读证据下暂不需要。若新增测试 RED，优先修 Service 层，而不是在 Controller 中散点补权限。

## 建议验证

```powershell
cd D:\多元agent\backend
mvn --% -Dtest=KnowledgeBaseControllerTest,DocumentControllerTest,AnalyticsControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,AssessmentControllerTest test
```

未修改文件，未运行 full test，未使用 `node_repl`。
