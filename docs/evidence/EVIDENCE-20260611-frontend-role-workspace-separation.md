# EVIDENCE-20260611 前端角色工作台分离

## 变更摘要

- `App.vue` 从单一混合侧栏改为按路由派生 `roleShell`。
- 学生、教师、管理员各自拥有独立的 quick nav、业务导航、上下文摘要和用户身份展示。
- 跨角色入口保留为全局工作台切换，不再混入当前角色业务侧栏。
- `App.spec.ts` 新增角色业务导航互斥断言，覆盖学生、教师、管理员三类入口。

## 验证命令

```bash
cd frontend && pnpm test -- --run
```

结果：33 passed。

```bash
cd frontend && pnpm build
```

结果：`vue-tsc -b && vite build` 成功，生成 `dist/`。

## 架构检查

- 未修改后端代码。
- 未修改 API path、DTO、数据库 schema。
- 未新增依赖。
- 前端仍通过共享 API wrapper 调用后端。
- 未新增前端直连 LLM/provider 行为。

## 限制

`StudentDashboard.vue` 内部仍保留演示默认值和固定 learner/kb/node id，本次只处理角色工作台壳分离。硬编码数据来源治理建议作为后续独立任务处理。
