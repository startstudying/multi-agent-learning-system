<script setup lang="ts">
import { CheckCircle2, MoreHorizontal, Share2 } from 'lucide-vue-next'

interface WorkflowStep {
  id: string
  label: string
  complete: boolean
}

defineProps<{
  title?: string
  courseName?: string
  topicName?: string
  stage: string
  mastery: number
  indexedDocuments: number
  pendingDocuments: number
  workflowSteps: WorkflowStep[]
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
  <header class="workspace-chat-header">
    <div class="session-title">
      <p class="eyebrow">AI 学习工作区</p>
      <h2>{{ title || 'AI 学习工作区' }}</h2>
      <div class="session-meta" aria-label="Course context">
        <span v-if="courseName">课程：{{ courseName }}</span>
        <span v-if="topicName">知识点：{{ topicName }}</span>
        <span class="progress-pill">进行中</span>
      </div>
    </div>

    <div class="header-actions">
      <label class="mode-select">
        <span class="sr-only">学习模式</span>
        <select aria-label="学习模式">
          <option>AI 导师</option>
          <option>刷题练习</option>
          <option>路径复盘</option>
        </select>
      </label>
      <button class="icon-action" type="button" aria-label="分享">
        <Share2 :size="17" aria-hidden="true" />
        <span>分享</span>
      </button>
      <button class="icon-action square" type="button" aria-label="更多">
        <MoreHorizontal :size="18" aria-hidden="true" />
      </button>
    </div>

    <section class="session-stats" aria-label="Learning session stats">
      <article>
        <span>RAG 阶段</span>
        <strong>{{ displayStage(stage) }}</strong>
      </article>
      <article>
        <span>平均掌握度</span>
        <strong>{{ mastery }}%</strong>
      </article>
      <article>
        <span>课程资料</span>
        <strong>{{ indexedDocuments }} / {{ pendingDocuments }}</strong>
      </article>
    </section>

    <nav class="workflow-rail" aria-label="Workflow status">
      <span
        v-for="step in workflowSteps"
        :key="step.id"
        :class="['workflow-dot', { complete: step.complete }]"
        :data-test="`workflow-${step.id}`"
      >
        <CheckCircle2 v-if="step.complete" :size="13" aria-hidden="true" />
        <i v-else aria-hidden="true"></i>
        {{ step.label }}
      </span>
    </nav>
  </header>
</template>

<style scoped>
.workspace-chat-header {
  display: grid;
  gap: 16px;
  padding: 22px 24px 18px;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid #e6ebf2;
  backdrop-filter: blur(14px);
}

.session-title {
  min-width: 0;
}

.eyebrow {
  color: #6366f1;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.session-title h2 {
  margin-top: 4px;
  color: #111827;
  font-size: 24px;
  line-height: 1.2;
  letter-spacing: 0;
}

.session-meta,
.header-actions,
.workflow-rail {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.session-meta {
  margin-top: 10px;
  color: #64748b;
  font-size: 13px;
}

.session-meta span {
  padding: 5px 8px;
  background: #f7f9fc;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
}

.progress-pill {
  color: #4f46e5;
  background: #eef2ff !important;
  border-color: #c7d2fe !important;
}

.header-actions {
  position: absolute;
  top: 20px;
  right: 24px;
}

.mode-select select,
.icon-action {
  min-height: 38px;
  color: #1f2937;
  font: inherit;
  font-size: 13px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 999px;
}

.mode-select select {
  padding: 0 34px 0 13px;
}

.icon-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 13px;
  cursor: pointer;
}

.icon-action.square {
  width: 38px;
  padding: 0;
}

.session-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.session-stats article {
  display: grid;
  gap: 2px;
  min-width: 0;
  padding: 12px;
  background: #fbfcfe;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
}

.session-stats span {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.session-stats strong {
  color: #111827;
  font-size: 18px;
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.workflow-rail {
  gap: 10px;
}

.workflow-dot {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.workflow-dot i {
  width: 8px;
  height: 8px;
  background: #cbd5e1;
  border-radius: 999px;
}

.workflow-dot.complete {
  color: #4f46e5;
}

.workflow-dot.complete i {
  background: #4f46e5;
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.12);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 980px) {
  .header-actions {
    position: static;
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .workspace-chat-header {
    padding: 18px;
  }

  .session-stats {
    grid-template-columns: 1fr;
  }
}
</style>
