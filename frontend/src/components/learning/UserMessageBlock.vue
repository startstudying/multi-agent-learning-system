<script setup lang="ts">
defineProps<{
  question: string
  profilePrompt: string
}>()
</script>

<template>
  <article v-if="question || profilePrompt" class="message-row user-message">
    <span class="legacy-hook">RAG 问答区</span>
    <div class="message-avatar">我</div>
    <div class="message-bubble">
      <p class="message-label">用户</p>
      <h3 v-if="question">{{ question }}</h3>
      <p v-if="profilePrompt" class="prompt-note">{{ profilePrompt }}</p>
    </div>
  </article>
</template>

<style scoped>
.legacy-hook {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
  border: 0;
}

.message-row {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}

.message-avatar {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
  background: #111827;
  border-radius: 999px;
}

.message-bubble {
  min-width: 0;
  padding: 16px 18px;
  color: #111827;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.message-label {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.message-bubble h3 {
  margin: 4px 0 0;
  font-size: 16px;
  line-height: 1.45;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.prompt-note {
  margin-top: 10px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}
</style>
