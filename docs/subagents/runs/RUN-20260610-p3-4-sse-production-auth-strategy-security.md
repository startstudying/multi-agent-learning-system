# RUN-20260610 P3-4 SSE production auth strategy - Security Reviewer

## 结论

当前后端 production-like 环境对 SSE endpoint 的安全目标应保持 fail-closed：`/api/chat/sessions/{id}/stream` 与 `/api/tutor/sessions/{id}/stream` 不应因为浏览器原生 `EventSource` 无法携带 `Authorization` header 而回退到 `X-User-Id`、`dev_user` 或裸 query token。

## 最高风险

1. 生产前端 SSE 使用原生 `EventSource`，无法携带 Bearer header；若为了修通生产流式能力而放开 endpoint、恢复 header fallback 或直接把 Bearer 放入 query，会造成认证绕过或 token 泄露。
2. 当前 SSE GET query 携带 `question` 与 `kbIds`，可能进入浏览器历史、代理日志、Referer 或监控系统；本轮不改变协议，但应在后续任务消除完整问题正文进入 URL 的设计。
3. 任务开始前缺少 Chat/Tutor SSE 在 production/staging 下的直接认证回归测试；已有普通 API 认证测试不足以锁住 SSE async 边界。

## 必须覆盖的测试项

- `production` 下无 Bearer 访问 Chat SSE，哪怕带 `X-User-Id: admin`，也返回 `UNAUTHORIZED`，且不启动 async。
- `production` 下 invalid Bearer 访问 Chat SSE 返回 `UNAUTHORIZED`，且不启动 async。
- `staging` 下 header-only 访问 Chat SSE 返回 `UNAUTHORIZED`，且不启动 async。
- `production` 下无 Bearer 访问 Tutor SSE 返回 `UNAUTHORIZED`，且不启动 async。
- `production` 下 valid Bearer 使用 JWT subject/roles，忽略 spoofed `X-User-Id`。
- Bearer `USER sub=admin` 不应获得 admin role facts。

## 后续建议

- 生产可用流式客户端优先使用 `fetch` / `ReadableStream` 并携带 `Authorization: Bearer ...`。
- 如果继续使用原生 `EventSource`，只能使用后端经已认证请求签发的短 TTL、一次性、绑定 `userId/sessionId/kbIds/purpose` 的 stream token；不能把 Bearer token 直接放入 query。
- 后续协议切片应避免把完整 `question` 放入 URL，可考虑 POST 创建 stream session 后再打开不含敏感正文的 stream。

## 本轮边界建议

本轮只应补后端 production/staging SSE auth regression；若现有后端策略已满足 fail-closed，则不要为了“修通”生产 SSE 而放宽认证策略。
