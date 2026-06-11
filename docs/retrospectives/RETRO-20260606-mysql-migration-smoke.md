# Retrospective - MySQL 迁移真实验证

## 1. Feature Summary

完成 P3-1 MySQL 迁移真实验证：补齐 workflow 文档，启用专家 subagent 并行分析，修复真实 MySQL 8 暴露的 V1 `kb_query_log` row-size 问题，扩展 smoke 从 V1-V5 到 V1-V15，并归档测试证据。

## 2. What Went Well

- 真实 MySQL smoke 找到了 H2/text test 无法暴露的 V1 DDL 缺陷。
- 保留 opt-in smoke，普通 `mvn test` 不依赖 Docker/MySQL。
- 专家 subagent 对 V1-V15 范围、Testcontainers 取舍、schema guard 风险形成一致结论。
- Red-Green 过程清晰：先暴露 V1 row-size，再暴露旧 V5 target，再完成 V1-V15。

## 3. What Didn't Go Well

- 本机已有 MySQL84 占用 3306，需要改用 `MYSQL_PORT=3307`。
- PowerShell/Maven 传带 `&` 的 JDBC URL 容易被 shell 解析，后续应优先使用脚本或 `JAVA_TOOL_OPTIONS`。
- 历史 V1 修改有 checksum 风险，需要在 acceptance 中显式记录。

## 4. Skill Extraction Candidates

| Pattern | Reusable? | Proposed Skill File |
|---|---|---|
| MySQL Flyway 空库 smoke：opt-in JUnit + Docker Compose runner + schema guard + `information_schema` 断言 | Yes | 后续可提炼为 `docs/skills/project-specific/mysql-flyway-smoke.md` |

## 5. Process Improvements

| Area | Current | Proposed |
|---|---|---|
| Testing | 普通测试不执行 Flyway | 保留 opt-in smoke，并在发布前手动或 CI 服务化运行。 |
| Documentation | P3-1 原始描述停留在 V1-V5 | 迁移 smoke 完成标准应随当前最新 migration 版本同步。 |
| Operations | Docker 默认端口可能冲突 | 文档中保留 `MYSQL_PORT` + `-JdbcUrl` 示例。 |

## 6. Action Items

| Action | Owner | TASK |
|---|---|---|
| 评估是否把 MySQL smoke 纳入 CI 可选 job | 后续工程质量任务 | P3 后续 |
| 已部署旧 V1 环境如存在，制定 Flyway repair 策略 | 后续运维/DBA | P3 后续 |

## 7. Memory Updates Needed

- [x] PROJECT_MEMORY.md
- [x] BACKEND_MEMORY.md
- [x] DATABASE_MEMORY.md
- [ ] SKILL_REGISTRY.md，本轮暂不新增 skill
- [ ] ARCHITECTURE_BASELINE.md，本轮无架构基线变更
