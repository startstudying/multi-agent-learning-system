# TASK-20260613-student-ui-design-alignment

## 目标

对齐学生端 UI 与参考设计图：侧栏品牌与导航、聊天主区、右侧上下文面板；将「管理后台」改为「设置」。

## 规模

S（前端 UI 文案与样式，无 API/Schema 变更）

## Context Pack

### 允许修改

- `frontend/src/App.vue`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/style.css`
- `frontend/src/App.spec.ts`

### 禁止修改

- 后端代码
- API 契约

### 验收

- 侧栏显示「AI 学习陪伴系统」、导航为「学习工作台 / 查找中心 / 设置」
- 学生主区具备顶栏搜索、聊天气泡、已深度思考、参考资料、快捷追问与底部输入区
- 右侧上下文面板含学习上下文、回答依据、下一步学习、记忆
- `pnpm test -- --run` 与 `pnpm build` 通过
