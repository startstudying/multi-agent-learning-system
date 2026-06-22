<script setup lang="ts">
import { Database, FileText, Layers3 } from 'lucide-vue-next'
import type { ThoughtRagSources } from '../../types/thought'

defineProps<{
  sources: ThoughtRagSources
}>()
</script>

<template>
  <section class="thought-card rag-sources-card" aria-label="检索来源 RAG">
    <div class="thought-card-heading">
      <div>
        <span class="thought-eyebrow">检索来源 RAG</span>
        <h3>引用依据</h3>
      </div>
      <Database :size="18" aria-hidden="true" />
    </div>

    <dl class="rag-summary">
      <div>
        <dt>命中知识库</dt>
        <dd>{{ sources.knowledgeBase }}</dd>
      </div>
      <div>
        <dt>命中片段数</dt>
        <dd>{{ sources.chunkCount }}</dd>
      </div>
    </dl>

    <ul class="rag-document-list" aria-label="来源文档列表">
      <li v-for="document in sources.documents" :key="`${document.documentId ?? document.name}-${document.pageNum ?? 'na'}`">
        <FileText :size="15" aria-hidden="true" />
        <div>
          <strong>{{ document.name }}</strong>
          <span>
            <Layers3 :size="12" aria-hidden="true" />
            {{ document.sectionTitle ?? '未标注章节' }}
            <template v-if="document.pageNum"> / 第 {{ document.pageNum }} 页</template>
            <template v-if="document.score !== undefined"> / 相关度 {{ document.score }}</template>
          </span>
          <p v-if="document.excerpt">{{ document.excerpt }}</p>
        </div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.thought-card {
  display: grid;
  gap: 14px;
  padding: 14px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06);
}

.thought-card-heading {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.thought-card-heading svg {
  color: #4f46e5;
}

.thought-eyebrow {
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
}

h3 {
  margin: 3px 0 0;
  color: #111827;
  font-size: 16px;
  line-height: 1.3;
  letter-spacing: 0;
}

.rag-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
}

.rag-summary div {
  min-width: 0;
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #edf0f3;
  border-radius: 10px;
}

.rag-summary dt {
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
}

.rag-summary dd {
  min-width: 0;
  margin: 4px 0 0;
  color: #111827;
  font-size: 13px;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.rag-document-list {
  display: grid;
  gap: 8px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.rag-document-list li {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 9px;
  padding: 10px;
  background: #fbfdff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}

.rag-document-list svg {
  color: #4f46e5;
}

.rag-document-list strong,
.rag-document-list span,
.rag-document-list p {
  min-width: 0;
  overflow-wrap: anywhere;
}

.rag-document-list strong {
  display: block;
  color: #111827;
  font-size: 13px;
}

.rag-document-list span {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
  margin-top: 3px;
  color: #4f46e5;
  font-size: 12px;
}

.rag-document-list p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.45;
}
</style>
