# PLAN：覆盖迁移 chanceNB/mu 前端

## 执行策略

采用单 Codex 覆盖迁移，限制改动范围为 `frontend/` 与本次流程文档。用户已明确允许覆盖当前前端，因此不再保留当前前端内部旧实现。

## 步骤

1. 创建 L 级流程文档。
2. 将 `chanceNB/main` 的 `frontend` tree checkout 到当前工作区。
3. 删除 `frontend/src` 下不属于 `chanceNB/main` 的残留源文件。
4. 检查覆盖后的前端文件列表和关键入口。
5. 运行前端测试。
6. 运行前端构建。
7. 必要时修复迁移源在当前工具链下的类型或测试问题，但不改变产品范围。
8. 创建 Evidence 和 Acceptance。
9. 更新 changelog 和 memory。

## 文件边界

允许修改：

- `frontend/**`
- `docs/product/PRD-20260621-chancenb-frontend-overwrite.md`
- `docs/requirements/REQ-20260621-chancenb-frontend-overwrite.md`
- `docs/specs/SPEC-20260621-chancenb-frontend-overwrite.md`
- `docs/plans/PLAN-20260621-chancenb-frontend-overwrite.md`
- `docs/tasks/TASK-20260621-chancenb-frontend-overwrite.md`
- `docs/context/CONTEXT-20260621-chancenb-frontend-overwrite.md`
- `docs/evidence/EVIDENCE-20260621-chancenb-frontend-overwrite.md`
- `docs/acceptance/ACCEPT-20260621-chancenb-frontend-overwrite.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`
- `docs/memory/FRONTEND_MEMORY.md`

禁止修改：

- `backend/**`
- `docs/data/**`
- `docs/api/**`
- `docs/architecture/**`
- `package` 管理文件之外的根目录配置
- 任何 secrets 或本地凭据文件

## 测试命令

```powershell
cd frontend
pnpm test -- --run
pnpm build
```

## 依赖审查

不新增依赖。`chanceNB/main` 与当前 `frontend/package.json` 的依赖一致，只是当前项目有额外 workspace scripts；覆盖后按迁移源为准。

## 集成审查

- 确认没有前端 LLM provider 直连。
- 确认没有 API key / token / secret 字符串进入前端源码。
- 确认没有后端/DB/API 改动。
- 确认前端测试和构建结果已记录。

## 停止条件

- 覆盖后出现非前端文件意外改动。
- 测试/构建失败超过两轮仍无法定位。
- 发现必须修改后端 API 或数据库才能让前端编译。
