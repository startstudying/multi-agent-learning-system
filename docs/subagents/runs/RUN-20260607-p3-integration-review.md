# RUN-20260607 P3 剩余 TODO 集成评审

## 结论

综合 P3-2/P3-3/P3-4/P3-5，下一步优先级最高的是 P3-4 权限矩阵扩展的最小安全切片。相比 OCR/VectorDB、真实模型接入和全量可观测性，Review Gate 课程范围收口无新增依赖、无数据库迁移、测试可证明，并直接降低横向越权风险。

## 排序

| 排名 | 切片 | 理由 |
|---:|---|---|
| 1 | Review Gate 课程范围权限收口 | 安全收益最高，可小步实现，可用现有字段验证 |
| 2 | P3-5 请求级结构化日志 | 无新增依赖，但安全收益低于权限收口 |
| 3 | P3-3 模型边界/结构化输出验证 | 有价值，但真实 provider 仍需后续依赖评审 |
| 4 | P3-2 Embedding/VectorDB/Hybrid/OCR | 需要依赖或更大架构设计，不适合当前最小切片 |

## 推荐实施

本轮推荐实现：

```text
P3-4 review-gate-course-scope-hardening
```

允许修改 `ReviewGovernanceService` 与相关测试，必要时注入 `CourseRepository`。不得扩展到完整 JWT/RBAC、数据库迁移、模型接入、VectorDB 或前端。

## 建议测试

```bash
cd backend && mvn "-Dtest=ReviewGovernanceServiceTest,ResourceReviewControllerTest" test
cd backend && mvn test
```

