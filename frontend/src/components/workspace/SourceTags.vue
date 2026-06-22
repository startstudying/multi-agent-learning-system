<script setup lang="ts">
import { BookOpen, Link2 } from 'lucide-vue-next'
import type { CitationSource } from '../../types/api'

defineProps<{
  sources: CitationSource[]
}>()
</script>

<template>
  <div class="source-tags" aria-label="RAG 引用来源">
    <span v-if="sources.length === 0" class="source-tag empty">
      <Link2 :size="14" aria-hidden="true" />
      暂无引用来源
    </span>
    <span
      v-for="source in sources"
      :key="`${source.documentName}-${source.pageNum ?? 'none'}-${source.sectionTitle ?? 'section'}`"
      class="source-tag"
    >
      <BookOpen :size="14" aria-hidden="true" />
      {{ source.documentName }}
      <small v-if="source.pageNum">第 {{ source.pageNum }} 页</small>
      <small v-if="source.score">分数 {{ source.score }}</small>
    </span>
  </div>
</template>

<style scoped>
.source-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.source-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  padding: 6px 9px;
  color: #3b4c64;
  font-size: 12px;
  font-weight: 700;
  overflow-wrap: anywhere;
  background: #f7f9fc;
  border: 1px solid #dce3ee;
  border-radius: 999px;
}

.source-tag svg {
  flex: 0 0 auto;
  color: #6366f1;
}

.source-tag small {
  color: #64748b;
  font-weight: 700;
}

.source-tag.empty {
  color: #7a8799;
  background: #fafbfc;
}
</style>
