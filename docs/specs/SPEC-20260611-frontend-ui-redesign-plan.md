# 前端 UI 修复计划规格

## 设计方向

视觉方向：安静、可信、教育 SaaS 工作台。

关键词：角色任务、证据可展开、少即是多、状态可信、移动优先。

## 外部参考映射

- Open edX Learning MFE：学习页面围绕课程大纲、进度和内容，启发学生端以学习主路径为先。
- LearnHouse：课程、作业、讨论、分析等围绕教学任务组织，启发教师端队列和决策结构。
- Kotaemon：RAG QA 强调 clean/minimal UI，启发答案主区 + sources 证据层。
- assistant-ui：生产级 AI chat 体验，启发消息流、输入区、工具状态分层。
- Vuestic Admin：Vue 管理后台强调响应式、可访问和可维护，启发 Admin triage。

## 文件边界

允许修改：

- `frontend/src/App.vue`
- `frontend/src/style.css`
- `frontend/src/App.spec.ts`
- `frontend/src/api/analytics.ts`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/pages/teacher/TeacherReviewQueue.vue`
- `frontend/src/pages/admin/AdminOperations.vue`
- `frontend/src/pages/admin/AdminModelProviders.vue`
- 可新增 `frontend/src/components/*.vue`

禁止修改：

- `backend/**`
- `frontend/src/api/client.ts`
- `frontend/src/types/api.ts`
- `frontend/src/router.ts`，除非只改 `meta.label` 展示文案
- 数据库 migration
- `frontend/package.json` / lockfile

## 页面规格

### Shell

- 桌面端：
  - 左侧导航宽度可从 292px 收窄或降低视觉密度。
  - `context-rail` 从“首屏大块说明”改为紧凑上下文卡。
- 移动端：
  - 品牌 + 当前身份 + 角色导航合并为顶部区域。
  - `context-rail` 可折叠或移到主内容之后。
  - 主内容在首屏可见，不被侧栏完全推下。

### 学生端

目标结构：

```text
顶部：页面标题 + 主操作
主区：RAG 问答 / 答案 / 引用摘要 / 下一步节点
次区：学习路径详情 / 资源 / 测评 / 画像
底区：Agent Trace / 接口来源 / 状态示例
```

保留 data-test：

- `student-primary-workspace`
- `student-support-workspace`
- `student-diagnostics`
- `no-source-card`
- `status-showcase`

允许调整测试断言文案，但必须保留核心结构测试。

### 教师端

目标结构：

```text
顶部：待审数量 + 刷新
主区：待审队列 + 当前资源审核决策
辅助区：引用/安全/画像检查、历史
```

保留 data-test：

- `teacher-review-workspace`
- `teacher-evidence-checklist`
- `review-detail`
- `review-feedback-input`
- `approve-selected-review`
- `request-revision`
- `reject-review`

### 管理端

目标结构：

```text
顶部：系统状态总览
主区：异常/降级/待处理/健康 triage
次区：依赖矩阵、告警、成本/Token 摘要
底区：API 来源、状态示例
```

要求：

- 不展示像真实数据的假趋势图。
- 待接入指标用空状态或骨架说明。
- 健康状态和告警优先级必须清楚。

### Model Provider

目标：

- 供应商列表和表单仍为主结构。
- 密钥输入说明更清楚：不回显完整 API key。
- 默认供应商、启用状态、连通性结果更醒目。

## 样式规格

- 保留 `--color-primary` 作为主操作色。
- `--color-ai` 仅用于 AI/RAG/Agent 语义，不作为所有强调色。
- 降低阴影数量和卡片层级。
- 卡片圆角保持 8px 左右，不使用大圆角装饰。
- 不新增渐变背景大面积装饰。
- 文本不使用 viewport 宽度缩放。

## 测试规格

必须覆盖：

- 构建通过。
- 学生端首屏结构中 `student-primary-workspace` 仍包含 RAG 问答和引用摘要。
- 教师端 `reject-review` 保持 disabled。
- 管理端不再依赖假图表作为主要信息。
- 移动端关键类名不导致横向滚动，需截图或浏览器检查记录。
