# PLAN - P3-4 KB-course binding schema and lifecycle governance

## Skill Selection Report

| Skill | Why |
|---|---|
| `feature-development-workflow` | 强制 L 级文档、实现、验证闭环 |
| `spring-boot-architecture` | 保持 Controller -> Service -> Repository 分层 |
| `database-design` | 新增 V20 schema / migration |
| `rag-project-review` | RAG query 权限前置过滤 |
| `security-review` | 防 broken access control / IDOR / role confusion |
| `object-scope-authorization` | 复用对象级授权和 anti-enumeration 规则 |
| `auth-context-boundary` | roles-first，不从 subject name 提权 |
| `test-generator` | TDD RED / GREEN / regression matrix |

Missing skills：无。  
GitHub research：不需要。  
New project skill：完成后视沉淀情况再决定。

## Size Classification

Size：L。

原因：DB schema、RAG 权限、CourseAccess、Document lifecycle、测试矩阵均受影响。

## Subagent Decision

Use Subagents：Yes。  
Parallelism Level：L2 Parallel Design + final review。  
Selected Subagents：

- Spec Architect
- Security & Quality
- Test Engineer
- Integration Reviewer
- Final Verification / Documentation Consistency reviewers

实现模式：专家并行分析/设计，主 Codex 单线实现，避免权限文件并发冲突。

## Confidence Check

- Duplicate check：现有只有 `kb_document.course_id`，没有 KB 级 course binding。
- Architecture compliance：沿用 Spring Boot/JPA/Flyway/CourseAccessService。
- Official docs：不新增外部 API 或依赖，使用既有项目栈。
- OSS references：不需要，问题是项目内部授权模型收敛。
- Root cause：KB 级课程事实缺失，RAG query 按 `kb_id` 检索。

Confidence：0.95，可以进入实现。

## 实施步骤

1. 写 RED tests：
   - V20 migration static test。
   - PermissionService course-bound read/write。
   - RagQueryService course-bound query and no artifacts。
   - KnowledgeBaseController create binding。
   - DocumentController binding/document-course consistency。
2. 实现 schema/entity/DTO。
3. 实现 `KnowledgeBaseService` create-time binding。
4. 实现 `PermissionService` course-bound read/write。
5. 实现 `DocumentService` upload consistency。
6. 修复专家审查发现的 requestId conflict 优先级和 `UNBOUND` 自动绑定并发锁风险。
7. 运行 focused tests。
8. 运行 adjacent tests。
9. 运行 full backend tests。
10. 记录 MySQL smoke 环境限制。
11. 更新 evidence / acceptance / changelog / memory / TODO。

## 风险

- H2 `ddl-auto=create-drop` 不验证 MySQL FK/check 细节；需保留 MySQL smoke 说明。
- `UNBOUND` KB 上传 course metadata 的最终策略是“空 KB 首次合法课程文档上传自动绑定；已有 active document 的 UNBOUND KB 拒绝 course metadata”，需在 SPEC/Evidence 中固定。
- requestId 幂等契约优先级必须高于 KB-course mismatch，避免同一幂等键不同 payload 返回 400。
- 空 `UNBOUND` KB 自动绑定需要 KB 行级锁，避免并发上传造成 KB/course 与 document/course 漂移。
- `PUBLIC` 在 BOUND KB 下语义收缩；需测试未选课学生不能 query。
