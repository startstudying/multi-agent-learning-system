# CONTEXT：覆盖迁移 chanceNB/mu 前端

## 参考源

- GitHub：`https://github.com/chanceNB/mu.git`
- 本地引用：`chanceNB/main`
- 提交：`20bc8aacb0d8e368df97c8d251adc704713bb0fe`

## 本地仓库状态

- 当前分支：`main`
- 当前 `origin/main`：`bf23b66e4ab320ebfaf43f697b38860bc2ab550f`
- `origin/main` 与 `chanceNB/main` 无共同 `merge-base`，不可按普通分支合并处理。
- 当前工作区有大量未提交改动；用户已允许覆盖前端。

## 迁移源前端关键文件

- `frontend/src/App.vue`
- `frontend/src/router.ts`
- `frontend/src/style.css`
- `frontend/src/components/shell/AppShell.vue`
- `frontend/src/components/shell/LeftSidebar.vue`
- `frontend/src/components/thought/*`
- `frontend/src/components/workspace/*`
- `frontend/src/components/mobbin/*`
- `frontend/src/components/learning/*`
- `frontend/src/pages/chat/NewChatPage.vue`
- `frontend/src/pages/login/StudentLoginPage.vue`
- `frontend/src/pages/login/TeacherLoginPage.vue`
- `frontend/src/pages/student/StudentDashboard.vue`
- `frontend/src/pages/teacher/TeacherReviewQueue.vue`
- `frontend/src/pages/admin/*`
- `frontend/src/stores/session.ts`
- `frontend/src/types/thought.ts`

## 当前项目需注意的残留

当前工作区中 `frontend/src/config`、`frontend/src/composables`、`frontend/src/pages/student/components`、`frontend/src/pages/student/StudentNavHub.vue`、`StudentSearch.vue`、`StudentSettings.vue` 等属于本地未提交前端线。用户已允许覆盖；迁移后这些文件应不再作为当前前端实现保留。

## 测试

```powershell
cd frontend
pnpm test -- --run
pnpm build
```

## 风险边界

- 不修改后端接口，因此若新前端期望 `/api/chat/sessions`，该接口在当前后端未迁移时可能不可用。
- 不新增依赖，因此如迁移源隐含依赖缺失，应优先确认是否已在当前 `package.json` 中存在。
- 不清理项目根部 `.tmp` 或非前端未跟踪文件。
