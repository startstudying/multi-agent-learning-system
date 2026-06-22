<script setup lang="ts">
import { Gauge, GitBranch, RotateCcw, ShieldCheck, WalletCards } from 'lucide-vue-next'
import type { ThoughtRuntimeMetrics } from '../../types/thought'

defineProps<{
  metrics: ThoughtRuntimeMetrics
}>()

const metricItems = [
  { key: 'latency', label: '响应延迟', icon: Gauge },
  { key: 'totalTokens', label: 'Token 总量', icon: WalletCards },
  { key: 'modelCalls', label: '模型调用', icon: GitBranch },
  { key: 'fallback', label: 'Fallback', icon: RotateCcw },
  { key: 'safety', label: 'Safety 检查', icon: ShieldCheck },
] as const
</script>

<template>
  <section class="thought-card metrics-card" aria-label="运行指标">
    <div class="thought-card-heading">
      <div>
        <span class="thought-eyebrow">运行指标</span>
        <h3>运行指标</h3>
      </div>
    </div>

    <div class="metric-grid">
      <article v-for="item in metricItems" :key="item.key">
        <component :is="item.icon" :size="15" aria-hidden="true" />
        <span>{{ item.label }}</span>
        <strong>{{ metrics[item.key] }}</strong>
      </article>
    </div>
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

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.metric-grid article {
  display: grid;
  gap: 5px;
  min-width: 0;
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #edf0f3;
  border-radius: 10px;
}

.metric-grid article:first-child {
  grid-column: 1 / -1;
}

.metric-grid svg {
  color: #4f46e5;
}

.metric-grid span {
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
}

.metric-grid strong {
  color: #111827;
  font-size: 15px;
  overflow-wrap: anywhere;
}
</style>
