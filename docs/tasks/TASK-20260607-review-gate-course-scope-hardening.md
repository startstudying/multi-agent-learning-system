# TASK - Review Gate 课程范围收口

状态：已完成（2026-06-07）。

## Task 1

补齐 `ReviewGovernanceService` 的课程范围权限判断。

### 目标

- list 只返回当前 teacher 自己课程的审核记录
- decision 只允许当前 teacher 处理自己课程的审核记录
- admin 保持全局能力
- student 继续拒绝

### 交付物

- `ReviewGovernanceService` 服务层权限收口
- `ReviewGovernanceServiceTest` 新增/调整课程范围权限测试
- `ResourceReviewControllerTest` 新增/调整 teacher/admin/student 行为测试

### Done Criteria

- [x] teacher 无法看到或处理其他课程 review
- [x] admin 行为不回退
- [x] student 行为不回退
- [x] teacher 对 missing review 和 foreign review 均返回安全 `FORBIDDEN`
- [x] `mvn --% -Dtest=ReviewGovernanceServiceTest,ResourceReviewControllerTest,ResourceGenerationControllerTest test` 通过
- [x] `mvn test` 通过
