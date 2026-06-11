# RUN-20260609 P3-4 子任务：Analytics teacherClassSummary legacy cleanup - Integration Review

## 集成结论

PASS。

## 合并后的决策

- 采用 Architect 建议：删除服务层两参 legacy overload 和 subject-name teacher helper。
- 采用 Security 建议：删除 `courseAccessService == null` subject-name 授权 fallback，并让缺少 `CourseAccessService` 时的 class learner set fail-closed。
- 采用 Test Engineer 建议：新增 `AnalyticsServiceTest`，用 reflection guard 和 behavior regression 防止 legacy surface 回潮。

## 代码边界核对

- 修改范围限制在 `AnalyticsService` 与新增 `AnalyticsServiceTest`。
- 未修改 `AnalyticsController`、`CourseAccessService`、API DTO、schema、dependency、frontend 或正式认证配置。
- Parent P3-4 仍未完成，broader class/course authorization matrix、formal OAuth2/JWK/Spring Security 和 broader permission penetration tests 仍是后续任务。

