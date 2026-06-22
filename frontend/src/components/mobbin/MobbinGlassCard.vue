<script setup lang="ts">
withDefaults(
  defineProps<{
    eyebrow?: string
    title?: string
    elevated?: boolean
  }>(),
  {
    eyebrow: '',
    title: '',
    elevated: false,
  },
)
</script>

<template>
  <article :class="['mobbin-glass-card', { elevated }]">
    <header v-if="eyebrow || title || $slots.icon || $slots.actions" class="mobbin-card-header">
      <div>
        <p v-if="eyebrow" class="mobbin-eyebrow">{{ eyebrow }}</p>
        <h3 v-if="title">{{ title }}</h3>
      </div>
      <div v-if="$slots.icon || $slots.actions" class="mobbin-card-tools">
        <slot name="icon" />
        <slot name="actions" />
      </div>
    </header>
    <slot />
  </article>
</template>

<style scoped>
.mobbin-glass-card {
  display: grid;
  align-content: start;
  gap: 16px;
  min-width: 0;
  padding: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(248, 250, 252, 0.86)),
    #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.86);
  border-radius: 22px;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.07);
}

.mobbin-glass-card.elevated {
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.1);
}

.mobbin-card-header {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  justify-content: space-between;
  min-width: 0;
}

.mobbin-card-header > div:first-child {
  min-width: 0;
}

.mobbin-eyebrow {
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: uppercase;
}

h3 {
  margin: 4px 0 0;
  color: #0f172a;
  font-size: 18px;
  line-height: 1.24;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.mobbin-card-tools {
  display: inline-flex;
  flex: 0 0 auto;
  gap: 8px;
  align-items: center;
  color: #4f46e5;
}
</style>
