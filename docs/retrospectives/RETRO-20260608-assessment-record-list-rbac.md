# Retrospective - P3-4-F Assessment Record List RBAC / Pagination

## 1. Feature Summary

新增 assessment answer / wrong-question 分页列表接口，并在 Service 层实现 student / teacher / admin 列表权限矩阵。teacher 列表强制绑定 `courseId`，通过 own-course + active enrollment learner scope 查询；列表响应使用 summary DTO，避免批量暴露 answer 原文、幂等快照和内部诊断 payload。

## 2. What Went Well

- TDD RED 阶段明确证明 list endpoint 缺失，随后最小 GREEN 实现闭环。
- 复用 P3-4-D `CourseAccessService` 和 P3-4-E assessment record scope 模式，权限语义保持一致。
- Subagent 分析提前指出 IDOR、字段泄露和空结果语义风险，最终实现采纳了 teacher 强 `courseId` 和 summary DTO。
- Focused / adjacent / full backend Maven 验证均通过。

## 3. What Didn't Go Well

- `AnswerRecord` 未持久化 `courseId`，answer list course 过滤只能通过 `courseId -> KnowledgePoint -> questionId` 推导。
- summary DTO 最初包含 `gradingResultId`，安全复核后移除，说明列表响应字段需要比详情更保守。
- 当前分页 summary 映射会按 item 查询 latest grading/wrong-question；本切片数据量和范围有限，后续大列表可考虑专用 projection 或 join 查询优化。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| Assessment record list RBAC: student owner、teacher course + active enrollment、admin global、teacher unenrolled learner empty page | Yes | `docs/skills/project-specific/object-scope-authorization.md` |
| 列表 summary DTO 不复用详情 DTO，批量响应比详情更少暴露内部关联字段 | Yes | `docs/skills/project-specific/object-scope-authorization.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Security review | Subagent 报告对字段泄露非常有价值。 | list API 默认先写 response redaction 清单，再写 DTO。 |
| Testing | Matrix 测试集中在 Controller integration test，覆盖直观。 | 后续复杂 list 可补 Service unit/projection tests，降低完整 Spring 测试负担。 |
| Documentation | SPEC 初版字段和最终实现有细微差异。 | GREEN 后立即回写 SPEC 的响应字段，而不是等最后统一补。 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 实现正式 JWT/RBAC，替换 `X-User-Id` 过渡身份。 | Backend | Future P3-4 |
| 为 grading evaluation 增加 course scope 或绑定 evaluation set course scope。 | Backend | Future P3-4 / P2 |
| 评估 assessment 记录是否需要冗余 `courseId` / `knowledgePointId` 以优化大列表 course-scope 查询。 | Backend | Future performance/schema slice |
| 对大型列表引入 projection/join，避免 per-item latest grading/wrong-question 查询。 | Backend | Future performance slice |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] Domain memory file
- [x] API memory file
- [x] SKILL_REGISTRY.md
- [x] Project-specific skill
