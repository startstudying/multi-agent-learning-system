<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    status?: string | number | boolean | null
    tone?: 'neutral' | 'success' | 'warning' | 'danger' | 'info' | 'dark'
  }>(),
  {
    status: 'UNKNOWN',
    tone: undefined,
  },
)

function normalizeTone(status: string | number | boolean | null | undefined) {
  const value = String(status ?? '').toLowerCase()
  if (props.tone) return props.tone
  if (['ok', 'ready', 'active', 'approved', 'enabled', 'default', 'done', 'succeeded', 'live', 'healthy'].includes(value)) {
    return 'success'
  }
  if (['pending', 'pending_critic', 'running', 'watch', 'waiting', 'idle'].includes(value)) {
    return 'warning'
  }
  if (['failed', 'error', 'critical', 'revision_requested', 'disabled', 'missing', 'down'].includes(value)) {
    return 'danger'
  }
  if (['observed', 'check', 'info', 'unknown'].includes(value)) {
    return 'info'
  }
  return 'neutral'
}

function displayStatus(status: string | number | boolean | null | undefined) {
  const value = String(status ?? 'UNKNOWN')
  const labels: Record<string, string> = {
    UNKNOWN: '未知',
    READY: '就绪',
    ACTIVE: '活跃',
    APPROVED: '已通过',
    ENABLED: '已启用',
    DISABLED: '已停用',
    DEFAULT: '默认',
    DONE: '已完成',
    SUCCEEDED: '成功',
    LIVE: '运行中',
    HEALTHY: '健康',
    OK: '正常',
    PENDING: '待处理',
    PENDING_CRITIC: '待审核',
    RUNNING: '运行中',
    WATCH: '需关注',
    WAITING: '等待中',
    IDLE: '空闲',
    FAILED: '失败',
    ERROR: '错误',
    CRITICAL: '严重',
    REVISION_REQUESTED: '需修改',
    MISSING: '缺失',
    DOWN: '不可用',
    OBSERVED: '已观测',
    CHECK: '检查中',
    INFO: '信息',
    CLEAR: '已清除',
    ACKNOWLEDGED: '已确认',
  }
  return labels[value] ?? labels[value.toUpperCase()] ?? value
}
</script>

<template>
  <span :class="['mobbin-status-pill', normalizeTone(status)]">
    <slot>{{ displayStatus(status) }}</slot>
  </span>
</template>

<style scoped>
.mobbin-status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  max-width: 100%;
  min-height: 24px;
  padding: 4px 9px;
  overflow: hidden;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.1;
  text-overflow: ellipsis;
  white-space: nowrap;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 999px;
}

.neutral {
  color: #475569;
  background: #f1f5f9;
}

.success {
  color: #047857;
  background: #d1fae5;
  border-color: #a7f3d0;
}

.warning {
  color: #92400e;
  background: #fef3c7;
  border-color: #fde68a;
}

.danger {
  color: #b91c1c;
  background: #fee2e2;
  border-color: #fecaca;
}

.info {
  color: #3730a3;
  background: #eef2ff;
  border-color: #c7d2fe;
}

.dark {
  color: #f8fafc;
  background: #0f172a;
  border-color: #334155;
}
</style>
