# RUN-20260609 P3-4-N 后端专家报告捕获

## 来源说明

后端架构 subagent `019eaaec-9165-7bc1-b179-34b845d3e877` 已通过工具通知返回只读分析，但未直接写入文件。原因是该 subagent 报告中声明本轮身份约束为只读分析，不能修改文件。

本文件由 Main Codex 按通知内容摘要捕获，用于满足 P3-4-N 多专家输出可追踪性；不伪装为 subagent 原始落盘产物。

## 推荐切片

后端专家推荐下一刀做：

```text
P3-4-N: POST /api/assessment/grading-evaluations roles-first 局部迁移
```

## 主要理由

- 它是 P3-4-G grading evaluation course scope 的自然延续。
- 修改面小：`AssessmentController`、`GradingEvaluationService`、`AssessmentControllerTest`。
- 可复用 P3-4-M 新增的 `CourseAccessService` roles-first overload。
- 不需要新增依赖、DB schema、frontend 或 formal OAuth2/JWK/Spring Security。
- 能补上 Bearer `ADMIN sub=ops_admin`、Bearer `TEACHER sub=instructor_1` 这类非 legacy subject 在 grading evaluation 路径上的回归风险。

## 后端专家指出的证据

- `backend/src/main/java/com/learningos/assessment/api/AssessmentController.java`：grading evaluation Controller 仍只传 `currentUserId`。
- `backend/src/main/java/com/learningos/assessment/application/GradingEvaluationService.java`：仍使用 `"admin"` / `"teacher"` / `teacher_` 字符串推断角色。
- `backend/src/main/java/com/learningos/knowledge/application/CourseAccessService.java`：已有 roles-first course read/manage/list overload。
- `backend/src/test/java/com/learningos/knowledge/api/CourseKnowledgeControllerTest.java`：已有 Bearer roles-first 测试模式可复用。

## 取舍

后端专家将 GradingEvaluation 排为最高优先级，认为 PromptVersion、Evaluation Set/Run、RAG Document、formal OAuth2/JWK/Spring Security 均应后置。

Main Codex 在集成评审中未采纳该排序作为 P3-4-N 的最终选择；原因见 `RUN-20260609-p3-4-n-integration-review.md`。

