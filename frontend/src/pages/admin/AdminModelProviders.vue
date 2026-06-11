<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Activity, CheckCircle2, PlugZap, RefreshCw, ServerCog, Star } from 'lucide-vue-next'
import {
  PROVIDER_PRESETS,
  createModelProvider,
  listModelProviders,
  setDefaultModelProvider,
  testModelProviderConnection,
  updateModelProvider,
  type ModelProviderSummary,
} from '../../api/modelProviders'

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
})

const isEditing = computed(() => selectedProviderId.value !== '')
const selectedProvider = computed(
  () => providers.value.find((provider) => provider.id === selectedProviderId.value) ?? null,
)

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
    errorMessage.value = error instanceof Error ? error.message : 'Unable to load model providers'
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
}

function resetForm() {
  selectedProviderId.value = ''
  form.apiKey = ''
  form.defaultProvider = false
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
    successMessage.value = isEditing.value ? '供应商配置已更新' : '供应商已创建'
    await loadProviders()
    selectProvider(saved)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Unable to save model provider'
  } finally {
    isSaving.value = false
  }
}

async function markDefault(provider: ModelProviderSummary) {
  errorMessage.value = ''
  try {
    const updated = await setDefaultModelProvider(provider.id)
    successMessage.value = `${updated.displayName} 已设为默认供应商`
    await loadProviders()
    selectProvider(updated)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Unable to set default provider'
  }
}

async function testConnection(provider: ModelProviderSummary) {
  testingProviderId.value = provider.id
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const result = await testModelProviderConnection(provider.id)
    if (result.status === 'SUCCEEDED') {
      successMessage.value = `${provider.displayName} 连通性测试成功（${result.latencyMs} ms）`
    } else {
      errorMessage.value = `${provider.displayName} 连通性测试失败：${result.errorCode ?? 'UNKNOWN'}`
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Unable to test provider connection'
  } finally {
    testingProviderId.value = ''
  }
}
</script>

<template>
  <section class="workspace" aria-label="Admin model providers" data-test="admin-model-providers">
    <header class="workspace-header">
      <div>
        <p class="eyebrow">管理员端 / Model Provider Registry</p>
        <h2>模型供应商配置</h2>
        <p class="header-note">
          配置 DeepSeek、MiMo、DashScope 等 OpenAI-compatible 供应商。API Key 仅保存在后端加密存储，前端不回显完整密钥。
        </p>
      </div>
      <button class="primary-action" type="button" :disabled="isLoading" @click="loadProviders">
        <RefreshCw :size="18" aria-hidden="true" />
        {{ isLoading ? '刷新中' : '刷新列表' }}
      </button>
    </header>

    <section class="workbench-grid">
      <article class="panel triage-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">已注册供应商</p>
            <h3>Provider 列表</h3>
          </div>
          <ServerCog :size="20" aria-hidden="true" />
        </div>
        <button class="secondary-action" type="button" data-test="new-provider" @click="resetForm">
          新建供应商
        </button>
        <ul v-if="providers.length" class="document-list">
          <li v-for="provider in providers" :key="provider.id">
            <PlugZap :size="16" aria-hidden="true" />
            <button
              type="button"
              class="link-button"
              :data-test="`select-provider-${provider.providerCode}`"
              @click="selectProvider(provider)"
            >
              <strong>{{ provider.displayName }}</strong>
              <span>{{ provider.providerCode }} / {{ provider.baseUrl }}</span>
            </button>
            <em :class="['status-pill', provider.enabled ? 'approved' : 'failed']">
              {{ provider.defaultProvider ? 'DEFAULT' : provider.enabled ? 'ENABLED' : 'DISABLED' }}
            </em>
          </li>
        </ul>
        <p v-else class="answer-text">暂无供应商，请创建第一个模型 provider。</p>
      </article>

      <article class="panel triage-panel" data-test="provider-form">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">{{ isEditing ? '编辑供应商' : '新建供应商' }}</p>
            <h3>{{ form.displayName || 'Provider 表单' }}</h3>
          </div>
          <Activity :size="20" aria-hidden="true" />
        </div>

        <p v-if="errorMessage" class="error-text" role="status">{{ errorMessage }}</p>
        <p v-if="successMessage" class="answer-text" role="status">{{ successMessage }}</p>

        <div class="form-grid">
          <label>
            供应商类型
            <select v-model="form.providerCode" :disabled="isEditing" @change="applyPreset(form.providerCode)">
              <option value="deepseek">DeepSeek</option>
              <option value="mimo">Xiaomi MiMo</option>
              <option value="dashscope">DashScope</option>
              <option value="openai">OpenAI</option>
              <option value="custom">Custom</option>
            </select>
          </label>
          <label>
            名称
            <input v-model="form.displayName" type="text" placeholder="显示名称" />
          </label>
          <label>
            备注
            <input v-model="form.remark" type="text" placeholder="可选备注" />
          </label>
          <label>
            官网
            <input v-model="form.websiteUrl" type="url" placeholder="https://..." />
          </label>
          <label>
            Base URL
            <input v-model="form.baseUrl" type="url" placeholder="https://api.example.com/v1" />
          </label>
          <label>
            Chat Model
            <input v-model="form.chatModel" type="text" placeholder="chat model id" />
          </label>
          <label>
            Embedding Model
            <input v-model="form.embeddingModel" type="text" placeholder="embedding model id" />
          </label>
          <label>
            API Key
            <input
              v-model="form.apiKey"
              type="password"
              :placeholder="selectedProvider?.apiKeyConfigured ? '留空则保留现有密钥' : '输入 API Key'"
            />
          </label>
          <label class="checkbox-row">
            <input v-model="form.enabled" type="checkbox" />
            启用
          </label>
          <label class="checkbox-row">
            <input v-model="form.defaultProvider" type="checkbox" />
            设为默认
          </label>
        </div>

        <div class="action-row">
          <button class="primary-action" type="button" :disabled="isSaving" data-test="save-provider" @click="saveProvider">
            <CheckCircle2 :size="16" aria-hidden="true" />
            {{ isSaving ? '保存中' : isEditing ? '更新配置' : '创建供应商' }}
          </button>
          <button
            v-if="selectedProvider"
            class="secondary-action"
            type="button"
            :disabled="testingProviderId === selectedProvider.id"
            data-test="test-provider"
            @click="testConnection(selectedProvider)"
          >
            {{ testingProviderId === selectedProvider.id ? '测试中' : '测试连通性' }}
          </button>
          <button
            v-if="selectedProvider && !selectedProvider.defaultProvider"
            class="secondary-action"
            type="button"
            data-test="set-default-provider"
            @click="markDefault(selectedProvider)"
          >
            <Star :size="16" aria-hidden="true" />
            设为默认
          </button>
        </div>
      </article>
    </section>
  </section>
</template>

<style scoped>
.form-grid {
  display: grid;
  gap: 0.85rem;
}

.form-grid label {
  display: grid;
  gap: 0.35rem;
  font-size: 0.92rem;
}

.form-grid input,
.form-grid select {
  width: 100%;
}

.checkbox-row {
  display: flex !important;
  align-items: center;
  gap: 0.5rem;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 1rem;
}

.secondary-action {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  border: 1px solid color-mix(in oklab, var(--ink) 18%, transparent);
  background: color-mix(in oklab, var(--surface) 92%, white);
  border-radius: 999px;
  padding: 0.55rem 0.95rem;
  cursor: pointer;
}

.link-button {
  background: none;
  border: 0;
  padding: 0;
  text-align: left;
  color: inherit;
  cursor: pointer;
}
</style>
