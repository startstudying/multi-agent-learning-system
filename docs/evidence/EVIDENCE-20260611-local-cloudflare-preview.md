# EVIDENCE-20260611-local-cloudflare-preview

## 任务

`TASK-20260611-local-cloudflare-preview`

## 验证时间

2026-06-11 19:39-19:44 Asia/Shanghai。

## 执行记录

### 前端本地启动

命令：

```powershell
cd frontend
$env:VITE_API_BASE_URL='http://localhost:8080'
$env:__VITE_ADDITIONAL_SERVER_ALLOWED_HOSTS='bring-tags-advances-bend.trycloudflare.com'
npm run dev -- --host 0.0.0.0 --port 5173
```

结果：

- Vite 监听 `http://localhost:5173/`
- 本地访问 `http://127.0.0.1:5173/` 返回 `200`
- HTML 长度：`443`

### Cloudflare Tunnel

命令：

```powershell
cloudflared tunnel --url http://localhost:5173 --no-autoupdate
```

结果：

- 临时公网地址：`https://bring-tags-advances-bend.trycloudflare.com`
- Cloudflare precheck：DNS、UDP、TCP、Cloudflare API 均为 PASS
- 公网访问返回 `200`
- HTML 长度：`443`

注意：

- 第一次公网访问返回 `403`，Vite 明确提示 host 未加入 `server.allowedHosts`。
- 未修改 `vite.config.ts`，而是通过临时环境变量 `__VITE_ADDITIONAL_SERVER_ALLOWED_HOSTS` 允许本次 trycloudflare host。

### 后端启动尝试

命令：

```powershell
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

结果：

- Spring Boot 尝试启动到 Tomcat `8080`。
- Flyway 初始化数据库连接失败：

```text
Access denied for user 'learning_os'@'localhost' (using password: YES)
```

- `http://localhost:8080/api/health` 无法连接。
- Docker Desktop 当前不可用，`docker ps` 无法连接 Docker API，因此未能用 `backend/docker-compose.yml` 自动拉起项目 MySQL。

## 架构漂移检查

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 未修改后端代码 |
| Frontend rules | PASS | 未新增前端直连 LLM；仅 Vite 本地预览 |
| Agent / RAG rules | PASS | 未修改 Agent/RAG 流程 |
| Security | PASS | 未暴露数据库、Redis、MinIO；未写入密钥；Tunnel 仅指向前端端口 |
| API / Database | PASS | 未修改 API 合同或数据库 schema |

## Acceptance

| 验收项 | Verdict | Evidence |
|---|---|---|
| 前端本地服务可访问 | PASS | `http://127.0.0.1:5173/` 返回 `200` |
| Cloudflare Tunnel 返回公网 HTTPS URL | PASS | `https://bring-tags-advances-bend.trycloudflare.com` |
| 公网 URL 可访问页面 | PASS | 公网访问返回 `200` |
| 后端限制被记录 | PASS | MySQL `learning_os` 凭据被拒绝，Docker Desktop 不可用 |
| 不新增依赖、不提交密钥、不修改业务代码 | PASS | 仅新增任务/证据文档，并追加 changelog/memory |

## 结论

本次 S 小切片验收通过：前端已通过 Cloudflare 临时隧道暴露到公网。后端因本机 MySQL 凭据不匹配且 Docker Desktop 不可用未启动，真实 API 联动不在本次已完成范围内。
