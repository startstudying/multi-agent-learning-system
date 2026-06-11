# TASK-20260609 P3-4-K 权限渗透测试矩阵补齐

## Status

Done

## Done Criteria

- [x] PRD / REQ / SPEC / PLAN / TASK / CONTEXT 已创建。
- [x] 三份 subagent 报告已落盘。
- [x] 新增或扩展权限矩阵测试覆盖 Bearer roles、student write deny、teacher foreign deny、student foreign learner deny、non-admin anti-enumeration、admin missing semantics。
- [x] 若测试暴露生产缺陷，已完成最小修复并记录 RED/GREEN。
- [x] 不新增依赖。
- [x] 不改 DB schema。
- [x] 不改前端。
- [x] Focused verification 已执行并记录。
- [x] Adjacent regression 已执行并记录。
- [x] Full backend Maven verification 已执行或限制已说明。
- [x] Evidence / Acceptance / Changelog / Memory / backend TODO 已更新。
- [x] Retrospective 已创建；若有可复用模式，完成 skill extraction 判断。

## Implementation Checklist

1. [x] 新增 `PermissionMatrixControllerTest` 或补充相邻测试类。（本切片选择补充相邻测试类。）
2. [x] 补 `DevAuthFilterTest` staging header-only auth。
3. [x] 补 `AnalyticsControllerTest` Bearer roles 驱动 admin-only endpoint 和 token-budget non-admin deny。
4. [x] 补 student 对 course graph 写入 deny 和 dropped course list 不泄露。
5. [x] 补 assessment / resource / review / document 的跨模块最小 anti-enumeration 或引用已有测试并在 matrix 中覆盖关键缺口。
6. [x] 运行 focused tests。
7. [x] 根据 RED 做最小修复或确认测试-only 完成。
8. [x] 运行 adjacent 和 full tests。
9. [x] 更新完成文档。

## Allowed Files

详见 `docs/context/CONTEXT-20260609-p3-4-permission-matrix.md`。

## Test Commands

```powershell
cd backend
mvn --% -Dtest=DevAuthFilterTest,AnalyticsControllerTest,CourseKnowledgeControllerTest,DocumentControllerTest test
mvn --% -Dtest=CourseKnowledgeControllerTest,AssessmentControllerTest,DocumentControllerTest,ResourceGenerationControllerTest,ResourceReviewControllerTest,AnalyticsControllerTest,DevAuthFilterTest test
mvn test
```

## Completion Evidence

见：

- `docs/evidence/EVIDENCE-20260609-p3-4-permission-matrix.md`
- `docs/acceptance/ACCEPT-20260609-p3-4-permission-matrix.md`

验证结果：

```text
Focused:  Tests run: 65,  Failures: 0, Errors: 0, Skipped: 0
Adjacent: Tests run: 119, Failures: 0, Errors: 0, Skipped: 0
Full:     Tests run: 367, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```
