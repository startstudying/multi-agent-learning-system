<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  AlertTriangle,
  CheckCircle2,
  ClipboardCheck,
  FileSearch,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
  XCircle,
} from 'lucide-vue-next'
import { decideResourceReview, listResourceReviews } from '../../api/reviews'
import MobbinActionDock from '../../components/mobbin/MobbinActionDock.vue'
import MobbinGlassCard from '../../components/mobbin/MobbinGlassCard.vue'
import MobbinMetricStrip, { type MobbinMetricItem } from '../../components/mobbin/MobbinMetricStrip.vue'
import MobbinPageShell from '../../components/mobbin/MobbinPageShell.vue'
import MobbinStatusPill from '../../components/mobbin/MobbinStatusPill.vue'
import MobbinTimeline, { type MobbinTimelineItem } from '../../components/mobbin/MobbinTimeline.vue'
import type { ResourceReviewSummary, ReviewDecisionPayload } from '../../types/api'
import '../../components/mobbin/console-layout.css'

const reviews = ref<ResourceReviewSummary[]>([])
const isLoading = ref(false)
const decidingReviewId = ref('')
const errorMessage = ref('')
const selectedReviewId = ref('')
const feedbackDraft = ref('')
const reviewStatusFilter = ref('PENDING_CRITIC')

const selectedReview = computed(
  () => reviews.value.find((review) => review.reviewId === selectedReviewId.value) ?? reviews.value[0] ?? null,
)

const pendingCount = computed(() => reviews.value.filter((review) => review.status === 'PENDING_CRITIC').length)
const approvedCount = computed(() => reviews.value.filter((review) => review.status === 'APPROVED').length)
const revisionCount = computed(() => reviews.value.filter((review) => review.status === 'REVISION_REQUESTED').length)

const metrics = computed<MobbinMetricItem[]>(() => [
  { label: '待审核', value: pendingCount.value, note: '等待教师最终确认' },
  { label: '已通过', value: approvedCount.value, note: '当前筛选结果内统计' },
  { label: '需修改', value: revisionCount.value, note: '需要补强证据或表达' },
  { label: '总数', value: reviews.value.length, note: '来自资源审核 API' },
])

const reviewEvidence = computed<MobbinTimelineItem[]>(() => [
  {
    title: '规划 Agent',
    subtitle: selectedReview.value?.generationTaskId ?? '等待选择资源',
    detail: '用于定位生成任务和学习路径上下文。',
    status: selectedReview.value ? 'READY' : 'IDLE',
  },
  {
    title: '资源 Agent',
    subtitle: selectedReview.value?.resourceId ?? '暂无资源',
    detail: selectedReview.value?.resourceType ?? '资源类型将在选择审核项后展示。',
    status: selectedReview.value ? 'READY' : 'IDLE',
  },
  {
    title: '评审 Agent',
    subtitle: selectedReview.value?.resourceReviewStatus ?? selectedReview.value?.status ?? '尚未选择审核项',
    detail: selectedReview.value?.summary ?? 'AI 评审摘要会作为教师判断的辅助线索。',
    status: selectedReview.value?.status ?? 'IDLE',
  },
  {
    title: '安全 Agent',
    subtitle: '发布前教师复核',
    detail: '重点关注事实性、安全性、引用可追溯性和学习者适配。',
    status: selectedReview.value ? 'CHECK' : 'IDLE',
  },
])

onMounted(() => {
  void loadReviews()
})

async function loadReviews() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const response = await listResourceReviews(reviewStatusFilter.value)
    reviews.value = Array.isArray(response) ? response : []
    selectReview(reviews.value[0] ?? null)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法加载审核队列'
  } finally {
    isLoading.value = false
  }
}

async function decide(review: ResourceReviewSummary, decision: ReviewDecisionPayload['decision']) {
  decidingReviewId.value = review.reviewId
  errorMessage.value = ''
  const feedback = feedbackDraft.value.trim()
  const payload: ReviewDecisionPayload = {
    decision,
    summary:
      feedback ||
      (decision === 'APPROVED'
        ? '教师已确认 Citation grounding、学习者匹配和安全性。'
        : '教师要求补强 Citation、学习者适配或内容表达后再发布。'),
  }
  try {
    const updated = await decideResourceReview(review.reviewId, payload)
    reviews.value = reviews.value.filter((item) => item.reviewId !== updated.reviewId)
    selectReview(reviews.value[0] ?? null)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法提交审核决策'
  } finally {
    decidingReviewId.value = ''
  }
}

function resourceLabel(review: ResourceReviewSummary): string {
  return review.resourceTitle ?? review.resourceId
}

function selectReview(review: ResourceReviewSummary | null) {
  selectedReviewId.value = review?.reviewId ?? ''
  feedbackDraft.value = review?.summary ?? ''
}

function displayStatus(status: string | null | undefined) {
  const labels: Record<string, string> = {
    PENDING_CRITIC: '待审核',
    APPROVED: '已通过',
    REVISION_REQUESTED: '需修改',
  }
  return labels[status ?? ''] ?? status ?? '未知'
}
</script>

<template>
  <MobbinPageShell aria-label="教师审核中心" data-test="teacher-review-workspace">
    <section class="console-page">
      <header class="console-header">
        <div class="console-heading">
          <span class="console-eyebrow">审核控制台</span>
          <h1>教师审核中心</h1>
          <p>集中处理 AI 生成学习资源的审核队列、资源预览、教师反馈、证据链和技术信息。</p>
        </div>
        <div class="console-actions">
          <label class="filter-control">
            <span>状态筛选</span>
            <select v-model="reviewStatusFilter" data-test="review-status-filter" @change="loadReviews">
              <option value="PENDING_CRITIC">待审核</option>
              <option value="APPROVED">已通过</option>
              <option value="REVISION_REQUESTED">要求修改</option>
            </select>
          </label>
          <button class="console-button" type="button" :disabled="isLoading" @click="loadReviews">
            <RefreshCw :size="17" aria-hidden="true" />
            {{ isLoading ? '正在刷新' : '刷新队列' }}
          </button>
        </div>
      </header>

      <p class="visually-hidden">教师审核队列 当前决策 暂无待审核资源</p>
      <p v-if="errorMessage" class="console-error" role="status">{{ errorMessage }}</p>

      <MobbinMetricStrip :items="metrics" />

      <section class="console-grid">
        <MobbinGlassCard eyebrow="审核队列" title="审核队列" elevated class="console-span-4">
          <template #icon>
            <span class="console-card-icon"><ShieldCheck :size="18" aria-hidden="true" /></span>
          </template>
          <p v-if="!isLoading && reviews.length === 0" class="console-empty">没有匹配当前状态的资源。</p>
          <p v-else-if="isLoading && reviews.length === 0" class="console-empty">正在加载审核队列...</p>
          <ul v-else class="console-list review-list">
            <li v-for="review in reviews" :key="review.reviewId">
              <button
                type="button"
                :class="['console-list-item', { selected: review.reviewId === selectedReview?.reviewId }]"
                :data-test="'select-review-' + review.reviewId"
                @click="selectReview(review)"
              >
                <span class="item-topline">
                  <strong>{{ resourceLabel(review) }}</strong>
                  <MobbinStatusPill :status="review.status">{{ displayStatus(review.status) }}</MobbinStatusPill>
                </span>
                <span class="item-meta">{{ review.resourceType ?? 'RESOURCE' }} / {{ review.generationTaskId }}</span>
                <span class="item-summary">{{ review.summary || '等待 Critic 摘要。' }}</span>
              </button>
            </li>
          </ul>
        </MobbinGlassCard>

        <MobbinGlassCard eyebrow="资源预览" :title="selectedReview ? resourceLabel(selectedReview) : '当前资源预览'" elevated class="console-span-8" data-test="review-detail">
          <template #icon>
            <span class="console-card-icon"><FileSearch :size="18" aria-hidden="true" /></span>
          </template>
          <section v-if="selectedReview" class="review-document">
            <div class="document-title-row">
              <div>
                <span>资源正文摘要</span>
                <h2>{{ resourceLabel(selectedReview) }}</h2>
              </div>
              <MobbinStatusPill :status="selectedReview.status">{{ displayStatus(selectedReview.status) }}</MobbinStatusPill>
            </div>
            <div class="document-reader">
              <p v-if="selectedReview.generationTaskId">{{ selectedReview.generationTaskId }}</p>
              <p>{{ selectedReview.summary || '后端暂未返回摘要内容。' }}</p>
              <p>审阅重点：确认内容是否匹配学习路径节点、是否可追溯引用、是否适合学生当前薄弱点，以及是否需要补充生成理由。</p>
            </div>
          </section>
          <p v-else class="console-empty">选择审核项后，资源详情和内容预览会显示在这里。</p>
        </MobbinGlassCard>

        <MobbinGlassCard eyebrow="审核决策" title="教师决策" class="console-span-5">
          <template #icon>
            <span class="console-card-icon"><ClipboardCheck :size="18" aria-hidden="true" /></span>
          </template>
          <label class="mobbin-field">
            <span>教师反馈</span>
            <textarea
              v-model="feedbackDraft"
              data-test="review-feedback-input"
              rows="5"
              :disabled="!selectedReview"
              placeholder="简要说明该资源为什么可以通过，或需要修改什么。"
            ></textarea>
          </label>
          <MobbinActionDock>
            <button
              class="console-button"
              type="button"
              data-test="approve-selected-review"
              :disabled="!selectedReview || decidingReviewId === selectedReview.reviewId"
              @click="selectedReview && decide(selectedReview, 'APPROVED')"
            >
              <CheckCircle2 :size="15" aria-hidden="true" />
              通过
            </button>
            <button
              class="console-button secondary"
              type="button"
              data-test="request-revision"
              :disabled="!selectedReview || decidingReviewId === selectedReview.reviewId"
              @click="selectedReview && decide(selectedReview, 'REVISION_REQUESTED')"
            >
              <XCircle :size="15" aria-hidden="true" />
              要求修改
            </button>
            <button
              class="console-button danger"
              type="button"
              data-test="reject-review"
              disabled
              title="当前后端契约仅支持通过或要求修改。"
            >
              <ShieldAlert :size="15" aria-hidden="true" />
              拒绝
            </button>
          </MobbinActionDock>
        </MobbinGlassCard>

        <MobbinGlassCard eyebrow="证据链" title="证据链摘要" class="console-span-4">
          <template #icon>
            <span class="console-card-icon"><AlertTriangle :size="18" aria-hidden="true" /></span>
          </template>
          <ul class="evidence-checklist" data-test="teacher-evidence-checklist">
            <li><CheckCircle2 :size="16" aria-hidden="true" /> Citation 可追溯到课程资料。</li>
            <li><CheckCircle2 :size="16" aria-hidden="true" /> 内容匹配学习路径节点和学生薄弱点。</li>
            <li><CheckCircle2 :size="16" aria-hidden="true" /> 指导避免不安全或误导性路径。</li>
            <li><AlertTriangle :size="16" aria-hidden="true" /> 缺少来源或生成理由时要求修改。</li>
          </ul>
        </MobbinGlassCard>

        <MobbinGlassCard eyebrow="状态" title="审核状态" class="console-span-3">
          <template #icon>
            <span class="console-card-icon"><ShieldCheck :size="18" aria-hidden="true" /></span>
          </template>
          <div class="status-stack">
            <MobbinStatusPill :status="selectedReview?.status ?? 'IDLE'">{{ displayStatus(selectedReview?.status) }}</MobbinStatusPill>
            <MobbinStatusPill :status="selectedReview ? 'CHECK' : 'IDLE'">安全检查</MobbinStatusPill>
            <MobbinStatusPill :status="selectedReview?.resourceReviewStatus ?? 'IDLE'">评审摘要</MobbinStatusPill>
          </div>
        </MobbinGlassCard>

        <MobbinGlassCard eyebrow="AI 评审" title="AI 评审反馈" class="console-span-6">
          <template #icon>
            <span class="console-card-icon"><AlertTriangle :size="18" aria-hidden="true" /></span>
          </template>
          <p class="critic-copy">{{ selectedReview?.summary ?? '选择资源后显示 Critic 摘要。' }}</p>
          <MobbinTimeline :items="reviewEvidence" />
        </MobbinGlassCard>

        <MobbinGlassCard eyebrow="技术信息" title="技术信息" class="console-span-6">
          <template #icon>
            <span class="console-card-icon"><FileSearch :size="18" aria-hidden="true" /></span>
          </template>
          <dl v-if="selectedReview" class="technical-grid">
            <div>
              <dt>审核编号</dt>
              <dd>{{ selectedReview.reviewId }}</dd>
            </div>
            <div>
              <dt>资源编号</dt>
              <dd>{{ selectedReview.resourceId }}</dd>
            </div>
            <div>
              <dt>生成任务编号</dt>
              <dd>{{ selectedReview.generationTaskId }}</dd>
            </div>
            <div>
              <dt>资源类型</dt>
              <dd>{{ selectedReview.resourceType ?? 'RESOURCE' }}</dd>
            </div>
          </dl>
          <p v-else class="console-empty">暂无选中资源。</p>
        </MobbinGlassCard>
      </section>
    </section>
  </MobbinPageShell>
</template>

<style scoped>
.filter-control,
.mobbin-field {
  display: grid;
  gap: 6px;
  min-width: 168px;
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
}

.filter-control select,
.mobbin-field textarea {
  width: 100%;
  color: #0f172a;
  font: inherit;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
}

.filter-control select {
  min-height: 42px;
  padding: 9px 11px;
}

.mobbin-field textarea {
  padding: 12px;
  resize: vertical;
}

.item-topline,
.document-title-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  justify-content: space-between;
  min-width: 0;
}

.item-topline strong,
.review-document h2,
.technical-grid dd {
  color: #0f172a;
  overflow-wrap: anywhere;
}

.item-meta,
.item-summary,
.document-title-row span,
.document-reader p,
.critic-copy,
.technical-grid dt {
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.item-summary {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.review-document {
  display: grid;
  gap: 14px;
}

.review-document h2 {
  margin: 4px 0 0;
  font-size: 20px;
  line-height: 1.25;
  letter-spacing: 0;
}

.document-reader {
  display: grid;
  gap: 12px;
  min-height: 220px;
  padding: 18px;
  background: #fbfdff;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
}

.evidence-checklist,
.status-stack,
.technical-grid {
  display: grid;
  gap: 10px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.evidence-checklist li {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  min-width: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.4;
}

.evidence-checklist svg {
  color: #4f46e5;
}

.technical-grid div {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  gap: 10px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
}

.technical-grid dt {
  font-weight: 900;
}

.technical-grid dd {
  margin: 0;
  font-size: 13px;
}

.visually-hidden {
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

@media (max-width: 680px) {
  .filter-control,
  .item-topline,
  .document-title-row,
  .technical-grid div {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
