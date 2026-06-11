# REQ-20260610-p3-4-assessment-submit-foreign-questionid

## 1. 背景

P3-4 权限加固过程中，专家 subagent 为 `POST /api/assessment/answers` 补充了 `foreign questionId` 回归测试。测试发现：Bearer `USER sub=alice` 即使只具备 `alice` 身份，也可以提交属于 `bob` foreign course 的 `questionId`，并触发答题、评分、掌握度、错题、学习事件等副作用。

该行为违反课程绑定答题提交的对象级授权规则，必须从 S Fast Lane 升级为 M 修复。

## 2. 任务类型

Bug fix / Backend permission hardening / Assessment submit write-path authorization。

## 3. 需求

1. `POST /api/assessment/answers` 在创建任何业务副作用前，必须校验 `questionId -> knowledgePointId -> KnowledgePoint.courseId -> ACTIVE enrollment`。
2. 若 `questionId` 可解析到已存在 `KnowledgePoint` 且该知识点绑定课程，提交人必须是该课程 ACTIVE enrollment learner。
3. Bearer token 身份优先于 `X-User-Id`，伪造 header 不得提升权限。
4. 越权提交必须返回安全 `FORBIDDEN`，无 `data`，响应不得泄露 foreign `questionId`、course id、知识点标题或 requestId。
5. 越权提交不得新增或修改：
   - `answer_record`
   - `grading_result`
   - `mastery_record`
   - `wrong_question`
   - `learning_event`
6. 保留现有 legacy/template question id 兼容：当 `questionId` 无法解析到已存在 `KnowledgePoint` 时，不在本切片强制拒绝，避免破坏现有 `q_sql_join` 等非课程模板测试/演示路径。
7. 不改变 REST API path、请求/响应 DTO、数据库 schema、依赖、前端或部署配置。

## 4. 非目标

- 不新增 assessment question 表或题库模型。
- 不改变 `AnswerSubmitRequest` / `AnswerSubmitResponse` 合同。
- 不改 Orchestrator `ANSWER_SUBMISSION` workflow 合同。
- 不做全量 dev/test legacy fallback cleanup。
- 不关闭 P3-4 父项。

## 5. 验收标准

- 新增 RED 测试修复后转 GREEN：student Bearer + spoofed header 不能提交 foreign course `questionId`。
- 既有 `q_sql_join` legacy/template 提交流程保持通过。
- Focused / adjacent / full backend 测试按证据记录运行。
