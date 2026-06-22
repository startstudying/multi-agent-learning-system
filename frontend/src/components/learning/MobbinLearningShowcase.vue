<script setup lang="ts">
import { computed } from 'vue'
import {
  BarChart3,
  BookOpen,
  ClipboardCheck,
  Database,
  FileText,
  FileUp,
  GitBranch,
  ListChecks,
  RefreshCw,
  UploadCloud,
  UserRound,
} from 'lucide-vue-next'
import MobbinGlassCard from '../mobbin/MobbinGlassCard.vue'
import MobbinStatusPill from '../mobbin/MobbinStatusPill.vue'
import type { DocumentRecord, GeneratedResource, PathNode, TraceStep, WorkbenchState } from '../../types/api'

const props = defineProps<{
  state: WorkbenchState
  documents: DocumentRecord[]
  pathNodes: PathNode[]
  resources: GeneratedResource[]
  traceSteps: TraceStep[]
  selectedDocumentFileName: string
  selectedResourceTypes: string[]
  resourceTypes: string[]
  indexedDocuments: number
  pendingDocuments: number
  averageMastery: number
  approvedResources: GeneratedResource[]
  pendingReviewResources: GeneratedResource[]
  revisionResources: GeneratedResource[]
  otherReviewResources: GeneratedResource[]
  localizedErrorMessage: string
  isLoading: boolean
  displayStatus: (status: string) => string
  displayReplanRecordId: (replanRecordId: string) => string
}>()

const emit = defineEmits<{
  'update:selectedResourceTypes': [value: string[]]
  'select-file': [event: Event]
  'upload': []
  'generate': []
  'refresh-resource-status': []
  'assess': []
  'refine-profile': []
  'select-follow-up-question': [question: string]
}>()

const recentDocuments = computed(() => props.documents.slice(0, 3))
const recentTraceSteps = computed(() => props.traceSteps.slice(-4).reverse())
const overallPathProgress = computed(() => {
  if (props.pathNodes.length === 0) return 0
  const done = props.pathNodes.filter((node) => node.status === 'DONE').length
  const activeWeight = props.pathNodes.some((node) => node.status === 'ACTIVE') ? 0.5 : 0
  return Math.round(((done + activeWeight) / props.pathNodes.length) * 100)
})

const abilityDimensions = computed(() => {
  const dimensions = props.state.learnerProfile.dimensions.slice(0, 4)
  if (dimensions.length > 0) {
    return dimensions.map((dimension) => ({
      name: dimension.name,
      value: Math.round(Number(dimension.value) <= 1 ? Number(dimension.value) * 100 : Number(dimension.value)),
    }))
  }
  return [
    { name: '概念理解', value: props.averageMastery || 0 },
    { name: '应用能力', value: Math.max(0, Math.min(100, props.averageMastery - 7)) },
    { name: '实践技能', value: Math.max(0, Math.min(100, props.averageMastery - 2)) },
    { name: '综合分析', value: Math.max(0, Math.min(100, props.averageMastery + 3)) },
  ]
})

function toggleResourceType(type: string) {
  const exists = props.selectedResourceTypes.includes(type)
  const next = exists
    ? props.selectedResourceTypes.filter((selectedType) => selectedType !== type)
    : [...props.selectedResourceTypes, type]
  emit('update:selectedResourceTypes', next)
}

function clampPercent(value: number) {
  return Math.max(0, Math.min(100, Math.round(Number.isFinite(value) ? value : 0)))
}

function statusLabel(status: string | undefined | null) {
  const labels: Record<string, string> = {
    IDLE: '空闲',
    RETRIEVING: '检索中',
    DONE: '已完成',
    ERROR: '错误',
    READY: '就绪',
    ACTIVE: '进行中',
    LOCKED: '待开始',
    PENDING: '待处理',
    PENDING_CRITIC: '待审核',
    APPROVED: '已通过',
    REVISION_REQUESTED: '需修改',
    OTHER_REVIEW_STATUS: '其他',
    COMPLETED: '已完成',
    RUNNING: 'RUNNING',
    WAITING: 'WAITING',
  }
  return labels[status ?? ''] ?? status ?? '暂无'
}

function traceTone(status: string) {
  if (status === 'DONE' || status === 'COMPLETED') return 'SUCCESS'
  if (status === 'RUNNING' || status === 'PENDING' || status === 'WAITING') return 'RUNNING'
  return 'ERROR'
}
</script>

<template>
  <section class="learning-workbench" aria-label="学习工作台" data-test="student-support-workspace">
    <div class="learning-card-grid">
      <MobbinGlassCard class="workspace-card profile-card" title="学习画像" elevated data-test="student-profile-showcase">
        <template #icon>
          <span class="card-icon teal"><UserRound :size="22" aria-hidden="true" /></span>
        </template>

        <div class="learner-summary">
          <span class="avatar-plate"><UserRound :size="30" aria-hidden="true" /></span>
          <div>
            <strong>{{ state.learnerProfile.learnerId || '学习者' }}</strong>
            <span>角色：学习者</span>
            <span>专业：{{ state.learnerProfile.major || '计算机科学' }}</span>
          </div>
          <button class="ghost-button compact" type="button" data-test="refine-profile" :disabled="isLoading" @click="emit('refine-profile')">
            <UserRound :size="15" aria-hidden="true" />
            {{ state.loadingAction === 'profile' ? '更新中' : '更新画像' }}
          </button>
        </div>

        <label class="compact-field profile-goal">
          <span>学习目标</span>
          <textarea
            v-model="state.profilePrompt"
            data-test="profile-prompt-input"
            rows="3"
            :placeholder="state.learnerProfile.goal || '掌握 RAG 的基本原理与实现，能够构建简单的知识问答应用。'"
          ></textarea>
        </label>

        <div v-if="state.followUpQuestions.length" class="followup-strip" aria-label="画像追问">
          <button
            v-for="(question, index) in state.followUpQuestions"
            :key="question"
            type="button"
            :class="{ selected: state.selectedFollowUpQuestion === question }"
            :title="question"
            :data-test="`profile-follow-up-${index}`"
            :aria-pressed="state.selectedFollowUpQuestion === question"
            @click="emit('select-follow-up-question', question)"
          >
            {{ question }}
          </button>
        </div>

        <div class="mini-stat-grid">
          <article>
            <span>当前水平</span>
            <strong>中级</strong>
          </article>
          <article>
            <span>学习偏好</span>
            <strong>{{ state.learnerProfile.preference || '实践导向' }}</strong>
          </article>
          <article>
            <span>可用时间</span>
            <strong>每周 6-8 小时</strong>
          </article>
        </div>

        <p v-if="state.errorMessage && state.loadingAction === 'profile'" class="soft-alert">{{ localizedErrorMessage }}</p>
        <footer class="card-footnote">
          <span>画像基于历史行为智能生成</span>
          <a href="#" @click.prevent>查看详情</a>
        </footer>
      </MobbinGlassCard>

      <MobbinGlassCard class="workspace-card knowledge-card" title="知识库" elevated data-test="knowledge-base-showcase">
        <template #icon>
          <span class="card-icon blue"><Database :size="22" aria-hidden="true" /></span>
        </template>
        <template #actions>
          <a href="#" @click.prevent>查看全部</a>
        </template>

        <div class="upload-actions">
          <label class="outline-action">
            <FileUp :size="18" aria-hidden="true" />
            <span>{{ selectedDocumentFileName || '选择课程资料' }}</span>
            <input type="file" data-test="document-file-input" accept=".md,.markdown,.pdf,.txt" @change="emit('select-file', $event)" />
          </label>
          <button class="outline-action" type="button" data-test="upload-document" :disabled="state.loadingAction === 'document'" @click="emit('upload')">
            <UploadCloud :size="18" aria-hidden="true" />
            {{ state.loadingAction === 'document' ? '上传中' : '上传课程资料' }}
          </button>
        </div>

        <section class="recent-files">
          <h3>最近资料</h3>
          <ul v-if="recentDocuments.length" aria-label="课程资料">
            <li v-for="document in recentDocuments" :key="document.id">
              <MobbinStatusPill :status="document.type" tone="neutral">{{ document.type }}</MobbinStatusPill>
              <strong :title="document.name">{{ document.name }}</strong>
              <span>{{ document.updatedAt }}</span>
              <span>{{ document.status }}</span>
            </li>
          </ul>
          <p v-else class="empty-state">暂无课程资料，上传后会显示最近文件。</p>
        </section>

        <p v-if="state.errorMessage && state.loadingAction === 'document'" class="soft-alert">{{ localizedErrorMessage }}</p>
        <footer class="card-footnote success">
          <span>已入库 {{ indexedDocuments }} 个文件，{{ pendingDocuments }} 个待处理</span>
          <a href="#" @click.prevent>管理知识库</a>
        </footer>
      </MobbinGlassCard>

      <MobbinGlassCard class="workspace-card path-card" title="学习路径" elevated data-test="learning-path-showcase">
        <template #icon>
          <span class="card-icon purple"><BookOpen :size="22" aria-hidden="true" /></span>
        </template>
        <template #actions>
          <a href="#" @click.prevent>查看全部</a>
        </template>

        <div class="path-meta">
          <span>RAG 学习路径 · 共 {{ pathNodes.length }} 个阶段</span>
        </div>

        <ol v-if="pathNodes.length" class="path-steps">
          <li v-for="(node, index) in pathNodes.slice(0, 5)" :key="node.id" :class="node.status.toLowerCase()">
            <span class="step-index">{{ index + 1 }}</span>
            <strong :title="node.title">{{ node.title }}</strong>
            <MobbinStatusPill :status="node.status">{{ statusLabel(node.status) }}</MobbinStatusPill>
          </li>
        </ol>
        <div v-else class="empty-state tall">
          <BookOpen :size="26" aria-hidden="true" />
          <strong>暂无学习路径</strong>
          <span>更新画像后会生成个性化路径。</span>
        </div>

        <footer class="progress-panel">
          <div>
            <span>整体进度</span>
            <strong>{{ overallPathProgress }}%</strong>
          </div>
          <div class="progress-track"><span :style="{ width: `${overallPathProgress}%` }"></span></div>
        </footer>
      </MobbinGlassCard>

      <MobbinGlassCard class="workspace-card resource-card" title="生成资源" elevated data-test="resource-showcase">
        <template #icon>
          <span class="card-icon red"><FileText :size="22" aria-hidden="true" /></span>
        </template>

        <section class="resource-types">
          <h3>选择资源类型</h3>
          <div class="chip-grid">
            <button
              v-for="type in resourceTypes"
              :key="type"
              type="button"
              :class="['resource-chip', { selected: selectedResourceTypes.includes(type) }]"
              :data-test="`resource-type-${type}`"
              @click="toggleResourceType(type)"
            >
              {{ type }}
            </button>
          </div>
        </section>

        <label class="compact-field">
          <span>资源主题</span>
          <input type="text" :value="state.learnerProfile.goal || 'RAG 系统架构与实现'" readonly />
        </label>

        <div class="resource-status-line">
          <MobbinStatusPill :status="state.resourceTaskStatus">{{ statusLabel(state.resourceTaskStatus) }}</MobbinStatusPill>
          <span>进度 {{ state.resourceProgressPercent }}%</span>
          <span>已通过 {{ approvedResources.length }} / 待审核 {{ pendingReviewResources.length }}</span>
        </div>

        <div class="resource-actions">
          <button
            class="ghost-button"
            type="button"
            data-test="refresh-resource-status"
            :disabled="isLoading || !state.resourceTaskId"
            @click="emit('refresh-resource-status')"
          >
            <RefreshCw :size="17" aria-hidden="true" />
            查看状态
          </button>
          <button class="danger-action" type="button" data-test="generate-resources" :disabled="isLoading" @click="emit('generate')">
            <ListChecks :size="17" aria-hidden="true" />
            {{ state.loadingAction === 'resources' ? '生成中' : '生成资源' }}
          </button>
        </div>
      </MobbinGlassCard>

      <MobbinGlassCard class="workspace-card assessment-card" title="测评反馈" elevated data-test="assessment-showcase">
        <template #icon>
          <span class="card-icon blue"><BarChart3 :size="22" aria-hidden="true" /></span>
        </template>

        <section class="mastery-summary">
          <div>
            <span>学习掌握度</span>
            <strong>{{ clampPercent(state.mastery || averageMastery) }}%</strong>
          </div>
          <MobbinStatusPill status="中等偏上" tone="info">中等偏上</MobbinStatusPill>
        </section>
        <div class="progress-track main"><span :style="{ width: `${clampPercent(state.mastery || averageMastery)}%` }"></span></div>
        <p class="muted-line">基于最近测评结果</p>

        <section class="ability-list">
          <h3>能力维度</h3>
          <div v-for="dimension in abilityDimensions" :key="dimension.name" class="ability-row">
            <span>{{ dimension.name }}</span>
            <div class="progress-track"><span :style="{ width: `${clampPercent(dimension.value)}%` }"></span></div>
            <strong>{{ clampPercent(dimension.value) }}%</strong>
          </div>
        </section>

        <details class="assessment-input">
          <summary>填写测评作答</summary>
          <label class="compact-field">
            <span>作答内容</span>
            <textarea
              v-model="state.assessmentAnswer"
              data-test="assessment-answer-input"
              rows="3"
              placeholder="填写作答内容"
            ></textarea>
          </label>
        </details>

        <footer class="card-footnote">
          <button class="link-action" type="button" data-test="submit-assessment" :disabled="isLoading" @click="emit('assess')">
            <ClipboardCheck :size="16" aria-hidden="true" />
            {{ state.loadingAction === 'assessment' ? '提交中' : '开始测评' }}
          </button>
          <span>重规划 {{ displayReplanRecordId(state.replanRecordId) }}</span>
        </footer>
      </MobbinGlassCard>

      <MobbinGlassCard class="workspace-card trace-card" title="Agent Trace" elevated data-test="student-diagnostics">
        <template #icon>
          <span class="card-icon dark"><GitBranch :size="22" aria-hidden="true" /></span>
        </template>
        <template #actions>
          <a href="#" @click.prevent>查看全部</a>
        </template>

        <section class="trace-list">
          <h3>最新 Trace 事件</h3>
          <ul v-if="recentTraceSteps.length">
            <li v-for="step in recentTraceSteps" :key="`${step.actor}-${step.detail}`">
              <div>
                <strong :title="step.actor">{{ step.actor }}</strong>
                <span :title="step.detail">{{ step.detail }}</span>
              </div>
              <small>{{ step.latencyMs }}ms</small>
              <MobbinStatusPill :status="traceTone(step.status)" :tone="traceTone(step.status) === 'ERROR' ? 'danger' : traceTone(step.status) === 'RUNNING' ? 'info' : 'success'">
                {{ traceTone(step.status) }}
              </MobbinStatusPill>
            </li>
          </ul>
          <div v-else class="empty-state tall">
            <GitBranch :size="26" aria-hidden="true" />
            <strong>暂无 Trace</strong>
            <span>运行画像、资源生成或 RAG 后会显示事件。</span>
          </div>
        </section>

        <footer class="card-footnote">
          <span>共 {{ traceSteps.length }} 个事件</span>
          <a href="#" @click.prevent>查看历史</a>
        </footer>
      </MobbinGlassCard>
    </div>
  </section>
</template>

<style scoped>
.learning-workbench {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.card-icon,
.avatar-plate {
  display: grid;
  place-items: center;
  color: #ffffff;
  background: linear-gradient(135deg, #7c3aed, #4f46e5);
  border-radius: 14px;
}

.learning-card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  align-items: stretch;
}

.workspace-card {
  min-height: 390px;
  height: 100%;
}

.workspace-card :deep(.mobbin-card-header) {
  align-items: center;
}

.workspace-card :deep(.mobbin-card-header h3) {
  font-size: 20px;
}

.workspace-card :deep(.mobbin-card-tools a),
.card-footnote a {
  color: #4f46e5;
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
}

.card-icon {
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
}

.card-icon.teal {
  background: linear-gradient(135deg, #0f766e, #14b8a6);
}

.card-icon.blue {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
}

.card-icon.purple {
  background: linear-gradient(135deg, #7c3aed, #9333ea);
}

.card-icon.red {
  background: linear-gradient(135deg, #be123c, #e11d48);
}

.card-icon.dark {
  background: linear-gradient(135deg, #334155, #0f172a);
}

.learner-summary {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
}

.avatar-plate {
  width: 56px;
  height: 56px;
  color: #14b8a6;
  background: #ccfbf1;
  border-radius: 999px;
}

.learner-summary strong,
.recent-files strong,
.path-steps strong,
.trace-list strong {
  display: block;
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.learner-summary span,
.muted-line,
.card-footnote,
.path-meta,
.resource-status-line,
.trace-list span,
.recent-files span {
  color: #64748b;
  font-size: 13px;
  line-height: 1.35;
}

.compact-field {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.compact-field span,
.resource-types h3,
.recent-files h3,
.ability-list h3,
.trace-list h3 {
  margin: 0;
  color: #0f172a;
  font-size: 13px;
  font-weight: 900;
}

.compact-field input,
.compact-field textarea {
  width: 100%;
  min-width: 0;
  padding: 11px 12px;
  color: #0f172a;
  font: inherit;
  font-size: 13px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  resize: none;
}

.profile-goal textarea {
  min-height: 78px;
}

.mini-stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.mini-stat-grid article {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 10px;
  text-align: center;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}

.mini-stat-grid span {
  color: #64748b;
  font-size: 12px;
}

.mini-stat-grid strong {
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.followup-strip {
  display: flex;
  gap: 7px;
  overflow: hidden;
}

.followup-strip button {
  min-width: 0;
  max-width: 160px;
  padding: 7px 9px;
  overflow: hidden;
  color: #4f46e5;
  font: inherit;
  font-size: 12px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  border-radius: 999px;
  cursor: pointer;
}

.followup-strip button.selected {
  color: #ffffff;
  background: #4f46e5;
}

.upload-actions,
.resource-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.outline-action {
  position: relative;
  display: inline-flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  min-width: 0;
  min-height: 48px;
  padding: 10px 12px;
  overflow: hidden;
  color: #2563eb;
  font: inherit;
  font-weight: 800;
  background: #ffffff;
  border: 1px solid #bfdbfe;
  border-radius: 12px;
  cursor: pointer;
}

.outline-action span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.outline-action input {
  position: absolute;
  inset: 0;
  cursor: pointer;
  opacity: 0;
}

.recent-files {
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
}

.recent-files ul,
.trace-list ul,
.path-steps {
  display: grid;
  gap: 9px;
  padding: 0;
  list-style: none;
}

.recent-files li {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 9px;
  align-items: center;
  min-width: 0;
}

.path-steps {
  position: relative;
}

.path-steps li {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-height: 40px;
  padding: 8px 10px;
  background: #ffffff;
  border: 1px solid transparent;
  border-radius: 12px;
}

.path-steps li.active,
.path-steps li.done {
  border-color: #c4b5fd;
  background: #faf7ff;
}

.step-index {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
  background: #e2e8f0;
  border-radius: 999px;
}

.path-steps li.active .step-index,
.path-steps li.done .step-index {
  color: #ffffff;
  background: #7c3aed;
}

.progress-panel {
  display: grid;
  gap: 8px;
  margin-top: auto;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
}

.progress-panel > div:first-child,
.mastery-summary,
.card-footnote {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
}

.progress-panel span,
.mastery-summary span {
  color: #64748b;
  font-size: 13px;
}

.progress-panel strong,
.mastery-summary strong {
  color: #0f172a;
}

.progress-track {
  overflow: hidden;
  height: 7px;
  background: #e5e7eb;
  border-radius: 999px;
}

.progress-track span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #7c3aed, #2563eb);
  border-radius: inherit;
}

.progress-track.main {
  height: 8px;
}

.resource-types {
  display: grid;
  gap: 12px;
}

.chip-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.resource-chip {
  min-height: 38px;
  padding: 8px 10px;
  overflow: hidden;
  color: #0f172a;
  font: inherit;
  font-size: 13px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 10px;
  cursor: pointer;
}

.resource-chip.selected {
  color: #be123c;
  background: #fff1f2;
  border-color: #fb7185;
}

.resource-status-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.ghost-button,
.danger-action,
.link-action {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 10px 12px;
  font: inherit;
  font-weight: 900;
  border-radius: 12px;
  cursor: pointer;
}

.ghost-button {
  color: #4f46e5;
  background: #ffffff;
  border: 1px solid #dbe3ef;
}

.ghost-button.compact {
  min-height: 34px;
  padding: 7px 10px;
  font-size: 12px;
}

.danger-action {
  color: #ffffff;
  background: linear-gradient(135deg, #be123c, #e11d48);
  border: 1px solid transparent;
  box-shadow: 0 14px 26px rgba(190, 18, 60, 0.2);
}

.link-action {
  min-height: 0;
  padding: 0;
  color: #4f46e5;
  background: transparent;
  border: 0;
}

.mastery-summary strong {
  display: block;
  margin-top: 4px;
  color: #2563eb;
  font-size: 34px;
  line-height: 1;
}

.ability-list {
  display: grid;
  gap: 10px;
}

.ability-row {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) 42px;
  gap: 10px;
  align-items: center;
  color: #64748b;
  font-size: 13px;
}

.ability-row strong {
  color: #334155;
  text-align: right;
}

.assessment-input {
  display: grid;
  gap: 8px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
}

.assessment-input summary {
  color: #4f46e5;
  font-size: 13px;
  font-weight: 900;
  cursor: pointer;
}

.assessment-input label {
  margin-top: 10px;
}

.trace-list {
  display: grid;
  gap: 10px;
}

.trace-list li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 10px;
  align-items: center;
  min-width: 0;
  min-height: 48px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}

.trace-list li > div {
  min-width: 0;
}

.trace-list small {
  color: #64748b;
  font-size: 12px;
}

.empty-state {
  display: grid;
  gap: 6px;
  place-items: center;
  min-width: 0;
  padding: 18px;
  color: #64748b;
  text-align: center;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 16px;
}

.empty-state.tall {
  min-height: 180px;
}

.empty-state strong {
  color: #0f172a;
}

.soft-alert {
  max-height: 42px;
  padding: 9px 10px;
  overflow: hidden;
  color: #b91c1c;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 12px;
}

.card-footnote {
  margin-top: auto;
  padding-top: 4px;
}

.card-footnote.success::before {
  width: 9px;
  height: 9px;
  content: '';
  background: #10b981;
  border-radius: 999px;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

@media (max-width: 900px) {
  .learning-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .learning-card-grid,
  .upload-actions,
  .resource-actions,
  .mini-stat-grid,
  .chip-grid {
    grid-template-columns: 1fr;
  }

  .workspace-card {
    min-height: 0;
  }

  .learner-summary,
  .recent-files li,
  .trace-list li,
  .ability-row {
    grid-template-columns: 1fr;
  }

  .ability-row strong {
    text-align: left;
  }
}
</style>
