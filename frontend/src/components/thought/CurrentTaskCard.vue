<script setup lang="ts">
import { Activity, Clock3, Cpu, Fingerprint, Layers3 } from 'lucide-vue-next'
import type { CurrentThoughtTask, ThoughtStatus } from '../../types/thought'

defineProps<{
  task: CurrentThoughtTask
}>()

function formatThoughtStatus(status: ThoughtStatus) {
  const labels: Record<ThoughtStatus, string> = {
    waiting: '等待中',
    running: '执行中',
    done: '已完成',
    warning: '需关注',
    failed: '失败',
  }
  return labels[status]
}
</script>

<template>
  <section class="thought-card current-task-card" aria-label="当前任务">
    <div class="thought-card-heading">
      <div>
        <span class="thought-eyebrow">当前任务</span>
        <h3>{{ task.title }}</h3>
      </div>
      <em :class="['thought-status', task.status]">{{ formatThoughtStatus(task.status) }}</em>
    </div>

    <dl class="task-detail-list">
      <div>
        <Layers3 :size="15" aria-hidden="true" />
        <dt>任务类型</dt>
        <dd>{{ task.taskType }}</dd>
      </div>
      <div>
        <Cpu :size="15" aria-hidden="true" />
        <dt>模型</dt>
        <dd>{{ task.model }}</dd>
      </div>
      <div>
        <Fingerprint :size="15" aria-hidden="true" />
        <dt>Trace 编号</dt>
        <dd>{{ task.traceId }}</dd>
      </div>
      <div>
        <Clock3 :size="15" aria-hidden="true" />
        <dt>开始时间</dt>
        <dd>{{ task.startedAt }}</dd>
      </div>
      <div>
        <Activity :size="15" aria-hidden="true" />
        <dt>状态</dt>
        <dd>{{ formatThoughtStatus(task.status) }}</dd>
      </div>
    </dl>
  </section>
</template>

<style scoped>
.thought-card {
  display: grid;
  gap: 14px;
  padding: 14px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
}

.thought-card-heading {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.thought-eyebrow {
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
}

h3 {
  margin: 3px 0 0;
  color: #111827;
  font-size: 16px;
  line-height: 1.3;
  letter-spacing: 0;
}

.thought-status {
  flex: 0 0 auto;
  padding: 4px 8px;
  color: #4b5563;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
  text-transform: uppercase;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
}

.thought-status.running {
  color: #5b21b6;
  background: #f3e8ff;
  border-color: #ddd6fe;
}

.thought-status.done {
  color: #047857;
  background: #d1fae5;
  border-color: #a7f3d0;
}

.thought-status.warning {
  color: #92400e;
  background: #fef3c7;
  border-color: #fde68a;
}

.thought-status.failed {
  color: #b91c1c;
  background: #fee2e2;
  border-color: #fecaca;
}

.task-detail-list {
  display: grid;
  gap: 9px;
  margin: 0;
}

.task-detail-list div {
  display: grid;
  grid-template-columns: 18px minmax(74px, auto) minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  min-width: 0;
  color: #6b7280;
}

.task-detail-list dt {
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
}

.task-detail-list dd {
  min-width: 0;
  margin: 0;
  color: #111827;
  font-size: 13px;
  overflow-wrap: anywhere;
}
</style>
