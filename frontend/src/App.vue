<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppShell from './components/shell/AppShell.vue'
</script>

<template>
  <AppShell>
    <template #default>
      <RouterView />
    </template>
  </AppShell>
</template>
