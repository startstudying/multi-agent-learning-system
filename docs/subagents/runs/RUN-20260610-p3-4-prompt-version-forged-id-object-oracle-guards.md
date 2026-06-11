# RUN-20260610 P3-4 PromptVersion forged-id object-oracle guards

## 范围

只读专家分析覆盖 Analytics / Review / Evaluation / PromptVersion 管理接口的 forged-id、missing-vs-foreign oracle、Bearer + spoofed header 组合。

本轮主线程采纳其中 PromptVersion 最小 S 切片。

## 专家结论摘要

PromptVersion 既有覆盖：

- Admin upsert/detail/list。
- Admin missing detail returns `NOT_FOUND`。
- Bearer admin + spoofed header upsert。
- Teacher cannot upsert。
- `USER sub=admin` cannot upsert。
- Teacher read metadata only, no `promptText`。
- Student cannot read detail。
- `USER sub=teacher_1` cannot list。

PromptVersion 建议缺口：

- Bearer admin + spoofed header missing detail `NOT_FOUND` 正向基线。
- Student missing detail should be denied before object lookup.
- Bearer `USER sub=admin` list/detail should not read management data.
- Teacher missing detail follows current authorized-reader spec: `NOT_FOUND` without `promptText` or `data`.

## 主线程集成

新增测试：

- `bearerAdminMissingPromptVersionReturnsNotFoundDespiteSpoofedUserIdHeader`
- `teacherMissingPromptVersionKeepsAuthorizedNotFoundWithoutPromptText`
- `studentMissingPromptVersionReturnsForbiddenWithoutOracle`
- `bearerUserSubjectAdminCannotReadPromptVersionManagementData`

## 验证

- Focused：`mvn --% -Dtest=PromptVersionControllerTest test`，`13 run, 0 failures, 0 errors, 0 skipped`。
- Adjacent：`mvn --% -Dtest=PromptVersionControllerTest,SecurityFilterChainTest test`，`20 run, 0 failures, 0 errors, 0 skipped`。
- Full backend：`mvn test`，`572 run, 0 failures, 0 errors, 1 skipped`。

## 结论

PASS。未发现生产缺陷，未修改生产代码。P3-4 父项仍保持 open，后续继续补 Course/Knowledge forged business-object matrix、Evaluation/Review forged-id matrix、Assessment submit foreign questionId、dev/test legacy fallback cleanup 和 frontend SSE sensitive URL cleanup。
