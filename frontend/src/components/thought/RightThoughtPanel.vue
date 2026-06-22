<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Bot, EyeOff, PanelRightOpen, X } from 'lucide-vue-next'
import AgentTimeline from './AgentTimeline.vue'
import CurrentTaskCard from './CurrentTaskCard.vue'
import RagSourcesCard from './RagSourcesCard.vue'
import RuntimeMetricsCard from './RuntimeMetricsCard.vue'
import {
  createDefaultThoughtPanelData,
  type CurrentThoughtTask,
  type ThoughtAgentStep,
  type ThoughtRagSources,
  type ThoughtRuntimeMetrics,
} from '../../types/thought'

const props = defineProps<{
  currentTask?: CurrentThoughtTask
  agentSteps?: ThoughtAgentStep[]
  ragSources?: ThoughtRagSources
  metrics?: ThoughtRuntimeMetrics
}>()

const emit = defineEmits<{
  (event: 'hidden-change', value: boolean): void
}>()

const route = useRoute()
const hidden = ref(false)

const routeFallback = computed(() => {
  if (route.name === 'teacher') {
    return {
      currentTask: {
        title: 'AI 审核线索',
        taskType: '教师决策',
        model: 'Critic / Safety / Citation',
        traceId: 'review-context',
        startedAt: '审核队列',
        status: 'running' as const,
      },
      agentSteps: [
        { name: 'Citation', status: 'running' as const, duration: '-', summary: '检查资源是否具备可追溯 Citation。' },
        { name: 'Critic', status: 'waiting' as const, duration: '-', summary: '等待教师结合摘要做最终判断。' },
        { name: 'Safety', status: 'waiting' as const, duration: '-', summary: '关注 Safety、事实性和学习者适配。' },
      ],
      ragSources: {
        knowledgeBase: '审核证据',
        chunkCount: 1,
        documents: [{ name: 'Citation / Critic / Safety', excerpt: '选择审核项后，页面主区域会展示后端返回的资源摘要和技术字段。' }],
      },
      metrics: { latency: '-', totalTokens: '-', modelCalls: '-', fallback: '教师决策', safety: '需要审核' },
    }
  }

  if (route.name === 'admin') {
    return {
      currentTask: {
        title: '运行信号',
        taskType: '运维监控',
        model: '健康检查 / 分析概览 / 告警',
        traceId: 'runtime-context',
        startedAt: '控制中心',
        status: 'running' as const,
      },
      agentSteps: [
        { name: '健康检查', status: 'running' as const, duration: '-', summary: '跟踪应用、数据库、Redis、MinIO、Model 和向量索引。' },
        { name: 'Token 预算', status: 'waiting' as const, duration: '-', summary: 'Token 用量来自分析概览。' },
        { name: '告警', status: 'waiting' as const, duration: '-', summary: '持久化告警可在运维页确认。' },
      ],
      ragSources: {
        knowledgeBase: '运行 Trace',
        chunkCount: 1,
        documents: [{ name: '健康检查 / 告警 / Trace', excerpt: '运维页保留健康检查、分析概览和告警确认 API。' }],
      },
      metrics: { latency: '健康检查', totalTokens: '分析概览', modelCalls: '概览', fallback: '告警', safety: '运维' },
    }
  }

  if (route.name === 'admin-model-providers') {
    return {
      currentTask: {
        title: 'Provider 策略',
        taskType: '模型供应商中心',
        model: '默认 Provider / Fallback / 连接测试',
        traceId: 'provider-context',
        startedAt: 'Provider 中心',
        status: 'running' as const,
      },
      agentSteps: [
        { name: '默认 Provider', status: 'running' as const, duration: '-', summary: '默认 Provider 决定主 Model 调用通道。' },
        { name: 'Fallback', status: 'waiting' as const, duration: '-', summary: '备用 Provider 用于支撑后端 Fallback 策略。' },
        { name: '连接测试', status: 'waiting' as const, duration: '-', summary: '连接测试不会回显已有 API key 明文。' },
      ],
      ragSources: {
        knowledgeBase: 'Provider 治理',
        chunkCount: 1,
        documents: [{ name: '默认 Provider / Fallback / Token 用量', excerpt: 'Provider 页面只改配置展示，不改变保存、测试和设为默认 Provider 的 API。' }],
      },
      metrics: { latency: '连接测试', totalTokens: '运维', modelCalls: 'Provider', fallback: '已配置', safety: '密钥已脱敏' },
    }
  }

  return {
    currentTask: {
      title: '学习思考流',
      taskType: 'RAG / Agent 执行流程',
      model: '学习系统',
      traceId: 'student-context',
      startedAt: '学习工作台',
      status: 'running' as const,
    },
    agentSteps: [
      { name: 'RAG', status: 'running' as const, duration: '-', summary: '围绕课程资料检索、Citation 和回答。' },
      { name: 'Agent 执行流程', status: 'waiting' as const, duration: '-', summary: '展示画像、路径、资源生成和测评闭环。' },
    ],
    ragSources: {
      knowledgeBase: '学习工作台',
      chunkCount: 1,
      documents: [{ name: 'RAG / Agent 执行流程', excerpt: '学习工作台会在主区域展示真实 Citation 来源和生成链路。' }],
    },
    metrics: { latency: '-', totalTokens: '-', modelCalls: '-', fallback: 'RAG Fallback', safety: '资源审核' },
  }
})

const panelData = computed(() => {
  const fallback = createDefaultThoughtPanelData()
  return {
    currentTask: props.currentTask ?? routeFallback.value.currentTask ?? fallback.currentTask,
    agentSteps: props.agentSteps?.length ? props.agentSteps : routeFallback.value.agentSteps,
    ragSources: props.ragSources ?? routeFallback.value.ragSources,
    metrics: props.metrics ?? routeFallback.value.metrics,
  }
})

const panelCopy = computed(() => {
  if (route.name === 'teacher') return { eyebrow: '审核线索', title: 'AI 审核线索', aria: 'AI 审核线索' }
  if (route.name === 'admin') return { eyebrow: '运行信号', title: '运行信号', aria: '运行信号' }
  if (route.name === 'admin-model-providers') return { eyebrow: 'Provider 策略', title: 'Provider 策略', aria: 'Provider 策略' }
  return { eyebrow: '思考流', title: '学习思考流', aria: '学习思考流' }
})

function hidePanel() {
  hidden.value = true
  emit('hidden-change', true)
}

function showPanel() {
  hidden.value = false
  emit('hidden-change', false)
}
</script>

<template>
  <aside
    v-if="!hidden"
    class="right-thought-panel"
    :aria-label="panelCopy.aria"
    data-test="right-thought-panel"
  >
    <header class="thought-panel-header">
      <div class="title-lockup">
        <span class="title-mark">
          <Bot :size="18" aria-hidden="true" />
        </span>
        <div>
          <p class="thought-eyebrow">{{ panelCopy.eyebrow }}</p>
          <h2>{{ panelCopy.title }}</h2>
        </div>
      </div>

      <div class="panel-actions">
        <button
          type="button"
          data-test="thought-panel-hide"
          :aria-label="`隐藏 ${panelCopy.title}`"
          title="隐藏"
          @click="hidePanel"
        >
          <X :size="16" aria-hidden="true" />
        </button>
      </div>
    </header>

    <div class="thought-panel-scroll">
      <CurrentTaskCard :task="panelData.currentTask" />
      <AgentTimeline :steps="panelData.agentSteps" />
      <RagSourcesCard :sources="panelData.ragSources" />
      <RuntimeMetricsCard :metrics="panelData.metrics" />
    </div>
  </aside>

  <button
    v-else
    class="thought-panel-restore"
    type="button"
    data-test="thought-panel-restore"
    :aria-label="`显示 ${panelCopy.title}`"
    :title="`显示 ${panelCopy.title}`"
    @click="showPanel"
  >
    <PanelRightOpen :size="18" aria-hidden="true" />
    <span>{{ panelCopy.title }}</span>
    <EyeOff :size="14" aria-hidden="true" />
  </button>
</template>

<style scoped>
.right-thought-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  width: 100%;
  min-width: 0;
  height: 100%;
  max-height: 100%;
  color: #111827;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.96)),
    #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
  overflow: hidden;
}

.thought-panel-header {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: rgba(255, 255, 255, 0.82);
  border-bottom: 1px solid #e5e7eb;
  backdrop-filter: blur(14px);
}

.title-lockup {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-width: 0;
}

.title-mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  color: #ffffff;
  background: linear-gradient(135deg, #4f46e5, #8b5cf6);
  border-radius: 10px;
}

.thought-eyebrow {
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
}

h2 {
  margin: 2px 0 0;
  color: #111827;
  font-size: 18px;
  line-height: 1.2;
  letter-spacing: 0;
}

.panel-actions {
  display: flex;
  gap: 6px;
  align-items: center;
}

.panel-actions button,
.thought-panel-restore {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  color: #4b5563;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
}

.panel-actions button:hover,
.thought-panel-restore:hover {
  color: #4f46e5;
  border-color: #c7d2fe;
}

.thought-panel-scroll {
  display: grid;
  align-content: start;
  gap: 12px;
  min-height: 0;
  padding: 14px;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.thought-panel-restore {
  position: fixed;
  right: 16px;
  bottom: 16px;
  z-index: 30;
  gap: 8px;
  width: auto;
  padding: 0 10px;
  color: #ffffff;
  background: linear-gradient(135deg, #4f46e5, #8b5cf6);
  border-color: #c4b5fd;
  box-shadow: 0 14px 34px rgba(79, 70, 229, 0.22);
}

@media (max-width: 1220px) {
  .right-thought-panel {
    height: auto;
    max-height: 720px;
    border-top: 1px solid #e5e7eb;
    border-left: 0;
  }
}
</style>
