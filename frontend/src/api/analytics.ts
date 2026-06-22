import { adminApiRequest } from './client'
import type { AnalyticsOverview, OpsAlertRecord, OpsAlertSummary } from '../types/api'

export function fetchAnalyticsOverview(): Promise<AnalyticsOverview> {
  return adminApiRequest<AnalyticsOverview>('/api/analytics/overview', {
    method: 'GET',
  })
}

export function fetchOpsAlerts(): Promise<OpsAlertSummary> {
  return adminApiRequest<OpsAlertSummary>('/api/analytics/ops/alerts', {
    method: 'GET',
  })
}

export function fetchPersistedOpsAlerts(): Promise<OpsAlertRecord[]> {
  return adminApiRequest<OpsAlertRecord[]>('/api/analytics/ops/alerts/persisted', {
    method: 'GET',
  })
}

export function acknowledgeOpsAlert(alertId: string): Promise<OpsAlertRecord> {
  return adminApiRequest<OpsAlertRecord>(`/api/analytics/ops/alerts/${alertId}/acknowledge`, {
    method: 'POST',
  })
}
