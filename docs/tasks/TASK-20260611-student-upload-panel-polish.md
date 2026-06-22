# TASK-20260611-student-upload-panel-polish

## 目标

修复学生端知识库上传区仍然像旧卡片、错误提示直接显示 `Failed to fetch` 或乱码的问题，让它符合当前 GPT Web 风格学生工作台。

## 范围

- 优化学生端知识库上传区域的文案、布局和错误态。
- 将网络连接失败错误转换为可读中文提示。
- 保留真实文件上传、课程/章节元数据、文档状态列表和现有 API wrapper。

## 允许修改文件

- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/style.css`
- `frontend/src/api/client.ts`
- `frontend/src/App.spec.ts`
- `docs/evidence/EVIDENCE-20260611-student-upload-panel-polish.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`

## 禁止修改文件

- `backend/**`
- `frontend/src/api/documents.ts`
- `frontend/src/types/api.ts`
- `frontend/package.json`
- lockfiles
- DB migrations

## 测试命令

- `cd frontend && pnpm test -- --run`
- `cd frontend && pnpm build`

## 验收标准

- 上传区不再出现原始 `Failed to fetch`。
- 未选择文件时仍不发起上传，并给出清楚提示。
- 文档列表、失败文档状态、真实上传 FormData 行为保持测试覆盖。
- 前端测试和构建通过。
