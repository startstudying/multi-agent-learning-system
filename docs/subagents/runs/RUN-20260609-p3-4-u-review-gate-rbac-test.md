# RUN - P3-4-U 测试专家报告

## 结论

测试专家建议候选切片为 `CourseAccessService legacy overload` 收口回归测试。

## 关键观察

- `CourseAccessService` 仍保留旧签名，并在旧签名中通过 `admin` / `teacher_*` 进行兼容推断。
- 多个 Controller 主路径已迁移到 Bearer roles-first，但未来新调用者仍可能误用 legacy overload。
- 建议新增 `CourseAccessServiceTest` 锁定 roles-first overload 不回落到 subject-name 推断。

## 集成备注

该建议适合作为后续 P3-4 切片；但本轮 Review Gate 审核发布面仍有 HIGH 风险且未迁移 roles-first，因此本次先做 Review Gate。CourseAccess legacy overload 清零作为后续候选保留。
