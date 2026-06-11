# PLAN-20260608 P3-4-I 真实认证上下文与 RBAC 兼容层

## 1. Skill Selection Report

### Task Type

Security hardening / authentication context / RBAC compatibility.

### Selected Skills

| Skill | Why Needed |
|---|---|
| feature-development-workflow | 项目强制 Spec-First 开发流水线。 |
| multi-agent-coder | 用户要求专家 subagent 并行开发，P3 剩余项涉及多个独立分析面。 |
| dispatching-parallel-agents | P3-2 / P3-3 / P3-4 可并行分析。 |
| security-review | 认证、RBAC、token、header spoofing 属于高风险安全边界。 |
| spring-boot-architecture | Filter / CurrentUserService / Controller-Service 分层。 |
| object-scope-authorization | 保持 P3-4-C..H 对象级授权矩阵不回退。 |
| test-driven-development | 先写 RED 认证层测试。 |
| verification-before-completion | 完成前用真实测试输出证明。 |

### Missing Skills

无。

### GitHub Research Needed

No。本切片不新增依赖，使用 JDK HMAC-SHA256 作为无依赖过渡 JWT 验签能力；后续 OAuth2/JWK/Spring Security 另开依赖审查切片。

### New Project-Specific Skill Created

`docs/skills/project-specific/auth-context-boundary.md`

## 2. Subagent Decision

- Use Subagents: Yes
- Reason: 总目标横跨 RAG parser、model provider、security/RBAC 三个独立 P3 面。
- Parallelism Level: L1 Parallel Analysis
- Selected Subagents:
  - RAG Parser Architect: P3-2 parser/OCR/page hierarchy 分析。
  - Model Provider Expert: P3-3 Spring AI provider 接入分析。
  - Security & Quality: P3-4 JWT/RBAC 分析。
  - Integration Reviewer: Main Codex 合并。
- Implementation Mode: Main Codex 单线程实现认证层切片。

## 3. Integration Decision

选择 P3-4-I 作为本轮实现切片：

- P3-4 剩余最高风险是生产环境仍信任 `X-User-Id`。
- P3-3 真实 provider 和 P3-2 OCR/真实页码都需要新增依赖审查。
- 本切片不新增依赖，可在现有边界内直接提升认证根安全。

## 4. Implementation Steps

1. [x] 并行专家分析 P3-2 / P3-3 / P3-4。
2. [x] 创建 PRD / REQ / SPEC / PLAN / TASK / Context Pack。
3. [x] RED: 新增认证 filter / CurrentUserService 测试。
4. [x] GREEN: 新增 `AuthProperties` / JWT verifier / filter 行为。
5. [x] GREEN: 调整 dev/test 兼容与 roles 判断。
6. [x] 运行 focused / adjacent / full backend tests。
7. [x] 更新 Evidence / Acceptance / Changelog / Memory / TODO / Skill。

## 5. Test Commands

```powershell
cd backend
mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest test
mvn --% -Dtest=DevAuthFilterTest,CurrentUserServiceTest,StructuredRequestLoggingFilterTest,CourseKnowledgeControllerTest,AssessmentControllerTest,DocumentControllerTest test
mvn test
```

## 6. Verification Results

- RED observed: focused test first failed at compilation because `AuthProperties`, `CurrentUserService(AppProperties)`, and `currentUser()` were not implemented.
- Focused after implementation/config: `13` tests, `0` failures, `0` errors, `0` skipped.
- Adjacent after implementation/config: `74` tests, `0` failures, `0` errors, `0` skipped.
- Full backend after implementation/config: `345` tests, `0` failures, `0` errors, `1` skipped.

## 7. Architecture Drift Check

| Check | Status | Notes |
|---|---|---|
| Backend layering | PASS | 认证上下文仍位于 `common/auth` filter + service；业务对象授权未迁移到 Controller。 |
| Frontend rules | PASS | 未修改 frontend，未暴露 API key。 |
| Agent / RAG rules | PASS | 未修改 Agent/RAG 业务链路。 |
| Security | PASS | 生产禁用 `X-User-Id` 身份建立；invalid Bearer 不 fallback；未提交真实 secret。 |
| API / Database | PASS | 无 DB schema 变更；错误响应沿用 `UNAUTHORIZED` envelope。 |

## 8. Risks

| Risk | Mitigation |
|---|---|
| 大量现有测试依赖 `X-User-Id` | dev/test 保留兼容模式。 |
| JDK JWT 过渡实现不等于完整 OAuth2/JWK | SPEC 明确为无依赖过渡；后续正式 provider 另开切片。 |
| roles 切换导致业务矩阵回退 | 保留 `currentUserId()` 签名；业务授权不改；运行 P3-4 回归。 |
| 认证失败日志泄露 token | 只返回固定 `UNAUTHORIZED`，不返回 raw token / secret / 签名。 |
