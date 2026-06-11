import { apiRequest } from './client'
import type {
  CreateKnowledgeBasePayload,
  DocumentStatusResponse,
  DocumentUploadResponse,
  KnowledgeBaseResponse,
} from '../types/api'

export function listKnowledgeBases(): Promise<KnowledgeBaseResponse[]> {
  return apiRequest<KnowledgeBaseResponse[]>('/api/knowledge-bases', {
    method: 'GET',
  })
}

export function createKnowledgeBase(
  payload: CreateKnowledgeBasePayload,
): Promise<KnowledgeBaseResponse> {
  return apiRequest<KnowledgeBaseResponse>('/api/knowledge-bases', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function listKnowledgeBaseDocuments(kbId: string): Promise<DocumentStatusResponse[]> {
  return apiRequest<DocumentStatusResponse[]>(`/api/knowledge-bases/${kbId}/documents`, {
    method: 'GET',
  })
}

export function uploadKnowledgeBaseDocument(
  kbId: string,
  file: File,
  options: { courseId?: string; chapterId?: string } = {},
): Promise<DocumentUploadResponse> {
  const form = new FormData()
  form.append('file', file)
  if (options.courseId) {
    form.append('courseId', options.courseId)
  }
  if (options.chapterId) {
    form.append('chapterId', options.chapterId)
  }
  return apiRequest<DocumentUploadResponse>(`/api/knowledge-bases/${kbId}/documents`, {
    method: 'POST',
    body: form,
  })
}

export function fetchDocumentStatus(documentId: string): Promise<DocumentStatusResponse> {
  return apiRequest<DocumentStatusResponse>(`/api/documents/${documentId}`, {
    method: 'GET',
  })
}
