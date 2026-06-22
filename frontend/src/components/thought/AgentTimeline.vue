<script setup lang="ts">
import { AlertTriangle, CheckCircle2, Clock3, LoaderCircle, XCircle } from 'lucide-vue-next'
import type { ThoughtAgentStep, ThoughtStatus } from '../../types/thought'

defineProps<{
  steps: ThoughtAgentStep[]
}>()

function statusIcon(status: ThoughtStatus) {
  if (status === 'done') return CheckCircle2
  if (status === 'running') return LoaderCircle
  if (status === 'warning') return AlertTriangle
  if (status === 'failed') return XCircle
  return Clock3
}

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
  <section class="thought-card agent-timeline-card" aria-label="Agent 执行流程">
    <div class="thought-card-heading">
      <div>
        <span class="thought-eyebrow">Agent 执行流程</span>
        <h3>Agent 执行流程</h3>
      </div>
      <span class="step-count">{{ steps.length }}</span>
    </div>

    <ol class="agent-timeline">
      <li v-for="(step, index) in steps" :key="`${step.name}-${index}`" :class="['timeline-step', step.status]">
        <span class="timeline-node" aria-hidden="true">
          <component :is="statusIcon(step.status)" :size="13" />
        </span>
        <div class="timeline-content">
          <div class="timeline-title-row">
            <strong>{{ step.name }}</strong>
            <em :class="['timeline-status', step.status]">{{ formatThoughtStatus(step.status) }}</em>
          </div>
          <p>{{ step.summary }}</p>
          <span class="duration">{{ step.duration }}</span>
        </div>
      </li>
    </ol>
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

.thought-card-heading,
.timeline-title-row {
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

.step-count {
  display: grid;
  min-width: 28px;
  height: 28px;
  place-items: center;
  color: #4f46e5;
  font-size: 12px;
  font-weight: 800;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  border-radius: 999px;
}

.agent-timeline {
  display: grid;
  gap: 0;
  padding: 0;
  margin: 0;
  list-style: none;
}

.timeline-step {
  position: relative;
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 10px;
  min-width: 0;
  padding-bottom: 16px;
  animation: thought-step-enter 220ms ease both;
}

.timeline-step:last-child {
  padding-bottom: 0;
}

.timeline-step::before {
  position: absolute;
  top: 24px;
  bottom: 0;
  left: 13px;
  width: 2px;
  content: '';
  background: #e5e7eb;
}

.timeline-step:last-child::before {
  display: none;
}

.timeline-node {
  z-index: 1;
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  color: #6b7280;
  background: #f3f4f6;
  border: 2px solid #d1d5db;
  border-radius: 999px;
}

.timeline-step.running .timeline-node {
  color: #ffffff;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-color: #c4b5fd;
  box-shadow: 0 0 0 5px rgba(139, 92, 246, 0.12);
}

.timeline-step.done .timeline-node {
  color: #ffffff;
  background: #10b981;
  border-color: #a7f3d0;
}

.timeline-step.warning .timeline-node {
  color: #92400e;
  background: #fef3c7;
  border-color: #fde68a;
}

.timeline-step.failed .timeline-node {
  color: #ffffff;
  background: #ef4444;
  border-color: #fecaca;
}

.timeline-content {
  display: grid;
  gap: 5px;
  min-width: 0;
  padding: 10px;
  background: #f9fafb;
  border: 1px solid #edf0f3;
  border-radius: 10px;
}

.timeline-title-row strong {
  min-width: 0;
  color: #111827;
  font-size: 13px;
  overflow-wrap: anywhere;
}

.timeline-content p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.45;
}

.duration {
  color: #9ca3af;
  font-size: 12px;
}

.timeline-status {
  flex: 0 0 auto;
  padding: 2px 7px;
  color: #4b5563;
  font-size: 11px;
  font-style: normal;
  font-weight: 800;
  text-transform: uppercase;
  background: #f3f4f6;
  border-radius: 999px;
}

.timeline-status.running {
  color: #5b21b6;
  background: #ede9fe;
}

@keyframes thought-step-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.timeline-status.done {
  color: #047857;
  background: #d1fae5;
}

.timeline-status.warning {
  color: #92400e;
  background: #fef3c7;
}

.timeline-status.failed {
  color: #b91c1c;
  background: #fee2e2;
}
</style>
