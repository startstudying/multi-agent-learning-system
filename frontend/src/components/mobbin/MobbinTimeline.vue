<script setup lang="ts">
import MobbinStatusPill from './MobbinStatusPill.vue'

export type MobbinTimelineItem = {
  title: string
  subtitle?: string
  detail?: string
  status?: string
  time?: string
}

defineProps<{
  items: MobbinTimelineItem[]
  emptyText?: string
}>()
</script>

<template>
  <ol v-if="items.length" class="mobbin-timeline">
    <li v-for="item in items" :key="`${item.title}-${item.time ?? item.status ?? ''}`">
      <span class="timeline-dot" />
      <div>
        <header>
          <strong>{{ item.title }}</strong>
          <MobbinStatusPill v-if="item.status" :status="item.status" />
        </header>
        <p v-if="item.subtitle">{{ item.subtitle }}</p>
        <small v-if="item.detail">{{ item.detail }}</small>
        <time v-if="item.time">{{ item.time }}</time>
      </div>
    </li>
  </ol>
  <p v-else class="mobbin-empty">{{ emptyText ?? '暂无时间线事件' }}</p>
</template>

<style scoped>
.mobbin-timeline {
  display: grid;
  gap: 12px;
  padding: 0;
  list-style: none;
}

li {
  position: relative;
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr);
  gap: 10px;
  min-width: 0;
}

li::before {
  position: absolute;
  top: 20px;
  bottom: -14px;
  left: 7px;
  width: 2px;
  content: '';
  background: #e2e8f0;
}

li:last-child::before {
  display: none;
}

.timeline-dot {
  width: 16px;
  height: 16px;
  margin-top: 5px;
  background: linear-gradient(135deg, #4f46e5, #14b8a6);
  border: 3px solid #ffffff;
  border-radius: 999px;
  box-shadow: 0 0 0 1px #c7d2fe;
}

li > div {
  display: grid;
  gap: 5px;
  min-width: 0;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
}

header {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
}

strong {
  min-width: 0;
  color: #0f172a;
  font-size: 14px;
  overflow-wrap: anywhere;
}

p,
small,
time,
.mobbin-empty {
  color: #64748b;
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

small,
time {
  display: block;
}

.mobbin-empty {
  padding: 14px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 16px;
}
</style>
