# RUN-20260610 P3-4 Evaluation/Review forged-id object-oracle matrix

## 范围

P3-4 子任务：Evaluation/Review forged-id object-oracle matrix。

本轮专家 subagent 负责并行只读审查，不修改文件；主线程负责唯一测试文件实现和最终验收。

## Subagent 调度

- 复用专家线程：`019eb08d-6e19-7370-99da-764ad17b9e62`。
- 并行级别：L1 Parallel Analysis。
- 输出要求：确认测试断言、错误码、无副作用断言方式；如发现生产行为不满足，标 RED 风险。

## 主线程初步结论

已读 `EvaluationRunService`、`EvaluationSetService`、`ReviewGovernanceService`：

- `EvaluationRunService.record(...)` 会先通过 `loadReadableEvaluationSet(...)` 检查 evaluation set 可读性，再保存 `EvaluationRun` 和 metrics；non-admin missing/foreign set 均应安全 `FORBIDDEN`。
- `ReviewGovernanceService.decide(...)` 会先加载 review/task 并执行 `assertCanReviewTask(...)`，teacher 只能处理自己课程的 review；missing/foreign review 对 teacher 均应安全 `FORBIDDEN`。
- 对应 Controller 已从 `UserContext.roles()` 显式传递 `ADMIN` / `TEACHER` role facts，符合 Bearer 优先和 header spoofing 防护要求。

## 建议测试

1. `bearerTeacherCannotRecordRunForForeignEvaluationSetDespiteSpoofedHeaderWithoutSideEffects`
   - 场景：foreign set 由 admin 或其他 teacher 创建，Bearer teacher 使用 spoofed `X-User-Id` 尝试记录 run。
   - 预期：HTTP 403，`code = FORBIDDEN`，无 `data`。
   - 去敏断言：body 不包含 `evaluationSetId`、set code/name、promptVersion、traceId。
   - 副作用断言：`EvaluationRunRepository.count()` 与 `EvaluationRunMetricRepository.count()` 不变。

2. `bearerTeacherCannotDecideForeignReviewDespiteSpoofedHeaderWithoutMutatingReviewState`
   - 场景：foreign review 属于其他 teacher 的 course，Bearer teacher 使用 spoofed `X-User-Id` 尝试 approve。
   - 预期：HTTP 403，`code = FORBIDDEN`，无 `data`。
   - 去敏断言：body 不包含 reviewId、taskId、resourceId、decision summary。
   - 副作用断言：review/resource/task 状态保持原值。

## 专家反馈

状态：等待复用 subagent 返回。若后续反馈发现冲突，以主线程实际测试结果和源码证据为准更新本文件。
