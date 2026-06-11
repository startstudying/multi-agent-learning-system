# PLAN - 权限与安全加固

## 1. 追踪

- PRD：`docs/product/PRD-20260606-permission-security-hardening.md`
- REQ：`docs/requirements/REQ-20260606-permission-security-hardening.md`
- SPEC：`docs/specs/SPEC-20260606-permission-security-hardening.md`
- Context Pack：`docs/context/CONTEXT-20260606-permission-security-hardening.md`

## 2. 实施阶段

| 阶段 | 说明 | 关联任务 | 状态 |
|---|---|---|---|
| 1 | 补齐安全需求文档和 Context Pack。 | TASK-01 | 完成 |
| 2 | 先做 API / service owner 校验与混合 `kbIds` strict 拒绝。 | TASK-01 | 完成 |
| 3 | 补充测试与证据，更新 TODO / memory / changelog。 | TASK-01 | 完成 |

## 3. 文件变更清单

| 文件 | 操作 | 阶段 | 负责人 |
|---|---|---|---|
| `docs/product/PRD-20260606-permission-security-hardening.md` | 新增 | 1 | Main Codex |
| `docs/requirements/REQ-20260606-permission-security-hardening.md` | 新增 | 1 | Main Codex |
| `docs/specs/SPEC-20260606-permission-security-hardening.md` | 新增 | 1 | Main Codex |
| `docs/plans/PLAN-20260606-permission-security-hardening.md` | 新增 | 1 | Main Codex |
| `docs/tasks/TASK-20260606-permission-security-hardening.md` | 新增 | 1 | Main Codex |
| `docs/context/CONTEXT-20260606-permission-security-hardening.md` | 新增 | 1 | Main Codex |
| `backend/src/main/java/com/learningos/learning/**` | 受限修改 | 2 | Main Codex |
| `backend/src/main/java/com/learningos/analytics/**` | 受限修改 | 2 | Main Codex |
| `backend/src/main/java/com/learningos/health/**` | 受限修改 | 2 | Main Codex |
| `backend/src/main/java/com/learningos/rag/**` | 受限修改 | 2 | Main Codex |
| `backend/src/test/java/**` | 受限修改 | 3 | Main Codex |
| `docs/evidence/**` / `docs/acceptance/**` / `docs/retrospectives/**` | 新增 | 3 | Main Codex |
| `docs/subagents/runs/RUN-20260606-permission-security-hardening-*.md` | 新增 | 3 | Main Codex |
| `docs/memory/**` / `docs/changelog/CHANGELOG.md` / `docs/planning/backend-architecture-todolist.md` | 更新 | 3 | Main Codex |

## 4. 依赖

- 前置条件：现有 current-user / permission / analytics / health / RAG 代码。
- 新增依赖：无。

## 5. 风险评估

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| `X-User-Id` dev auth 仍可伪造 | 安全风险未根治 | 本轮只收口高风险接口，并把生产认证列入后续 P3 专项 |
| RAG strict 拒绝影响部分现有调用者 | 可能回归 | 先补测试，保持 explicit 403 |
| Health 输出收敛后调试信息减少 | 运维需要适应 | 公开健康和运维详细视图分层 |

## 6. 回滚策略

如果安全测试表明某个接口无法在当前边界内安全收口，则保留为 P3 安全专项，不强行完成整块改造。

## 7. 测试策略

- owner mismatch/forbidden service tests
- MockMvc 越权 tests
- RAG mixed `kbIds` strict tests
- Health sensitive field tests

## 7.1 验证结果

| 命令 | 结果 |
|---|---|
| `cd backend && mvn "-Dtest=ChatControllerTest" test` | 通过：2 tests, 0 failures, 0 errors |
| `cd backend && mvn "-Dtest=LearningWorkflowControllerTest,LearningWorkflowServiceTest,AnalyticsControllerTest,HealthControllerTest,ChatControllerTest,RagQueryServiceTest" test` | 通过：38 tests, 0 failures, 0 errors |
| `cd backend && mvn test` | 通过：217 tests, 0 failures, 0 errors, 1 skipped |
| `scan-mybatis-dollar.ps1 -ScanDir backend` | 仅命中 Spring `@Scheduled` 属性占位符，非 MyBatis SQL 注入 |
| `find-hardcoded-secrets.ps1 -ScanDir backend` | 脚本自身字符串语法错误，未作为通过证据；已用 `rg` 手工替代扫描 |

## 8. Subagent 计划

| 专家 | 是否需要 | 职责 |
|---|---|---|
| Security & Quality | 是 | 补越权与 health 安全测试 |
| Backend Expert | 是 | owner 校验与 API 收口 |
| Integration Reviewer | 是 | 合并证据与 TODO 状态 |

并行级别：L1 并行分析 / 设计，实施时单 Codex。

## 9. 审批

| 角色 | 日期 | 状态 |
|---|---|---|
| Main Codex | 2026-06-06 | 已执行 |
