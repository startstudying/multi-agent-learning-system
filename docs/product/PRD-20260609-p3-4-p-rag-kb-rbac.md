# PRD - P3-4-P RAG KB management roles-first RBAC

## 1. 问题陈述

RAG KB management 与 document/index 入口仍主要依赖 `currentUserId`、owner/public/USER permission 和 legacy subject 推断。Bearer JWT 接入后，这会造成权限语义不一致：

- Bearer `ADMIN sub=ops_admin` 不是 literal `admin` 时，无法获得 admin 全局 KB/document/index 管理语义。
- Bearer `USER sub=teacher_1` 可能在 course metadata 校验中被 legacy `teacher_*` 推断为 teacher。
- Bearer `USER/STUDENT sub=admin` 可能在 missing document/index task 上得到 admin 风格 `NOT_FOUND`，形成存在性 oracle。

本切片将 RAG KB management 主路径迁移到 roles-first RBAC，继续保留个人 KB 创建能力，不引入新的 KB-course schema。

## 2. 目标用户

| 用户 | 角色 | 核心需求 |
|---|---|---|
| 管理员 | `ADMIN` | 全局查看、上传、重建索引和排查 KB/document/index task，且不受 spoofed `X-User-Id` 影响 |
| 教师 | `TEACHER` | 管理自己的 KB 和自己课程的 RAG document metadata，不依赖 `teacher_` subject 命名 |
| 学生/普通用户 | `STUDENT` / `USER` | 保留个人 KB 能力，但不能通过 subject 名称获得 admin/teacher 权限 |

## 3. 用户故事

- 作为管理员，我希望 Bearer `ADMIN` 可以看到全部 active KB，并能处理任意 KB 下的 document/index task。
- 作为教师，我希望 Bearer `TEACHER sub=instructor_1` 可以上传自己课程的 RAG document metadata，即使用户 id 没有 `teacher_` 前缀。
- 作为安全负责人，我希望 Bearer `USER sub=teacher_1` 不能被当作 teacher 管理课程 metadata。
- 作为安全负责人，我希望 Bearer `USER sub=admin` 不能通过 missing document/index task 响应区分对象是否存在。

## 4. MVP 范围

### 纳入范围

- `POST /api/knowledge-bases`
- `GET /api/knowledge-bases`
- `POST /api/knowledge-bases/{kbId}/documents`
- `GET /api/knowledge-bases/{kbId}/documents`
- `GET /api/documents/{documentId}`
- `POST /api/documents/{documentId}/reindex`
- `GET /api/index-tasks/{taskId}`
- Controller 从 `UserContext.roles()` 派生 role facts。
- Service 使用 explicit admin/teacher facts。
- Bearer spoof / role-confusion / missing-vs-foreign 防回归测试。

### 非目标

- 不移除学生/普通用户个人 KB 创建能力。
- 不新增 KB-course 绑定 schema。
- 不新增或修改 API path、request DTO、response DTO。
- 不修改 frontend。
- 不修改 parser/vector/index worker/storage/provider。
- 不修改 `/api/rag/query` retrieval runtime。
- 不迁移 formal OAuth2/JWK/Spring Security。
- 不声明 broader class/course 或整个 P3-4 完成。

## 5. 成功指标

| 指标 | 目标值 | 衡量方式 |
|---|---|---|
| RAG KB roles-first 覆盖 | P3-4-P 场景 100% 通过 | Controller tests |
| Admin global KB management | Bearer admin 可 list/read/write all active KB | Controller tests |
| Role-confusion 阻断 | `USER sub=admin/teacher_1` 不提权 | Controller tests |
| API/DB/frontend drift | 0 | 文件变更与 SPEC 对照 |

## 6. 用户流程

```text
Bearer JWT request
  -> DevAuthFilter validates token and builds UserContext
  -> KnowledgeBaseController / DocumentController reads CurrentUserService.currentUser()
  -> Controller derives currentUserAdmin/currentUserTeacher from UserContext.roles()
  -> Service enforces KB permission and course metadata scope with explicit role facts
  -> Repository/storage/index task operations run only after authorization passes
```

## 7. 关键产品决策

| 决策 | 结论 | 原因 |
|---|---|---|
| 个人 KB 创建 | 保留 | 学生端已有创建个人 KB 能力，且本切片目标是 roles-first 管理语义，不是课程 KB 产品重构 |
| Admin KB 权限 | `ADMIN` role 全局可读写 active KB | 解决 Bearer admin 不是 literal `admin` 时无法管理的问题 |
| Teacher 课程 metadata | 依赖 explicit `TEACHER` role + own-course manage | 避免 `teacher_*` subject 命名成为权限来源 |
| Missing response | admin missing 返回 `NOT_FOUND`；非 admin missing/foreign 返回 `FORBIDDEN` | 延续对象级 anti-enumeration 策略 |

## 8. 发布边界

本切片作为 P3-4 permission hardening 的一个独立小步发布。它完成 RAG KB management 主路径的 roles-first RBAC，但不代表整个 P3-4 权限体系完成。
