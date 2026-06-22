<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Activity,
  CheckCircle2,
  GitBranch,
  PlugZap,
  RefreshCw,
  ServerCog,
  ShieldCheck,
  Star,
} from 'lucide-vue-next'
import {
  PROVIDER_PRESETS,
  createModelProvider,
  listModelProviders,
  setDefaultModelProvider,
  testModelProviderConnection,
  updateModelProvider,
  type ModelProviderSummary,
} from '../../api/modelProviders'
import { type MobbinMetricItem } from '../../components/mobbin/MobbinMetricStrip.vue'
import MobbinPageShell from '../../components/mobbin/MobbinPageShell.vue'
import MobbinStatusPill from '../../components/mobbin/MobbinStatusPill.vue'
import MobbinTimeline, { type MobbinTimelineItem } from '../../components/mobbin/MobbinTimeline.vue'
import '../../components/mobbin/console-layout.css'

const providers = ref<ModelProviderSummary[]>([])
const isLoading = ref(false)
const isSaving = ref(false)
const testingProviderId = ref('')
const errorMessage = ref('')
const successMessage = ref('')
const selectedProviderId = ref('')

const form = reactive({
  providerCode: 'deepseek',
  displayName: PROVIDER_PRESETS.deepseek.displayName,
  remark: '',
  websiteUrl: PROVIDER_PRESETS.deepseek.websiteUrl,
  baseUrl: PROVIDER_PRESETS.deepseek.baseUrl,
  chatModel: PROVIDER_PRESETS.deepseek.chatModel,
  embeddingModel: PROVIDER_PRESETS.deepseek.embeddingModel,
  apiKey: '',
  enabled: true,
  defaultProvider: false,
  budget: '后端控制',
  priority: '普通',
})

const providerTemplates = [
  {
    code: 'deepseek',
    name: 'DeepSeek',
    detail: '适合中文学习场景的 OpenAI-compatible 模型服务。',
    status: '预设',
  },
  {
    code: 'openai',
    name: 'OpenAI',
    detail: 'OpenAI 官方生产端点，支持对话与向量模型。',
    status: '预设',
  },
  {
    code: 'dashscope',
    name: 'DashScope',
    detail: '阿里云 DashScope 兼容模式端点。',
    status: '预设',
  },
  {
    code: 'mimo',
    name: 'Xiaomi MiMo',
    detail: '小米 MiMo 模型服务预设。',
    status: '预设',
  },
  {
    code: 'custom',
    name: '自定义',
    detail: '接入任意 OpenAI 兼容 Base URL。',
    status: '手动',
  },
]

const isEditing = computed(() => selectedProviderId.value !== '')
const selectedProvider = computed(() => providers.value.find((provider) => provider.id === selectedProviderId.value) ?? null)
const primaryProvider = computed(
  () => providers.value.find((provider) => provider.defaultProvider) ?? providers.value.find((provider) => provider.enabled) ?? null,
)
const backupProvider = computed(
  () => providers.value.find((provider) => provider.enabled && provider.id !== primaryProvider.value?.id) ?? null,
)
const enabledCount = computed(() => providers.value.filter((provider) => provider.enabled).length)

const metrics = computed<MobbinMetricItem[]>(() => [
  { label: '默认 Provider', value: primaryProvider.value?.displayName ?? '未配置', note: primaryProvider.value?.providerCode ?? '需要设置默认通道' },
  { label: '启用数量', value: enabledCount.value, note: `${providers.value.length} 个已注册 Provider` },
  { label: '最近测试结果', value: successMessage.value ? '成功' : errorMessage.value ? '失败' : '暂无', note: successMessage.value || errorMessage.value || '当前会话尚未测试' },
  { label: 'Token 策略', value: form.budget, note: 'Token 消耗仍由后端治理策略控制' },
])

const providerMonitorItems = computed<MobbinTimelineItem[]>(() => [
  {
    title: '默认 Provider',
    subtitle: primaryProvider.value?.displayName ?? '未选择',
    detail: primaryProvider.value?.baseUrl ?? '保存并设为默认后生效。',
    status: primaryProvider.value?.enabled ? 'ACTIVE' : 'MISSING',
  },
  {
    title: '备用 Provider',
    subtitle: backupProvider.value?.displayName ?? '未配置备用 Provider',
    detail: backupProvider.value ? backupProvider.value.baseUrl : '配置备用通道后可支撑 Fallback。',
    status: backupProvider.value ? 'READY' : 'WAITING',
  },
  {
    title: 'Fallback 策略',
    subtitle: backupProvider.value ? '已具备备用通道' : '需要手动配置',
    detail: '失败切换策略仍由后端运行时执行，前端只展示配置状态。',
    status: backupProvider.value ? 'READY' : 'WATCH',
  },
  {
    title: '最近测试结果',
    subtitle: successMessage.value || errorMessage.value || '暂无测试结果',
    detail: '测试连接不会回显 API key 明文。',
    status: errorMessage.value ? 'FAILED' : successMessage.value ? 'SUCCEEDED' : 'IDLE',
  },
])

onMounted(() => {
  void loadProviders()
})

async function loadProviders() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    providers.value = await listModelProviders()
    if (!selectedProviderId.value && providers.value.length > 0) {
      selectProvider(providers.value[0])
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法加载模型供应商'
  } finally {
    isLoading.value = false
  }
}

function applyPreset(providerCode: string) {
  const preset = PROVIDER_PRESETS[providerCode]
  if (!preset) return
  form.providerCode = providerCode
  form.displayName = preset.displayName
  form.websiteUrl = preset.websiteUrl
  form.baseUrl = preset.baseUrl
  form.chatModel = preset.chatModel
  form.embeddingModel = preset.embeddingModel
}

function startProviderDraft(templateCode: string) {
  selectedProviderId.value = ''
  form.apiKey = ''
  form.defaultProvider = false
  form.enabled = true
  form.budget = '后端控制'
  form.priority = templateCode === 'openai' ? '高' : '普通'

  if (templateCode in PROVIDER_PRESETS) {
    applyPreset(templateCode)
    return
  }

  form.providerCode = 'custom'
  form.displayName = providerTemplates.find((template) => template.code === templateCode)?.name ?? '自定义'
  form.remark = templateCode === 'custom' ? '' : `${form.displayName} 自定义端点`
  form.websiteUrl = ''
  form.baseUrl = ''
  form.chatModel = ''
  form.embeddingModel = ''
}

function selectProvider(provider: ModelProviderSummary) {
  selectedProviderId.value = provider.id
  form.providerCode = provider.providerCode
  form.displayName = provider.displayName
  form.remark = provider.remark ?? ''
  form.websiteUrl = provider.websiteUrl ?? ''
  form.baseUrl = provider.baseUrl
  form.chatModel = provider.chatModel ?? ''
  form.embeddingModel = provider.embeddingModel ?? ''
  form.apiKey = provider.apiKeyMasked ?? ''
  form.enabled = provider.enabled
  form.defaultProvider = provider.defaultProvider
  form.budget = '后端控制'
  form.priority = provider.defaultProvider ? '高' : provider.enabled ? '普通' : '暂停'
}

function resetForm() {
  selectedProviderId.value = ''
  form.apiKey = ''
  form.defaultProvider = false
  form.enabled = true
  form.budget = '后端控制'
  form.priority = '普通'
  applyPreset('deepseek')
}

async function saveProvider() {
  isSaving.value = true
  errorMessage.value = ''
  successMessage.value = ''
  const payload = {
    providerCode: form.providerCode,
    displayName: form.displayName.trim(),
    remark: form.remark.trim() || undefined,
    websiteUrl: form.websiteUrl.trim() || undefined,
    baseUrl: form.baseUrl.trim(),
    chatModel: form.chatModel.trim() || undefined,
    embeddingModel: form.embeddingModel.trim() || undefined,
    apiKey: form.apiKey.trim() || undefined,
    enabled: form.enabled,
    defaultProvider: form.defaultProvider,
  }
  try {
    const saved = isEditing.value
      ? await updateModelProvider(selectedProviderId.value, payload)
      : await createModelProvider(payload)
    successMessage.value = isEditing.value ? '模型供应商配置已更新。' : '模型供应商已创建。'
    await loadProviders()
    selectProvider(saved)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法保存模型供应商'
  } finally {
    isSaving.value = false
  }
}

async function markDefault(provider: ModelProviderSummary) {
  errorMessage.value = ''
  try {
    const updated = await setDefaultModelProvider(provider.id)
    successMessage.value = `${updated.displayName} 已设为默认 Provider。`
    await loadProviders()
    selectProvider(updated)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法设置默认 Provider'
  }
}

async function testConnection(provider: ModelProviderSummary) {
  testingProviderId.value = provider.id
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const result = await testModelProviderConnection(provider.id)
    if (result.status === 'SUCCEEDED') {
      successMessage.value = `${provider.displayName} 测试调用成功，用时 ${result.latencyMs} ms。`
    } else {
      errorMessage.value = `${provider.displayName} 测试调用失败，${result.errorCode ?? 'UNKNOWN'}`
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '无法测试 Provider 连接'
  } finally {
    testingProviderId.value = ''
  }
}
</script>

<template>
  <MobbinPageShell aria-label="模型供应商中心" data-test="admin-model-providers">
    <section class="status-bar provider-control-bar">
      <article class="status-cell">
        <span>当前默认 Provider</span>
        <strong>{{ primaryProvider?.displayName ?? '未配置' }}</strong>
        <p>{{ primaryProvider?.providerCode ?? '需要设置默认通道' }}</p>
      </article>
      <article class="status-cell">
        <span>备用 Provider</span>
        <strong>{{ backupProvider?.displayName ?? '未配置' }}</strong>
        <p>{{ backupProvider?.baseUrl ?? '配置备用通道后支持 Fallback' }}</p>
      </article>
      <article class="status-cell">
        <span>Provider 数量</span>
        <strong>{{ providers.length }}</strong>
        <p>{{ enabledCount }} 个已启用</p>
      </article>
      <article v-for="item in metrics.slice(2, 4)" :key="item.label" class="status-cell">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.note }}</p>
      </article>
      <button class="mobbin-primary-button" type="button" :disabled="isLoading" @click="loadProviders">
        <RefreshCw :size="18" aria-hidden="true" />
        {{ isLoading ? '刷新中' : '刷新 Provider' }}
      </button>
    </section>

    <p v-if="errorMessage" class="mobbin-error" role="status">{{ errorMessage }}</p>
    <p v-if="successMessage" class="mobbin-success" role="status">{{ successMessage }}</p>

    <section class="three-column-desk provider-orchestration-layout">
      <aside class="provider-marketplace" aria-label="Provider 市场">
        <div class="panel-heading compact">
          <div>
            <span class="surface-eyebrow">模型供应商市场</span>
            <h1>模型市场</h1>
          </div>
          <ServerCog :size="20" aria-hidden="true" />
        </div>

        <div class="marketplace-grid">
          <button
            v-for="template in providerTemplates"
            :key="template.code"
            type="button"
            :class="['marketplace-card', { selected: form.providerCode === template.code && !selectedProviderId }]"
            @click="startProviderDraft(template.code)"
          >
            <span class="provider-logo">{{ template.name.slice(0, 2).toUpperCase() }}</span>
            <span class="marketplace-copy">
              <strong>{{ template.name }}</strong>
              <p>{{ template.detail }}</p>
              <MobbinStatusPill :status="template.status">{{ template.status }}</MobbinStatusPill>
            </span>
          </button>
        </div>

        <button class="mobbin-secondary-button" type="button" data-test="new-provider" @click="resetForm">
          <PlugZap :size="16" aria-hidden="true" />
          新建 DeepSeek 供应商
        </button>

        <section class="registered-providers">
          <div class="panel-heading compact">
            <h2>已注册连接</h2>
            <ShieldCheck :size="18" aria-hidden="true" />
          </div>
          <ul v-if="providers.length" class="connection-list">
            <li v-for="provider in providers" :key="provider.id" :class="{ selected: provider.id === selectedProviderId }">
              <button
                type="button"
                class="connection-main"
                :data-test="'select-provider-' + provider.providerCode"
                @click="selectProvider(provider)"
              >
                <span class="provider-logo small">{{ provider.displayName.slice(0, 2).toUpperCase() }}</span>
                <span>
                  <strong>{{ provider.displayName }}</strong>
                  <small>{{ provider.providerCode }} / {{ provider.baseUrl }}</small>
                </span>
              </button>
              <div class="connection-meta">
                <MobbinStatusPill :status="provider.enabled ? 'ENABLED' : 'DISABLED'">
                  {{ provider.defaultProvider ? '默认' : provider.enabled ? '已启用' : '已停用' }}
                </MobbinStatusPill>
                <span>对话模型：{{ provider.chatModel ?? '未配置' }}</span>
                <span>向量模型：{{ provider.embeddingModel ?? '未配置' }}</span>
              </div>
            </li>
          </ul>
          <p v-else class="mobbin-empty">暂无 Provider。请从预设或自定义端点创建第一个 Provider。</p>
        </section>
      </aside>

      <main class="config-canvas provider-config-canvas" data-test="provider-form">
        <div class="canvas-heading">
          <div>
            <span class="surface-eyebrow">配置画布</span>
            <h1>{{ form.displayName || 'Provider 配置' }}</h1>
            <p>当前画布仅编辑 Provider 配置，不改变后端路由和业务请求流程。</p>
          </div>
          <MobbinStatusPill :status="form.enabled ? 'ENABLED' : 'DISABLED'">
            {{ form.enabled ? '已启用' : '已停用' }}
          </MobbinStatusPill>
        </div>

        <section class="config-section">
          <h2>基础信息</h2>
          <div class="form-grid two-col">
            <label>
              Provider 类型
              <select v-model="form.providerCode" :disabled="isEditing" @change="applyPreset(form.providerCode)">
                <option value="deepseek">DeepSeek</option>
                <option value="mimo">Xiaomi MiMo</option>
                <option value="dashscope">DashScope</option>
                <option value="openai">OpenAI</option>
                <option value="custom">自定义</option>
              </select>
            </label>
            <label>
              展示名称
              <input v-model="form.displayName" type="text" placeholder="Provider 展示名称" />
            </label>
            <label class="wide-field">
              备注
              <input v-model="form.remark" type="text" placeholder="可选运维备注" />
            </label>
            <label class="wide-field">
              官网
              <input v-model="form.websiteUrl" type="url" placeholder="https://..." />
            </label>
          </div>
        </section>

        <section class="config-section">
          <h2>连接信息</h2>
          <div class="form-grid">
            <label>
              Base URL
              <input v-model="form.baseUrl" type="url" placeholder="https://api.example.com/v1" />
            </label>
            <label>
              API key 已脱敏
              <input
                v-model="form.apiKey"
                type="password"
                :placeholder="selectedProvider?.apiKeyConfigured ? '留空则保留当前加密 key' : '输入 API key'"
              />
            </label>
          </div>
        </section>

        <section class="config-section">
          <h2>模型配置</h2>
          <div class="form-grid two-col">
            <label>
              对话模型
              <input v-model="form.chatModel" type="text" placeholder="对话模型 ID" />
            </label>
            <label>
              向量模型
              <input v-model="form.embeddingModel" type="text" placeholder="向量模型 ID" />
            </label>
          </div>
        </section>
      </main>

      <aside class="routing-panel provider-routing-panel" aria-label="Provider 路由策略">
        <div class="panel-heading compact">
          <div>
            <span class="surface-eyebrow">路由检查器</span>
            <h2>编排策略</h2>
          </div>
          <GitBranch :size="20" aria-hidden="true" />
        </div>

        <section class="routing-section">
          <h3>启用状态</h3>
          <label class="checkbox-row">
            <input v-model="form.enabled" type="checkbox" />
            已启用
          </label>
          <label class="checkbox-row">
            <input v-model="form.defaultProvider" type="checkbox" />
            默认 Provider
          </label>
        </section>

        <section class="routing-section">
          <h3>治理策略</h3>
          <div class="form-grid">
            <label>
              预算
              <input v-model="form.budget" type="text" placeholder="后端控制" />
            </label>
            <label>
              优先级
              <select v-model="form.priority">
                <option>高</option>
                <option>普通</option>
                <option>低</option>
                <option>暂停</option>
              </select>
            </label>
          </div>
        </section>

        <section class="routing-section policy-stack">
          <h3>Fallback / 超时 / 重试</h3>
          <article>
            <span>Fallback</span>
            <strong>{{ backupProvider ? '已就绪' : '等待配置' }}</strong>
            <p>{{ backupProvider?.displayName ?? '需要至少一个备用 Provider' }}</p>
          </article>
          <article>
            <span>超时</span>
            <strong>后端治理</strong>
            <p>前端仅展示策略，不修改运行时契约。</p>
          </article>
          <article>
            <span>重试</span>
            <strong>{{ form.priority }}</strong>
            <p>优先级用于运维识别，实际重试仍由后端控制。</p>
          </article>
        </section>

        <div class="action-row routing-actions">
          <button class="mobbin-primary-button" type="button" :disabled="isSaving" data-test="save-provider" @click="saveProvider">
            <CheckCircle2 :size="16" aria-hidden="true" />
            {{ isSaving ? '正在保存' : isEditing ? '更新 Provider' : '创建 Provider' }}
          </button>
          <button
            v-if="selectedProvider"
            class="mobbin-secondary-button"
            type="button"
            :disabled="testingProviderId === selectedProvider.id"
            data-test="test-provider"
            @click="testConnection(selectedProvider)"
          >
            <Activity :size="16" aria-hidden="true" />
            {{ testingProviderId === selectedProvider.id ? '正在测试调用' : '测试连接' }}
          </button>
          <button
            v-if="selectedProvider && !selectedProvider.defaultProvider"
            class="mobbin-secondary-button"
            type="button"
            data-test="set-default-provider"
            @click="markDefault(selectedProvider)"
          >
            <Star :size="16" aria-hidden="true" />
            设为默认
          </button>
        </div>
      </aside>
    </section>

    <section class="provider-monitor-strip">
      <div class="panel-heading compact">
        <div>
          <span class="surface-eyebrow">Provider 运行监控</span>
          <h2>运行监控</h2>
        </div>
        <Activity :size="18" aria-hidden="true" />
      </div>
      <MobbinTimeline :items="providerMonitorItems" />
    </section>
  </MobbinPageShell>
</template>

<style scoped>
.provider-control-bar {
  grid-template-columns: repeat(5, minmax(140px, 1fr)) auto;
}

.provider-orchestration-layout {
  grid-template-columns: minmax(260px, 300px) minmax(0, 1fr) minmax(300px, 340px);
}

.mobbin-primary-button,
.mobbin-secondary-button {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 10px 14px;
  font: inherit;
  font-weight: 900;
  border-radius: 12px;
  cursor: pointer;
}

.mobbin-primary-button {
  color: #ffffff;
  background: linear-gradient(135deg, #4f46e5, #2563eb);
  border: 1px solid transparent;
  box-shadow: 0 14px 26px rgba(79, 70, 229, 0.22);
}

.mobbin-secondary-button {
  color: #4f46e5;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
}

.panel-heading,
.canvas-heading {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.panel-heading h1,
.panel-heading h2,
.canvas-heading h1,
.config-section h2,
.routing-section h3,
.provider-monitor-strip h2 {
  margin: 4px 0 0;
  color: #0f172a;
  font-size: 20px;
  line-height: 1.2;
  letter-spacing: 0;
  overflow-wrap: anywhere;
}

.panel-heading.compact h2,
.routing-section h3,
.config-section h2 {
  font-size: 16px;
}

.canvas-heading p,
.marketplace-card p,
.connection-main small,
.connection-meta span,
.policy-stack p,
.mobbin-empty {
  color: #64748b;
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.marketplace-copy,
.connection-main span:last-child {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.provider-logo {
  display: grid;
  width: 44px;
  height: 44px;
  place-items: center;
  color: #ffffff;
  font-size: 12px;
  font-weight: 900;
  background: linear-gradient(135deg, #0f172a, #4f46e5);
  border-radius: 15px;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.14);
}

.provider-logo.small {
  width: 38px;
  height: 38px;
  border-radius: 13px;
}

.marketplace-card strong,
.connection-main strong,
.policy-stack strong {
  color: #0f172a;
  font-size: 14px;
  overflow-wrap: anywhere;
}

.registered-providers,
.config-section,
.routing-section,
.provider-monitor-strip {
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
}

.provider-monitor-strip {
  padding: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(248, 250, 252, 0.86)),
    #ffffff;
  border-color: rgba(226, 232, 240, 0.9);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.07);
}

.connection-list {
  display: grid;
  gap: 10px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.connection-list li {
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
}

.connection-list li.selected {
  background:
    linear-gradient(#f8fbff, #f8fbff) padding-box,
    linear-gradient(135deg, #4f46e5, #14b8a6) border-box;
  border-color: transparent;
}

.connection-main {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-width: 0;
  padding: 0;
  color: inherit;
  text-align: left;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.connection-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  align-items: center;
}

.form-grid {
  display: grid;
  gap: 12px;
}

.form-grid.two-col {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.form-grid label {
  display: grid;
  gap: 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
}

.form-grid input,
.form-grid select {
  width: 100%;
  min-height: 42px;
  padding: 10px 12px;
  color: #0f172a;
  font: inherit;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
}

.wide-field {
  grid-column: 1 / -1;
}

.checkbox-row {
  display: flex !important;
  gap: 8px;
  align-items: center;
  min-height: 42px;
  padding: 10px 12px;
  color: #0f172a !important;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
}

.checkbox-row input {
  width: auto;
  min-height: 0;
  padding: 0;
}

.policy-stack article {
  display: grid;
  gap: 5px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
}

.policy-stack span {
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
}

.routing-actions {
  align-items: stretch;
}

.routing-actions > button {
  width: 100%;
}

.mobbin-empty,
.mobbin-error,
.mobbin-success {
  padding: 13px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 16px;
}

.mobbin-error {
  color: #b91c1c;
  background: #fef2f2;
  border-color: #fecaca;
}

.mobbin-success {
  color: #047857;
  background: #ecfdf5;
  border-color: #a7f3d0;
}

@media (max-width: 1180px) {
  .provider-control-bar,
  .provider-orchestration-layout,
  .form-grid.two-col {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .panel-heading,
  .canvas-heading,
  .connection-main {
    display: grid;
    grid-template-columns: 1fr;
    justify-content: stretch;
  }

  .mobbin-primary-button,
  .mobbin-secondary-button {
    width: 100%;
  }
}
</style>
