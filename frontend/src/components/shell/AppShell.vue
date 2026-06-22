<script setup lang="ts">
import LeftSidebar from './LeftSidebar.vue'
</script>

<template>
  <div class="app-shell" data-test="app-shell">
    <LeftSidebar />

    <main class="workbench-main" aria-label="AI 学习工作台">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  display: grid;
  grid-template-columns: var(--shell-sidebar-width) minmax(0, 1fr);
  width: 100%;
  height: 100vh;
  max-width: 100vw;
  overflow: hidden;
  color: var(--color-text);
  background: #ffffff;
}

.workbench-main {
  min-width: 0;
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
  background: #ffffff;
}

:deep(.left-sidebar) {
  width: var(--shell-sidebar-width);
  min-width: var(--shell-sidebar-width);
  height: 100vh;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  border-right: 1px solid var(--color-border);
}

@media (max-width: 768px) {
  .app-shell {
    grid-template-columns: minmax(0, 1fr);
    grid-template-rows: auto minmax(0, 1fr);
  }

  .workbench-main {
    height: auto;
    min-height: 0;
  }

  :deep(.left-sidebar) {
    width: 100%;
    min-width: 0;
    height: auto;
    max-height: 46vh;
    border-right: 0;
    border-bottom: 1px solid var(--color-border);
  }
}
</style>
