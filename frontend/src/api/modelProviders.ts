import { adminApiRequest } from './client'

export interface ModelProviderSummary {
  id: string
  providerCode: string
  displayName: string
  remark: string | null
  websiteUrl: string | null
  baseUrl: string
  chatModel: string | null
  embeddingModel: string | null
  apiKeyConfigured: boolean
  apiKeyMasked: string | null
  enabled: boolean
  defaultProvider: boolean
  createdBy: string | null
  createdAt: string
  updatedAt: string
}

export interface ModelProviderUpsertPayload {
  providerCode: string
  displayName: string
  remark?: string
  websiteUrl?: string
  baseUrl: string
  chatModel?: string
  embeddingModel?: string
  apiKey?: string
  enabled?: boolean
  defaultProvider?: boolean
}

export interface ModelProviderTestConnectionResult {
  status: string
  providerCode: string
  latencyMs: number
  errorCode: string | null
}

export const PROVIDER_PRESETS: Record<
  string,
  { displayName: string; websiteUrl: string; baseUrl: string; chatModel: string; embeddingModel: string }
> = {
  deepseek: {
    displayName: 'DeepSeek',
    websiteUrl: 'https://platform.deepseek.com',
    baseUrl: 'https://api.deepseek.com',
    chatModel: 'deepseek-chat',
    embeddingModel: 'deepseek-embedding',
  },
  mimo: {
    displayName: 'Xiaomi MiMo',
    websiteUrl: 'https://platform.xiaomimimo.com',
    baseUrl: 'https://api.xiaomimimo.com/v1',
    chatModel: 'mimo-chat',
    embeddingModel: 'mimo-embedding',
  },
  dashscope: {
    displayName: 'DashScope',
    websiteUrl: 'https://dashscope.aliyun.com',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    chatModel: 'qwen-plus',
    embeddingModel: 'text-embedding-v3',
  },
  openai: {
    displayName: 'OpenAI',
    websiteUrl: 'https://platform.openai.com',
    baseUrl: 'https://api.openai.com/v1',
    chatModel: 'gpt-4o-mini',
    embeddingModel: 'text-embedding-3-small',
  },
}

export function listModelProviders(): Promise<ModelProviderSummary[]> {
  return adminApiRequest<ModelProviderSummary[]>('/api/admin/model-providers', { method: 'GET' })
}

export function createModelProvider(payload: ModelProviderUpsertPayload): Promise<ModelProviderSummary> {
  return adminApiRequest<ModelProviderSummary>('/api/admin/model-providers', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function updateModelProvider(id: string, payload: ModelProviderUpsertPayload): Promise<ModelProviderSummary> {
  return adminApiRequest<ModelProviderSummary>(`/api/admin/model-providers/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export function setDefaultModelProvider(id: string): Promise<ModelProviderSummary> {
  return adminApiRequest<ModelProviderSummary>(`/api/admin/model-providers/${id}/set-default`, {
    method: 'POST',
  })
}

export function testModelProviderConnection(id: string): Promise<ModelProviderTestConnectionResult> {
  return adminApiRequest<ModelProviderTestConnectionResult>(
    `/api/admin/model-providers/${id}/test-connection`,
    { method: 'POST' },
  )
}
