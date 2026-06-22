<template>
  <div class="mobbin-action-dock">
    <slot />
  </div>
</template>

<style scoped>
.mobbin-action-dock {
  position: sticky;
  bottom: 14px;
  z-index: 4;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
  padding: 12px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.95);
  border-radius: 18px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(18px);
}
</style>
