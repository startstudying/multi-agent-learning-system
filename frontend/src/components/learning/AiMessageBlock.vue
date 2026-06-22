<script setup lang="ts">
import { AlertTriangle, Sparkles } from 'lucide-vue-next'
import SourceTags from '../workspace/SourceTags.vue'
import type { AiQaVerificationSummary, CitationSource } from '../../types/api'

defineProps<{
  answer: string
  stage: string
  traceId: string
  sources: CitationSource[]
  verification: AiQaVerificationSummary | null
  qualityFlags: string[]
  sourcePolicy: string
  uncertaintyLevel: string
  answerMode: string
  reasoningEffort: string
  toolCallCount: number
  errorMessage: string
}>()

function displayStage(stage: string) {
  const stageLabels: Record<string, string> = {
    IDLE: '空闲',
    RETRIEVING: '检索中',
    DONE: '完成',
    ERROR: '错误',
  }
  return stageLabels[stage] ?? stage
}
</script>

<template>
  <article class="message-row ai-message">
    <div class="message-avatar">
      <Sparkles :size="17" aria-hidden="true" />
    </div>
    <div class="message-bubble">
      <div class="ai-heading">
        <div>
          <p class="message-label">AI 回答</p>
          <h3>AI 回答</h3>
        </div>
        <span class="stage-pill">{{ displayStage(stage) }}</span>
      </div>
      <p v-if="answer" class="answer-text">{{ answer }}</p>
      <p v-else class="answer-empty">等待输入</p>
      <div v-if="verification" class="qa-quality-card" data-test="qa-quality-summary">
        <div>
          <span>质量校验</span>
          <strong>{{ verification.verdict }}</strong>
        </div>
        <div>
          <span>Gate</span>
          <strong>{{ verification.gatePolicy }}</strong>
        </div>
        <div>
          <span>来源策略</span>
          <strong>{{ sourcePolicy || '未返回' }}</strong>
        </div>
        <div>
          <span>不确定性</span>
          <strong>{{ uncertaintyLevel || '未返回' }}</strong>
        </div>
        <div>
          <span>模式</span>
          <strong>{{ answerMode || 'THINKING' }} / {{ reasoningEffort || '-' }}</strong>
        </div>
        <div>
          <span>工具链</span>
          <strong>{{ toolCallCount }} 步</strong>
        </div>
        <p v-if="qualityFlags.length">{{ qualityFlags.join(' / ') }}</p>
      </div>
      <SourceTags :sources="sources" />
      <p v-if="traceId" class="trace-line">Trace ID: {{ traceId }}</p>
      <div v-if="sources.length === 0" class="no-source-card" data-test="no-source-card">
        <AlertTriangle :size="17" aria-hidden="true" />
        <span>暂无引用来源</span>
      </div>
      <p v-if="errorMessage" class="error-text" role="status">{{ errorMessage }}</p>
    </div>
  </article>
</template>

<style scoped>
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
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  border-radius: 999px;
}

.message-bubble {
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 18px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.ai-heading {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.message-label {
  color: #6366f1;
  font-size: 12px;
  font-weight: 800;
}

.ai-heading h3 {
  margin: 4px 0 0;
  color: #111827;
  font-size: 17px;
  line-height: 1.25;
  letter-spacing: 0;
}

.stage-pill {
  flex: 0 0 auto;
  padding: 5px 8px;
  color: #4f46e5;
  font-size: 12px;
  font-weight: 800;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  border-radius: 999px;
}

.answer-text,
.answer-empty {
  color: #243041;
  font-size: 15px;
  line-height: 1.65;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.answer-empty {
  color: #8a97aa;
}

.trace-line {
  color: #8a97aa;
  font-size: 12px;
}

.no-source-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 8px;
  align-items: start;
  padding: 11px;
  color: #92400e;
  font-size: 13px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 8px;
}

.qa-quality-card {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.qa-quality-card div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.qa-quality-card span {
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
}

.qa-quality-card strong {
  color: #0f172a;
  font-size: 13px;
  overflow-wrap: anywhere;
}

.qa-quality-card p {
  grid-column: 1 / -1;
  margin: 2px 0 0;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.error-text {
  padding: 10px 12px;
  color: #991b1b;
  font-size: 13px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
}

@media (max-width: 720px) {
  .qa-quality-card {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
