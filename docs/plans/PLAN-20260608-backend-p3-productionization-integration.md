# PLAN-20260608 后端 P3 生产化集成总控

## 1. 执行顺序

1. [x] 读取项目记忆、技能注册表、subagent 注册表、架构基线与 TODO。
2. [x] 启用专家 subagent 并行分析 P3-2/P3-3/P3-4。
3. [x] 回收 Integration Reviewer 和 Security & Quality 报告。
4. [x] 落盘专家报告。
5. [x] 创建 P3-4-C 当前切片文档与 Context Pack。
6. [x] 按 TDD 执行 P3-4-C：先写失败测试，再做最小实现。
7. [x] 运行聚焦测试和相邻回归。
8. [x] 运行后端全量测试。
9. [x] 创建 Evidence / Acceptance。
10. [x] 更新 Changelog / Memory / TODO。

## 2. 切片计划

| Slice | Scope | Status |
|---|---|---|
| P3-4-C | Course read/graph scope + grading evaluation permission | Done |
| P3-3-B | `model_call_log.provider` schema and logging | Done |
| P3-3-C | Spring AI OpenAI-compatible Chat provider adapter | Done |
| P3-3-D | Spring AI OpenAI-compatible Embedding provider adapter | Done |
| P3-2-D | Real VectorDB adapter | Pending |
| P3-2-E | Parser layout/page hierarchy | Pending |
| P3-2-F | OCR fallback dependency review and adapter | Pending |

## 3. 风险与约束

| Risk | Mitigation |
|---|---|
| 当前目录不是 git 仓库，无法使用 git worktree | 使用当前工作区，严格按 Context Pack 限定文件。 |
| 真实 JWT/RBAC 引入范围过大 | 当前切片只做 Service 层 scope，真实认证另开切片。 |
| Class/enrollment 数据模型不存在 | 当前切片不新增 class 表；先按 teacher-owned course 与 student 无课程读取权限收口。 |
| 评分评估接口历史上无权限 | 当前切片收口为 admin/teacher 可用，student 禁止。 |

## 4. 验证命令

```powershell
cd backend
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,DocumentControllerTest test
mvn test
```

## 5. 验证结果

| 命令 | 结果 |
|---|---|
| `mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest test` | PASS；19 tests，0 failures，0 errors |
| `mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,AnalyticsControllerTest,LearningWorkflowControllerTest,ResourceGenerationControllerTest,DocumentControllerTest test` | PASS；71 tests，0 failures，0 errors |
| `mvn test` | PASS；302 tests，0 failures，0 errors，1 skipped |
