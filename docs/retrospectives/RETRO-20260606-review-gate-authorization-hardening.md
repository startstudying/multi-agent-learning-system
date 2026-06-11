# Review Gate 审核权限加固 Retrospective

## 做得有效

- 权限判断放在 `ReviewGovernanceService`，Controller 只传入当前用户，保持了后端分层。
- student 访问在 review/resource/task 查询前直接 403，避免了详情泄露。
- controller 和 service 测试同时覆盖，能证明 HTTP 行为和 service guard 顺序。

## 需要注意

- 当前 `teacher/admin` 是临时 `X-User-Id` 策略，不是真实 RBAC。
- 教师目前没有课程/班级范围过滤，后续不能长期保持全局审核权限。

## 后续建议

- 引入正式 role / permission / course scope 后替换硬编码 reviewer 判断。
- 增加 forged reviewId / taskId / courseId 的权限渗透测试。
