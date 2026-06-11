import { apiRequest } from './client'
import type { ResourceReviewSummary, ReviewDecisionPayload } from '../types/api'

export function listResourceReviews(status = 'PENDING_CRITIC'): Promise<ResourceReviewSummary[]> {
  const params = new URLSearchParams({ status })
  return apiRequest<ResourceReviewSummary[]>(`/api/reviews/resources?${params.toString()}`, {
    method: 'GET',
  })
}

export function decideResourceReview(
  reviewId: string,
  payload: ReviewDecisionPayload,
): Promise<ResourceReviewSummary> {
  return apiRequest<ResourceReviewSummary>(`/api/reviews/resources/${reviewId}/decision`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
