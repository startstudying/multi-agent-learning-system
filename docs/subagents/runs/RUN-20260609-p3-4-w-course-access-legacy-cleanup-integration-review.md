# RUN - P3-4-W Integration Review

## 输入

- Architect report
- Security Reviewer report
- Test Engineer report
- 本地 `rg` 审计结果

## 合并结论

P3-4-W 应选择最小实现：

1. 新增服务层测试，先 RED 证明 legacy public overload 和 subject-name helper 仍存在。
2. 删除 `CourseAccessService` 中 4 个 legacy overload。
3. 删除 `scopedCourseMissing(String)`、`isAdmin(String)`、`isTeacherUser(String)`。
4. 保持 roles-first overload 行为不变。
5. 不修改其他业务服务 legacy overload。

## 冲突解决

Test Engineer 提出较宽 HTTP 回归矩阵；Architect/Security 均指出当前 HTTP 主路径已基本迁移完成。最终采用：

- Focused：`CourseAccessServiceTest`
- Compile guard：`mvn --% -DskipTests compile`
- Adjacent：覆盖 Course、Analytics、Assessment、LearningPath、ResourceGeneration、Orchestrator、Document controller
- Full：`mvn test`

## 风险接受

- `LearningWorkflowService.getPathForUser` 本地 `admin` subject-name 判断是真风险，但不是 `CourseAccessService` 调用残留，记录为后续独立切片。
- `KnowledgeCatalogService` 自身 legacy overload 暂不删除，避免扩大到知识目录服务 API 面。

## Verdict

PASS for implementation. 建议由 Main Codex 单线实现，防止多 agent 修改同一文件。
