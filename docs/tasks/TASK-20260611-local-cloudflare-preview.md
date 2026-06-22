# TASK-20260611-local-cloudflare-preview

## 目标

在本机启动 AI Learning OS 的本地预览，并通过 Cloudflare Tunnel 暴露一个临时公网访问地址，用于快速查看前端效果。

## 任务类型

Docs / config / deployment：本地运行与临时公网预览，不修改业务代码。

## Skill Selection

| Skill | 选择原因 |
|---|---|
| feature-development-workflow | 用户提出“本地部署 + 公网预览”原始需求，必须走 S/M/L 工作流 |
| architecture-drift-check | 确认本次不引入前端直连 LLM、密钥泄露、API/DB 漂移 |
| test-generator | 选择最小可验证命令：前端启动、后端健康检查、Cloudflare 隧道连通性 |
| changelog-writer | 完成后记录本次临时部署证据 |

缺失 Skill：无。

GitHub Research Needed：No。

New Project-Specific Skill：不需要。

## Size Classification

Size：S。

原因：只涉及本地启动与临时 Cloudflare Tunnel 暴露端口，不修改 REST API、DTO、数据库 schema、依赖、前后端合同或 Agent/RAG 流程。

Required Documents：本 mini TASK，完成后补 combined Evidence/Acceptance。

Can Skip：PRD、REQ、SPEC、PLAN、独立 Context Pack、Subagent 报告、独立 ACCEPT。

Upgrade Trigger：如果需要新增部署依赖、修改 Docker/环境配置、引入认证域名或持久化 tunnel，则升级为 M。

## Context Pack

相关记忆与文档：

- `docs/memory/PROJECT_MEMORY.md`
- `docs/skills/SKILL_REGISTRY.md`
- `docs/architecture/ARCHITECTURE_BASELINE.md`
- `docs/architecture/ARCHITECTURE_DRIFT_CHECK.md`
- `docs/harness/TEST_COMMANDS.md`
- `README.md`
- `frontend/README.md`
- `backend/docker-compose.yml`

允许修改文件：

- `docs/tasks/TASK-20260611-local-cloudflare-preview.md`
- `docs/evidence/EVIDENCE-20260611-local-cloudflare-preview.md`
- `docs/changelog/CHANGELOG.md`
- `docs/memory/PROJECT_MEMORY.md`

禁止修改文件：

- `backend/src/**`
- `frontend/src/**`
- `backend/pom.xml`
- `frontend/package.json`
- 数据库迁移文件
- Docker Compose 配置

测试与验证命令：

- `npm run dev -- --host 0.0.0.0`
- `Invoke-WebRequest http://127.0.0.1:<frontend-port>`
- `cloudflared tunnel --url http://localhost:<frontend-port>`
- 可选：`mvn spring-boot:run -Dspring-boot.run.profiles=dev`
- 可选：`Invoke-RestMethod http://localhost:8080/api/health`

当前边界：

- 暴露前端 Vite 预览端口。
- 不暴露数据库、Redis、MinIO 或管理控制台。
- Cloudflare Tunnel 使用临时 trycloudflare URL，不持久化域名和凭据。

## Subagent Decision

Use Subagents：No。

Reason：S 小切片，单机运行与临时隧道，无跨模块实现设计。

Parallelism Level：N/A。

Implementation Mode：Single Codex。

## 验收标准

- 前端本地服务可访问。
- Cloudflare Tunnel 返回公网 HTTPS URL。
- 如后端无法启动，记录原因与已完成的前端预览范围。
- 不新增依赖、不提交密钥、不修改业务代码。
