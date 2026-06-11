# EVIDENCE-20260611-backend-followup-enhancements-epic

## 范围

Epic F1–F7 + `backend-architecture-todolist.md` 后续增强全部项。

## 验证命令

```bash
cd backend && mvn test
cd frontend && pnpm test
```

## 后端结果

- `mvn test`：**通过**（exit 0）
- 新增/更新测试：`TokenBudgetGateServiceTest`、`BusinessPermissionMatrixRegressionTest`、`ModelProviderExternalSmokeTest`、`QdrantVectorExternalSmokeTest`、`MysqlMigrationSmokeTest` V22 对齐、V22 migration test、Health/Analytics 相关回归

## 前端结果

- `pnpm test`：**31 passed**

## 交付摘要

| 项 | 交付 |
|---|---|
| F1 前端 | `/admin/model-providers` 供应商配置页 + API 客户端 |
| F1 Embedding | registry 默认 embedding provider 路由 |
| F2 smoke | external-smoke opt-in 真实 smoke；默认跳过，显式环境变量时外连验证 |
| F3 Qdrant | health probe + dimension validation + `/api/health.vector` |
| F4 PDF | `RENDERED_PAGE` / TABLE/TOC 启发式 + layout/OCR confidence |
| F5 权限 | teacher/student/admin 矩阵回归测试 |
| F6 告警 | V22 持久化 + webhook opt-in + Admin 真实告警列表 |
| F7 预算 | 调用前 token budget gate + deterministic fallback |

## 验收结论

**PASS** — Epic 后续增强项已按计划落地；外部 provider/Qdrant 真实 smoke 需设置 `MODEL_PROVIDER_SMOKE_ENABLED=true` / `QDRANT_SMOKE_ENABLED=true` 后 opt-in 执行。MySQL migration smoke 已对齐 V22，但仍需 `-Dlearningos.mysql.smoke=true` 和真实 MySQL 环境。

## 剩余风险

- 外部 smoke 依赖真实 API Key / Qdrant 实例，本机未配置时不自动运行。
- PDF 工业级解析为启发式增强，非商业 OCR/layout 引擎全量替换。
- 生产环境需配置 `MODEL_PROVIDER_ENCRYPTION_KEY`、可选 `OPS_ALERT_WEBHOOK_URL`。
