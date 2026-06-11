# REQ - P3-4-Q Analytics student summary roles-first RBAC

## 1. 追踪

- PRD: `docs/product/PRD-20260609-p3-4-q-analytics-student-summary-rbac.md`
- 关联规格：
  - `docs/specs/SPEC-20260608-real-auth-rbac-context.md`
  - `docs/specs/SPEC-20260608-analytics-student-summary-course-scope.md`
  - `docs/specs/SPEC-20260609-p3-4-m-course-access-roles-first-overload.md`
  - `docs/specs/SPEC-20260609-p3-4-p-rag-kb-rbac.md`

## 2. Skill Selection Report

## Task Type

Bug fix / security hardening：Analytics student summary 的课程读取权限从 legacy subject-name inference 迁移到 roles-first `CourseAccessService` overload。

## Selected Skills

| Skill | Why Needed |
|---|---|
| `feature-development-workflow` | 项目强制的 PRD/REQ/SPEC/PLAN/TASK/CONTEXT -> implementation -> evidence 流程 |
| `auth-context-boundary` | Bearer JWT、spoofed header、`UserContext.roles()` 与 legacy fallback 边界 |
| `object-scope-authorization` | course-scoped student summary 的对象级授权与 missing-vs-foreign 防枚举 |
| `test-driven-development` | 先新增 failing regression tests，再实现最小修复 |
| `security-review` | 校验 role-confusion、IDOR 与响应泄漏风险 |
| `test-generator` | 设计 Bearer admin/teacher/user 回归矩阵 |
| `architecture-drift-check` | 确认无 API/DB/frontend/dependency drift |
| `verification-before-completion` | 完成声明前必须有 fresh verification evidence |

## Missing Skills

无。

## GitHub Research Needed

No。现有项目 `CourseAccessService` role-aware overload 与本地安全规则已覆盖该补口。

## New Project-Specific Skill To Create

暂不创建。该模式继续归入 `auth-context-boundary` 和 `object-scope-authorization`。

## 3. Functional Requirements

| ID | Requirement | Priority | Acceptance |
|---|---|---|---|
| REQ-P3-4-Q-01 | `GET /api/analytics/students/{learnerId}/summary?courseId=...` 必须保留 Controller 传入的 explicit admin/teacher role facts | P0 | Service course access 不再调用 legacy `requireCourseRead(currentUserId, courseId)` |
| REQ-P3-4-Q-02 | Bearer `ADMIN sub=ops_admin` 必须可读取已有课程下学生摘要，即使请求带 spoofed `X-User-Id` | P0 | HTTP test 返回 `OK` 且 course-scoped 数据不泄漏 foreign course signals |
| REQ-P3-4-Q-03 | Bearer `TEACHER sub=instructor_1` 必须可读取自己课程 active enrolled learner 的摘要，不要求 subject 为 `teacher` 或 `teacher_*` | P0 | HTTP test 返回 `OK` |
| REQ-P3-4-Q-04 | Bearer `USER sub=teacher_1` 不能通过 subject 前缀获得 teacher course read 权限 | P0 | 未 enrollment 的 course-scoped self summary 返回 `FORBIDDEN` |
| REQ-P3-4-Q-05 | 非管理员对 missing course 与 foreign course 的响应必须同为安全 `FORBIDDEN`，且无 `data` | P0 | HTTP test 覆盖 missing/foreign body 不含目标 course id |
| REQ-P3-4-Q-06 | 本切片不得新增 DB migration、依赖、API path、DTO 字段或 frontend 改动 | P0 | 文件变更审查通过 |

## 4. Non-Functional Requirements

- 权限判断必须在 backend Service 层执行。
- 不得在文档或测试输出中保存真实 secret、API key、raw Bearer token。
- 保持现有 API response contract。
- 保留 `CourseAccessService` legacy 签名，不做大范围删除。

## 5. Out of Scope

- Assessment / GradingEvaluation roles-first 迁移。
- LearningPath / ResourceGeneration course-bound create roles-first 迁移。
- Broader class/course permission matrix。
- Formal OAuth2/JWK/Spring Security。
- 前端、DB、依赖和 API contract 变更。
