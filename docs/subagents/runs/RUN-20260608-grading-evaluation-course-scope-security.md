# RUN-20260608 Grading Evaluation Course Scope - Security & Quality

## 1. 推荐权限矩阵

| Actor | `courseId` omitted | existing own course | existing foreign course | non-existent course |
|---|---|---:|---:|---:|
| admin | `VALIDATION_ERROR` | 200 | 200 | `NOT_FOUND` |
| teacher / `teacher_*` | `VALIDATION_ERROR` | 200 | `FORBIDDEN` | `FORBIDDEN` |
| student | `FORBIDDEN` | `FORBIDDEN` | `FORBIDDEN` | `FORBIDDEN` |

## 2. 风险点

- 当前 teacher 只有角色门禁，没有 course scope。
- 若先校验 sample `knowledgePointId` 再校验 course 权限，可能形成对象枚举。
- 若 legacy score request 不要求 `courseId`，teacher 可绕过 course scope。

## 3. 负向测试

- student 带合法 course 仍 `FORBIDDEN`。
- teacher 不带 `courseId` 返回 `VALIDATION_ERROR`。
- teacher foreign course 与 missing course 均返回 `FORBIDDEN`。
- admin missing course 返回 `NOT_FOUND`。
- teacher own course 混入 foreign/missing `knowledgePointId` 返回统一安全失败。

## 4. 结论

建议 `courseId` 强制必填，作为授权锚点；`knowledgePointId` 只做样本一致性校验，不能作为权限边界来源。
