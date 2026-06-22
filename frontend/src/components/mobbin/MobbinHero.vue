<script setup lang="ts">
withDefaults(
  defineProps<{
    eyebrow?: string
    title: string
    description?: string
  }>(),
  {
    eyebrow: '',
    description: '',
  },
)
</script>

<template>
  <header class="mobbin-hero">
    <div class="mobbin-hero-copy">
      <p v-if="eyebrow" class="mobbin-eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <p v-if="description" class="mobbin-description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions || $slots.preview" class="mobbin-hero-side">
      <slot name="actions" />
      <slot name="preview" />
    </div>
  </header>
</template>

<style scoped>
.mobbin-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(260px, 0.6fr);
  gap: 22px;
  align-items: stretch;
  min-width: 0;
  padding: 26px;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 0%, rgba(255, 255, 255, 0.88), transparent 34%),
    linear-gradient(135deg, #f8fafc 0%, #eef4ff 54%, #f6f7fb 100%);
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 30px;
  box-shadow: 0 24px 68px rgba(15, 23, 42, 0.08);
}

.mobbin-hero-copy {
  display: grid;
  align-content: center;
  gap: 10px;
  min-width: 0;
}

.mobbin-eyebrow {
  color: #4f46e5;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1 {
  max-width: 820px;
  color: #0f172a;
  font-size: clamp(30px, 4vw, 54px);
  line-height: 1.02;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.mobbin-description {
  max-width: 720px;
  color: #64748b;
  font-size: 15px;
  line-height: 1.7;
}

.mobbin-hero-side {
  display: grid;
  align-content: stretch;
  gap: 12px;
  min-width: 0;
}

@media (max-width: 900px) {
  .mobbin-hero {
    grid-template-columns: 1fr;
    padding: 20px;
    border-radius: 24px;
  }

  h1 {
    font-size: 32px;
  }
}
</style>
