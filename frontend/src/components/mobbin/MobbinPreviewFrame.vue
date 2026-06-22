<script setup lang="ts">
withDefaults(
  defineProps<{
    label?: string
  }>(),
  {
    label: 'Preview',
  },
)
</script>

<template>
  <section class="mobbin-preview-frame" :aria-label="label">
    <div class="preview-topbar">
      <span />
      <span />
      <span />
      <strong>{{ label }}</strong>
    </div>
    <div class="preview-body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.mobbin-preview-frame {
  display: grid;
  min-width: 0;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid rgba(203, 213, 225, 0.9);
  border-radius: 24px;
  box-shadow: 0 22px 52px rgba(15, 23, 42, 0.11);
}

.preview-topbar {
  display: flex;
  gap: 7px;
  align-items: center;
  min-height: 42px;
  padding: 0 14px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.preview-topbar span {
  width: 9px;
  height: 9px;
  background: #cbd5e1;
  border-radius: 999px;
}

.preview-topbar strong {
  margin-left: auto;
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
}

.preview-body {
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 16px;
}
</style>
