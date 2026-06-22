# SPEC-20260611 前端角色工作台分离

## 现状证据

- `frontend/src/router.ts` 已有角色路由，但没有角色级 layout。
- `frontend/src/App.vue` 根据 `route.name` 切换文案，却始终使用同一个 sidebar、同一组 quick nav、同一组项目/聊天入口。
- `StudentDashboard.vue` 包含大量学生业务默认状态，本次不处理内部硬编码，只约束角色入口边界。

## 设计

### 角色壳模型

在 `App.vue` 中建立 `roleShell` 派生模型：

- `student`：学习工作台，主导航只包含学习、课程资料、学习记录、错题练习等学生语义。
- `teacher`：教师审核台，主导航只包含审核队列、引用检查、发布闸口、班级反馈等教师语义。
- `admin`：管理后台，主导航只包含运维总览、模型供应商、告警、Trace 等管理语义。

### 导航边界

- 业务侧栏只显示当前角色自己的导航。
- 顶部角色入口用于切换工作台，承担全局切换职责。
- 管理员下保留 `/admin/model-providers` 二级入口。

### API 和安全边界

- 不改变 `apiRequest`、`adminApiRequest`、`openSse`、`streamRequest`。
- 不改变前端调用的 API path。
- 不在前端新增任何 LLM/provider 直接调用。

## 文件影响

- 修改 `frontend/src/App.vue`。
- 按需修改 `frontend/src/style.css`。
- 修改 `frontend/src/App.spec.ts`，覆盖角色壳分离。

## 风险

- 现有测试可能依赖旧的跨角色侧栏文案，需要调整为角色边界断言。
- 单文件 `StudentDashboard.vue` 仍有演示默认值，后续应单独治理数据来源。
