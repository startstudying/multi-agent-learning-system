# RUN-20260611-p3-4-teacher-permission-residual-sampling-matrix-architect

## 角色

Backend Integration / Architect，只读分析。

## 结论

`teacher permission residual sampling matrix` 可以作为 S 级 test-only 切片执行。前提是只新增少量高价值权限回归测试和 S mini TASK / Evidence，不改生产代码、不改 API/DTO/schema/dependency、不试图关闭 P3-4 父 epic。

## 涉及模块

- `analytics`：教师班级/学生摘要、active enrollment、DROPPED/no-enrollment、伪造 header、Bearer role facts。
- `knowledge` / `rag`：课程知识、KB/document course metadata 范围。
- `assessment`：answer/wrong-question/grading evaluation 教师 own-course + active learner 范围。
- `agent` / `review` / `resource generation`：review list/detail/decision、resource create/cancel 等残余抽样。
- `common/auth`：只作为已存在行为边界参考，不建议本切片修改。

## Context Pack 建议

允许新增 S mini TASK、Evidence、Changelog/Memory/TODO 子任务更新，以及最多 2-3 个现有 controller test 文件。

禁止修改：

- `backend/src/main/java/**`
- `backend/src/main/resources/db/migration/**`
- `backend/pom.xml`
- `backend/src/main/resources/application*.yml`
- `frontend/**`
- PRD/REQ/SPEC/PLAN 正式文档，除非任务升级
- RAG/vector/model provider/parser 等无关模块

## 文档级别判断

S mini TASK 足够，不需要新 REQ/SPEC/PLAN。若执行中发现需要改生产代码，尤其跨 2 个以上模块修权限实现，应立即停止并升级为 M。

## 集成风险

- 重复测试风险：已有权限矩阵很多，新增测试应抽样缺口，不复制已有断言。
- S 级膨胀风险：一次覆盖 analytics + assessment + rag + agent 全矩阵会变成 M。
- 架构漂移风险低；若把权限逻辑塞进 Controller、恢复 subject-name role inference、或误关闭 P3-4 父 epic，则属于漂移。

未修改文件，未运行测试，未使用 `node_repl`。
