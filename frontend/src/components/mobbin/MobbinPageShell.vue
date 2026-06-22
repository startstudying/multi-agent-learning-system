<script setup lang="ts">
withDefaults(
  defineProps<{
    ariaLabel?: string
  }>(),
  {
    ariaLabel: 'Mobbin 风格页面',
  },
)
</script>

<template>
  <section class="mobbin-page-shell" :aria-label="ariaLabel">
    <slot />
  </section>
</template>

<style scoped>
.mobbin-page-shell {
  display: grid;
  align-content: start;
  gap: 18px;
  min-width: 0;
  min-height: 100svh;
  padding: 26px;
  background:
    radial-gradient(circle at 20% 0%, rgba(219, 234, 254, 0.74), transparent 30%),
    linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%);
}

@media (max-width: 760px) {
  .mobbin-page-shell {
    padding: 18px;
  }
}
</style>
