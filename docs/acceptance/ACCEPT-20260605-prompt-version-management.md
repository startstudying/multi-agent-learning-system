# Acceptance: Prompt Version 管理基础

## 1. 追踪

- PRD: `docs/product/PRD-20260605-prompt-version-management.md`
- REQ: `docs/requirements/REQ-20260605-prompt-version-management.md`
- SPEC: `docs/specs/SPEC-20260605-prompt-version-management.md`
- 证据: `docs/evidence/EVIDENCE-20260605-prompt-version-management.md`

## 2. 功能验收

- [x] `PromptVersion` JPA entity 映射现有 `prompt_version` 表。
- [x] 支持创建 Prompt Version。
- [x] 支持重复 `code/version` upsert 更新原记录。
- [x] 支持按 `code/version` 查询。
- [x] 支持按 `code` 列表查询，未传 `code` 时可列出全部。
- [x] 服务层提供 `findActiveByCode(String code)`，为后续模型调用关联预留。
- [x] 不新增 migration。
- [x] 不修改 `AgentRunRecorder`。

## 3. 非功能验收

- [x] 使用统一 `ApiResponse` envelope。
- [x] 缺失记录返回 `NOT_FOUND` envelope。
- [x] 请求 DTO 使用 validation 注解。
- [x] 状态统一规范化为大写，并限制在 `ACTIVE`、`INACTIVE`、`DRAFT`、`ARCHIVED`。

## 4. 架构验收

- [x] Controller 只调用 Service。
- [x] Service 承载 upsert、查询和校验规则。
- [x] Repository 不包含业务逻辑。
- [x] 未新增依赖。
- [x] 未提交密钥或敏感数据。

## 5. 测试摘要

| 测试项 | 结果 | 备注 |
|---|---|---|
| `PromptVersionControllerTest` | 通过 | 3 tests, 0 failures |
| `PromptVersionServiceTest` | 通过 | 4 tests, 0 failures |
| 用户指定 Maven 命令 | 通过 | `mvn "-Dtest=PromptVersionControllerTest,PromptVersionServiceTest" test` |

## 6. 遗留问题

| 问题 | 严重程度 | 后续 TASK |
|---|---|---|
| evaluation set 未实现 | 中 | 后续 P2-1 子任务：RAG/评分/资源生成实验集管理 |
| `model_call_log` 尚未保存 prompt code/version/model temperature/schema | 中 | 后续 P2-1 子任务：模型调用日志与 Prompt Version 绑定 |

## 7. 验收结论

- [x] 通过
- [ ] 有条件通过
- [ ] 不通过
