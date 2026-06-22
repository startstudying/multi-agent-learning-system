<script setup lang="ts">
import { LockKeyhole, Route } from 'lucide-vue-next'
import type { PathNode } from '../../types/api'

defineProps<{
  nodes: PathNode[]
}>()

function displayStatus(status: PathNode['status']) {
  const statusLabels: Record<PathNode['status'], string> = {
    READY: '就绪',
    ACTIVE: '当前',
    LOCKED: '锁定',
    DONE: '已完成',
  }
  return statusLabels[status] ?? status
}
</script>

<template>
  <article class="stream-block path-block" data-test="learning-path-block">
    <div class="block-heading">
      <div>
        <p class="eyebrow">学习路径</p>
        <h3>学习路径卡片</h3>
      </div>
      <Route :size="19" aria-hidden="true" />
    </div>

    <div v-if="nodes.length" class="path-stepper" aria-label="学习路径">
      <section
        v-for="node in nodes"
        :key="node.id"
        :class="['path-card', node.status.toLowerCase()]"
      >
        <div class="path-card-top">
          <strong>{{ node.title }}</strong>
          <span :class="['status-chip', node.status.toLowerCase()]">
            <LockKeyhole v-if="node.status === 'LOCKED'" :size="13" aria-hidden="true" />
            {{ displayStatus(node.status) }}
          </span>
        </div>
        <p>学习路径节点：{{ node.reason }}</p>
        <div class="mastery-row">
          <span>掌握度 {{ node.mastery }}%</span>
          <div class="mini-meter" aria-hidden="true">
            <i :style="{ width: `${node.mastery}%` }"></i>
          </div>
        </div>
      </section>
    </div>

    <p v-else class="empty-state">暂无学习路径</p>
  </article>
</template>

<style scoped>
.stream-block {
  display: grid;
  gap: 14px;
  padding: 18px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.block-heading,
.path-card-top {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.eyebrow {
  color: #6366f1;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.block-heading h3 {
  margin: 4px 0 0;
  color: #111827;
  font-size: 17px;
  letter-spacing: 0;
}

.block-heading svg {
  color: #6366f1;
}

.path-stepper {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.path-card {
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 14px;
  background: #fbfcfe;
  border: 1px solid #e3e9f2;
  border-radius: 8px;
  transition: border-color 180ms ease, transform 180ms ease, box-shadow 180ms ease;
}

.path-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.06);
}

.path-card.active {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1), rgba(139, 92, 246, 0.08)), #ffffff;
  border-color: #a5b4fc;
}

.path-card.locked {
  color: #8a97aa;
  background: #f8fafc;
}

.path-card strong {
  color: #111827;
  font-size: 14px;
  line-height: 1.3;
}

.path-card p,
.empty-state {
  color: #64748b;
  font-size: 13px;
  line-height: 1.45;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 7px;
  color: #475569;
  font-size: 11px;
  font-weight: 800;
  background: #eef2f7;
  border-radius: 999px;
}

.status-chip.active {
  color: #4f46e5;
  background: #eef2ff;
}

.status-chip.done {
  color: #047857;
  background: #d1fae5;
}

.status-chip.locked {
  color: #64748b;
  background: #e2e8f0;
}

.mastery-row {
  display: grid;
  gap: 6px;
}

.mastery-row span {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.mini-meter {
  overflow: hidden;
  height: 8px;
  background: #e2e8f0;
  border-radius: 999px;
}

.mini-meter i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
}

@media (max-width: 980px) {
  .path-stepper {
    grid-template-columns: 1fr;
  }
}
</style>
