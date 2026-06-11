<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { CheckCircle2, ClipboardCheck, RefreshCw, ShieldCheck, XCircle } from 'lucide-vue-next'
import { decideResourceReview, listResourceReviews } from '../../api/reviews'
import type { ResourceReviewSummary, ReviewDecisionPayload } from '../../types/api'

const reviews = ref<ResourceReviewSummary[]>([])
const isLoading = ref(false)
const decidingReviewId = ref('')
const errorMessage = ref('')
const selectedReviewId = ref('')
const feedbackDraft = ref('')

const pendingCount = computed(
  () => reviews.value.filter((review) => review.status === 'PENDING_CRITIC').length,
)

const selectedReview = computed(
  () => reviews.value.find((review) => review.reviewId === selectedReviewId.value) ?? reviews.value[0] ?? null,
)

onMounted(() => {
  void loadReviews()
})

async function loadReviews() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    reviews.value = await listResourceReviews('PENDING_CRITIC')
    selectReview(reviews.value[0] ?? null)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Unable to load review queue'
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
        ? 'Teacher approved citation grounding, learner fit, and safety.'
        : 'Teacher requested stronger citations or learner-fit revisions.'),
  }
  try {
    const updated = await decideResourceReview(review.reviewId, payload)
    reviews.value = reviews.value.filter((item) => item.reviewId !== updated.reviewId)
    selectReview(reviews.value[0] ?? null)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Unable to submit review decision'
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
</script>

<template>
  <section class="workspace" aria-label="Teacher review queue">
    <header class="workspace-header">
      <div>
        <p class="eyebrow">教师端 / Critic Agent + Teacher Governance</p>
        <h2>教师 Review Queue / Teacher Review Queue</h2>
        <p class="header-note">审核引用、内容安全和画像适配，确保 AI 生成资源经过教师确认后再发布给学生。</p>
      </div>
      <button class="primary-action" type="button" :disabled="isLoading" @click="loadReviews">
        <RefreshCw :size="18" aria-hidden="true" />
        {{ isLoading ? '刷新审核队列中' : '刷新队列' }}
      </button>
    </header>

    <section class="metric-row" aria-label="Review queue summary">
      <article>
        <span>待审核任务</span>
        <strong>{{ pendingCount }}</strong>
        <p>Loaded from /api/reviews/resources</p>
      </article>
      <article>
        <span>发布闸口</span>
        <strong>Critic + Teacher</strong>
        <p>未批准前资源保持 blocked。</p>
      </article>
      <article>
        <span>Decision API</span>
        <strong>ACTIVE</strong>
        <p>当前支持 APPROVED / REVISION_REQUESTED。</p>
      </article>
    </section>

    <section class="review-layout review-workspace" data-test="teacher-review-workspace">
      <article class="panel review-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Selected decision / 当前审核决策</p>
            <h3>当前资源发布闸口</h3>
          </div>
          <ClipboardCheck :size="20" aria-hidden="true" />
        </div>
        <section v-if="selectedReview" class="review-detail decision-surface" data-test="review-detail">
          <p class="eyebrow">选中资源</p>
          <h3>{{ resourceLabel(selectedReview) }}</h3>
          <dl class="detail-grid">
            <div>
              <dt>reviewId</dt>
              <dd>{{ selectedReview.reviewId }}</dd>
            </div>
            <div>
              <dt>resourceId</dt>
              <dd>{{ selectedReview.resourceId }}</dd>
            </div>
            <div>
              <dt>generationTaskId</dt>
              <dd>{{ selectedReview.generationTaskId }}</dd>
            </div>
            <div>
              <dt>审核状态</dt>
              <dd>{{ selectedReview.status }}</dd>
            </div>
          </dl>
          <p class="answer-text">{{ selectedReview.summary }}</p>
          <label class="field-control">
            <span>教师反馈区</span>
            <textarea
              v-model="feedbackDraft"
              data-test="review-feedback-input"
              rows="4"
              placeholder="请填写审核意见，说明批准或退回修改原因"
            ></textarea>
          </label>
          <div class="review-actions detail-actions">
            <button
              class="tool-button"
              type="button"
              data-test="approve-selected-review"
              :disabled="decidingReviewId === selectedReview.reviewId"
              @click="decide(selectedReview, 'APPROVED')"
            >
              <CheckCircle2 :size="15" aria-hidden="true" />
              批准 Approve
            </button>
            <button
              class="tool-button warning"
              type="button"
              data-test="request-revision"
              :disabled="decidingReviewId === selectedReview.reviewId"
              @click="decide(selectedReview, 'REVISION_REQUESTED')"
            >
              <XCircle :size="15" aria-hidden="true" />
              退回修改 Return
            </button>
            <button
              class="tool-button danger"
              type="button"
              data-test="reject-review"
              disabled
              title="当前前端原型展示目标态按钮；后端决策类型接入后再启用"
            >
              <XCircle :size="15" aria-hidden="true" />
              拒绝 Reject
            </button>
          </div>
        </section>
        <section v-else class="review-detail decision-surface" data-test="review-detail">
          <p class="eyebrow">选中资源</p>
          <h3>暂无待审核任务</h3>
          <p class="answer-text">No pending resources / 暂无待审核资源。目标态审核按钮保持禁用，避免调用未实现决策。</p>
          <div class="review-actions detail-actions">
            <button class="tool-button" type="button" disabled>
              <CheckCircle2 :size="15" aria-hidden="true" />
              批准 Approve
            </button>
            <button class="tool-button warning" type="button" disabled>
              <XCircle :size="15" aria-hidden="true" />
              退回修改 Return
            </button>
            <button class="tool-button danger" type="button" data-test="reject-review" disabled>
              <XCircle :size="15" aria-hidden="true" />
              拒绝 Reject
            </button>
          </div>
        </section>
        <div class="panel-subheading">
          <p class="eyebrow">审核检查面板</p>
          <h4>引用检查 Citation Check / 安全检查 Safety Check / 画像适配 Profile Fit</h4>
        </div>
        <ul class="rubric-list evidence-checklist" data-test="teacher-evidence-checklist">
          <li><CheckCircle2 :size="16" aria-hidden="true" /> 引用检查：课程 citation 存在且可追溯。</li>
          <li><CheckCircle2 :size="16" aria-hidden="true" /> 画像适配：资源匹配当前路径节点和薄弱点。</li>
          <li><CheckCircle2 :size="16" aria-hidden="true" /> 安全检查：练习包含解题指导和误区诊断。</li>
          <li><XCircle :size="16" aria-hidden="true" /> 高风险：无依据结论、unsafe shortcut、no source 需退回或拒绝。</li>
        </ul>
      </article>

      <article class="panel review-card">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">待审核列表</p>
            <h3>Resources awaiting review</h3>
          </div>
          <ShieldCheck :size="20" aria-hidden="true" />
        </div>
        <p v-if="errorMessage" class="error-text" role="status">{{ errorMessage }}</p>
        <p v-if="!isLoading && reviews.length === 0" class="answer-text">No pending resources / 暂无待审核任务</p>
        <p v-else-if="isLoading && reviews.length === 0" class="answer-text">Loading review queue from backend governance APIs / 正在加载审核任务。</p>
        <ul v-else class="document-list">
          <li
            v-for="review in reviews"
            :key="review.reviewId"
            :class="{ selected: review.reviewId === selectedReview?.reviewId }"
          >
            <ClipboardCheck :size="16" aria-hidden="true" />
            <button
              class="review-select"
              type="button"
              :data-test="`select-review-${review.reviewId}`"
              @click="selectReview(review)"
            >
              <strong>{{ resourceLabel(review) }}</strong>
              <span>{{ review.resourceType ?? 'RESOURCE' }} / {{ review.generationTaskId }}</span>
              <span>{{ review.summary }}</span>
            </button>
            <em :class="['status-pill', review.status.toLowerCase()]">{{ review.status }}</em>
          </li>
        </ul>
      </article>

      <article class="panel review-history-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">审核历史</p>
            <h3>可追溯决策记录</h3>
          </div>
          <ClipboardCheck :size="20" aria-hidden="true" />
        </div>
        <ul class="review-history-list">
          <li><span class="status-dot approved"></span>10:15 AI 生成建议已记录</li>
          <li><span class="status-dot warning"></span>10:18 教师二次复核：引用待补强</li>
          <li><span class="status-dot rejected"></span>10:25 高风险项进入人工确认</li>
        </ul>
        <button class="tool-button secondary" type="button">查看 Agent Trace</button>
      </article>
    </section>
  </section>
</template>
