<script setup lang="ts">
export type MobbinMetricItem = {
  label: string
  value: string | number
  note?: string
}

defineProps<{
  items: MobbinMetricItem[]
}>()
</script>

<template>
  <section class="mobbin-metric-strip" aria-label="关键指标">
    <article v-for="item in items" :key="item.label">
      <span>{{ item.label }}</span>
      <strong>{{ item.value }}</strong>
      <p v-if="item.note">{{ item.note }}</p>
    </article>
  </section>
</template>

<style scoped>
.mobbin-metric-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  min-width: 0;
}

article {
  display: grid;
  gap: 6px;
  min-width: 0;
  min-height: 104px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 20px;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
}

span {
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
}

strong {
  color: #0f172a;
  font-size: 26px;
  line-height: 1.1;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

p {
  color: #64748b;
  font-size: 12px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}
</style>
