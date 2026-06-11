<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Activity, BarChart3, Database, GitBranch, ServerCog, WalletCards } from 'lucide-vue-next'
import { fetchAnalyticsOverview, fetchPersistedOpsAlerts, acknowledgeOpsAlert } from '../../api/analytics'
import { fetchHealth } from '../../api/health'
import type { AnalyticsOverview, ComponentHealthResponse, HealthResponse, OpsAlertRecord } from '../../types/api'

const health = ref<HealthResponse | null>(null)
const analytics = ref<AnalyticsOverview | null>(null)
const alerts = ref<OpsAlertRecord[]>([])
const acknowledgingAlertId = ref('')
const isLoading = ref(false)
const errorMessage = ref('')

const healthItems = computed(() => {
  if (!health.value) return []
  return [
    { name: 'Application', component: health.value.application, icon: Activity },
    { name: 'Database', component: health.value.database, icon: Database },
    { name: 'Redis', component: health.value.redis, icon: ServerCog },
    { name: 'MinIO', component: health.value.minio, icon: ServerCog },
    { name: 'Model provider', component: health.value.model, icon: GitBranch },
    ...(health.value.vector
      ? [{ name: 'Vector index', component: health.value.vector, icon: ServerCog }]
      : []),
  ]
})

const runtimeStats = computed(() => [
  {
    label: 'Runtime health / 系统健康',
    value: health.value?.application.status ?? (isLoading.value ? 'LOADING' : 'UNKNOWN'),
    note: health.value?.application.detail ?? 'Fetched from /api/health.',
  },
  {
    label: 'Database',
    value: health.value?.database.status ?? 'UNKNOWN',
    note: metadataSummary(health.value?.database),
  },
  {
    label: 'Model provider',
    value: health.value?.model.status ?? 'UNKNOWN',
    note: metadataSummary(health.value?.model),
  },
  {
    label: 'Agent tasks / Agent 成功率',
    value: String(analytics.value?.agentTaskCount ?? 0),
    note: `${analytics.value?.modelCallCount ?? 0} model calls logged`,
  },
  {
    label: 'Token activity / 今日 Token',
    value: String(analytics.value?.tokenUsage.totalTokens ?? 0),
    note: `${analytics.value?.tokenUsage.promptTokens ?? 0} prompt / ${analytics.value?.tokenUsage.completionTokens ?? 0} completion`,
  },
])

const learningStats = computed(() => [
  { label: 'Answers', value: analytics.value?.answerRecordCount ?? 0 },
  { label: 'Wrong questions', value: analytics.value?.wrongQuestionCount ?? 0 },
  { label: 'Learning events', value: analytics.value?.learningEventCount ?? 0 },
])

const reviewStatusItems = computed(() =>
  Object.entries(analytics.value?.resourceReviewStatusCounts ?? {}).map(([status, count]) => ({
    status,
    count,
  })),
)

onMounted(() => {
  void loadOperations()
})

async function loadOperations() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const [healthResponse, analyticsResponse, alertsResponse] = await Promise.all([
      fetchHealth(),
      fetchAnalyticsOverview(),
      fetchPersistedOpsAlerts(),
    ])
    health.value = healthResponse
    analytics.value = analyticsResponse
    alerts.value = alertsResponse
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Unable to load operations data'
  } finally {
    isLoading.value = false
  }
}

function metadataSummary(component?: ComponentHealthResponse): string {
  if (!component) return 'Waiting for backend health data.'
  const entries = Object.entries(component.metadata)
  if (entries.length === 0) return component.detail
  return entries.map(([key, value]) => `${key}: ${value}`).join(' / ')
}

async function acknowledgeAlert(alert: OpsAlertRecord) {
  acknowledgingAlertId.value = alert.alertId
  errorMessage.value = ''
  try {
    const updated = await acknowledgeOpsAlert(alert.alertId)
    alerts.value = alerts.value.map((item) => (item.alertId === updated.alertId ? updated : item))
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Unable to acknowledge alert'
  } finally {
    acknowledgingAlertId.value = ''
  }
}
</script>

<template>
  <section class="workspace" aria-label="Admin operations">
    <header class="workspace-header">
      <div>
        <p class="eyebrow">Available signals / 管理员端</p>
        <h2>管理员 Operations Dashboard / Admin Operations</h2>
        <p class="header-note">基于当前 API 总览系统健康、Review backlog、Token activity 和 Learning activity；生产级观测指标保持待接入说明。</p>
      </div>
      <button class="primary-action" type="button" :disabled="isLoading" @click="loadOperations">
        <Activity :size="18" aria-hidden="true" />
        {{ isLoading ? '刷新运营数据中' : '刷新数据' }}
      </button>
    </header>

    <section class="summary-strip admin-triage" data-test="admin-triage">
      <article v-for="stat in runtimeStats" :key="stat.label">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
        <p>{{ stat.note }}</p>
      </article>
    </section>

    <section class="workbench-grid">
      <article class="panel admin-health-panel triage-panel" data-test="admin-dependency-matrix">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Dependency health / 服务状态卡片</p>
            <h3>运行时依赖</h3>
          </div>
          <ServerCog :size="20" aria-hidden="true" />
        </div>
        <p v-if="errorMessage" class="error-text" role="status">{{ errorMessage }}</p>
        <ul v-if="healthItems.length" class="document-list">
          <li v-for="item in healthItems" :key="item.name">
            <component :is="item.icon" :size="16" aria-hidden="true" />
            <div>
              <strong>{{ item.name }}</strong>
              <span>{{ item.component.detail }} / {{ metadataSummary(item.component) }}</span>
            </div>
            <em :class="['status-pill', item.component.status.toLowerCase()]">{{ item.component.status }}</em>
          </li>
        </ul>
        <p v-else class="answer-text">Loading runtime dependency health from /api/health / 正在加载服务状态。</p>
      </article>

      <article class="panel admin-cost-panel triage-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Analytics overview / 指标趋势与分析</p>
            <h3>Review backlog and learning activity</h3>
          </div>
          <WalletCards :size="20" aria-hidden="true" />
        </div>
        <p v-if="!analytics" class="answer-text">Loading analytics overview from /api/analytics/overview / 正在加载运营指标。</p>
        <ul v-else class="document-list">
          <li>
            <BarChart3 :size="16" aria-hidden="true" />
            <div>
              <strong>Agent tasks</strong>
              <span>{{ analytics.agentTaskCount }} tasks / {{ analytics.modelCallCount }} model calls</span>
            </div>
            <em class="status-pill active">{{ analytics.agentTaskCount }}</em>
          </li>
          <li>
            <WalletCards :size="16" aria-hidden="true" />
            <div>
              <strong>Token usage</strong>
              <span>{{ analytics.tokenUsage.promptTokens }} prompt / {{ analytics.tokenUsage.completionTokens }} completion</span>
            </div>
            <em class="status-pill active">{{ analytics.tokenUsage.totalTokens }}</em>
          </li>
          <li>
            <Activity :size="16" aria-hidden="true" />
            <div>
              <strong>Learning activity</strong>
              <span>Answers, wrong questions, and learning events from analytics overview</span>
            </div>
            <em class="status-pill active">{{ analytics.learningEventCount }}</em>
          </li>
          <li v-for="stat in learningStats" :key="stat.label">
            <Activity :size="16" aria-hidden="true" />
            <div>
              <strong>{{ stat.label }}</strong>
              <span>Analytics overview count</span>
            </div>
            <em class="status-pill active">{{ stat.value }}</em>
          </li>
          <li v-for="item in reviewStatusItems" :key="item.status">
            <ServerCog :size="16" aria-hidden="true" />
            <div>
              <strong>{{ item.status }}</strong>
              <span>Resource review status count</span>
            </div>
            <em :class="['status-pill', item.status.toLowerCase()]">{{ item.count }}</em>
          </li>
        </ul>
      </article>

      <article class="panel chart-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">指标趋势占位</p>
            <h3>Agent / Token / RAG / Review</h3>
          </div>
          <BarChart3 :size="20" aria-hidden="true" />
        </div>
        <div class="chart-grid">
          <div class="mini-chart line-chart"><span></span><span></span><span></span><span></span></div>
          <div class="mini-chart bar-chart"><span></span><span></span><span></span><span></span></div>
          <div class="mini-chart donut-chart">78.6%</div>
        </div>
        <p class="answer-text">图表区域为前端原型占位；真实 RAG 命中率、模型失败原因和链路覆盖率等待后端生产观测 API 接入。</p>
      </article>

      <article class="panel admin-alert-panel" data-test="admin-alert-table">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">异常告警区</p>
            <h3>持久化 Ops Alerts</h3>
          </div>
          <Activity :size="20" aria-hidden="true" />
        </div>
        <div v-if="alerts.length" class="alert-table">
          <div v-for="alert in alerts" :key="alert.alertId">
            <strong>{{ alert.severity }}</strong>
            <span>{{ alert.alertType }}</span>
            <span>{{ alert.summary }}</span>
            <button
              type="button"
              :disabled="alert.status === 'ACKNOWLEDGED' || acknowledgingAlertId === alert.alertId"
              @click="acknowledgeAlert(alert)"
            >
              {{ alert.status === 'ACKNOWLEDGED' ? '已处理' : '标记已处理' }}
            </button>
          </div>
        </div>
        <p v-else class="answer-text">暂无持久化告警；阈值触发后会写入 /api/analytics/ops/alerts/persisted。</p>
      </article>

      <article class="panel admin-api-panel" data-test="admin-api-sources">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">接口数据来源</p>
            <h3>当前可用 API</h3>
          </div>
          <Database :size="20" aria-hidden="true" />
        </div>
        <ul class="api-source-list expanded">
          <li>GET /api/health</li>
          <li>GET /api/analytics/overview</li>
          <li>GET /api/analytics/ops/alerts/persisted</li>
          <li>POST /api/analytics/ops/alerts/{alertId}/acknowledge</li>
          <li>GET /api/admin/model-providers</li>
          <li>GET /api/agent/tasks/{taskId}/trace</li>
          <li>GET /api/reviews/resources</li>
        </ul>
      </article>

      <article class="panel status-showcase" data-test="status-showcase-admin">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">状态展示示例</p>
            <h3>运维语义色</h3>
          </div>
          <ServerCog :size="20" aria-hidden="true" />
        </div>
        <div class="state-token-grid">
          <span class="state-token loading">loading</span>
          <span class="state-token failed">failed</span>
          <span class="state-token empty">empty</span>
          <span class="state-token warning">degraded</span>
          <span class="state-token failed">down</span>
          <span class="state-token approved">healthy</span>
          <span class="state-token pending">backlog warning</span>
        </div>
      </article>
    </section>
  </section>
</template>
