# RETRO - P3-4-W CourseAccessService legacy overload cleanup

## What Went Well

- 先由专家并行审计确认 legacy overload 已无真实调用残留，再进行删除，避免盲改。
- RED 测试同时覆盖 API 面删除和 roles-first 行为，不只依赖编译错误。
- 编译守卫、adjacent controller 矩阵和 full backend tests 都覆盖到迁移后的调用形态。
- 本切片没有引入依赖、schema、REST contract、frontend 或 formal OAuth2 配置变更。

## What Was Risky

- 删除 public overload 会放大隐藏调用的编译风险；通过 `rg` 和 `mvn -DskipTests compile` 控制。
- 只清理 `CourseAccessService`，不能顺手把其他 service 的 legacy overload 一并删掉，否则切片边界会膨胀。
- P3-4-W 完成后容易误读为 P3-4 全部完成；实际 broader class/course、formal OAuth2/JWK/Spring Security、broader penetration tests 仍未完成。

## Reusable Pattern

后续 legacy authorization API cleanup 可复用：

1. 先审计调用残留：`rg` + compile guard。
2. 用 reflection RED test 锁定旧 public API/helper 不存在。
3. 添加 role-confusion 行为测试：`USER sub=admin` / `USER sub=teacher_1` 不因 subject name 获权。
4. 删除旧 API 后运行 focused、adjacent、full verification。
5. 只更新当前切片 TODO，不把父级大项标完成。

## Skill Extraction Decision

不新增项目技能；现有 `auth-context-boundary`、`object-scope-authorization`、`test-driven-development` 和 `security-review` 已覆盖该模式。

## Follow-up

建议下一步独立处理：

- `LearningWorkflowService.getPathForUser(String currentUserId, String pathId)` subject-name admin 判断。
- broader class/course authorization matrix。
- formal OAuth2/JWK/Spring Security。
- broader permission penetration tests。
