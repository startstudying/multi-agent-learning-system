# CONTEXT-20260611 前端角色工作台分离

## 当前任务边界

本任务只处理全局工作台壳的角色分离：当前路由决定当前角色的侧栏导航、上下文摘要和用户身份。业务页面内部数据硬编码、学生页组件拆分、后端权限控制不在本次范围内。

## 现有证据

- `frontend/src/router.ts` 已有 `/`、`/teacher/reviews`、`/admin/operations`、`/admin/model-providers`。
- `frontend/src/App.vue` 当前把跨角色切换、项目、聊天、上下文摘要放在同一个侧栏中。
- `frontend/src/api/*` 已通过共享 API wrapper 调后端，不是完全静态前端。

## 实施约束

- 只修改 Context Pack 允许文件。
- 不新增依赖。
- 不改变 API 调用路径。
- 不把权限控制放到前端文案中。

## 验证

- 前端单元测试。
- 前端生产构建。
