# PLAN-20260611 前端角色工作台分离

## 执行步骤

1. 在 `App.vue` 中抽象当前路由对应的 `roleShell`，把业务侧栏导航改为角色专属。
2. 将跨角色切换从侧栏业务导航中分离为清晰的全局 role gateway。
3. 保持 `RouterView` 和现有页面不变，避免扩大改造范围。
4. 更新测试，断言学生、教师、管理员页面不会互相暴露业务侧栏项。
5. 运行 `cd frontend && pnpm test -- --run` 和 `cd frontend && pnpm build`。
6. 写入 Evidence、Acceptance、Changelog 和 Project Memory。

## 架构漂移检查

- 前端仍通过共享 API wrapper 调后端。
- 前端不直接调用 LLM。
- 不新增依赖。
- 不改变后端权限或 API 合同。

## 回滚策略

如视觉壳改造造成路由或测试异常，只回滚 `App.vue` / `style.css` / `App.spec.ts` 相关改动，不影响后端和业务 API。
