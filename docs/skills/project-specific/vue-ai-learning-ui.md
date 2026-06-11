# Skill: Vue AI Learning UI

## When to Use

- Building AI chat interfaces
- Creating learning cockpit dashboards
- Implementing SSE streaming UI
- Designing AI resource workbench

## Core Composables

- `useSseStream()` — SSE connection management with reconnect
- `useAiChat()` — message history, send, stream append
- `useCitationPanel()` — RAG citation display

## Page Patterns

### AI Chat Page

- Message list (user / assistant roles)
- Streaming indicator during generation
- Citation panel for RAG answers
- Input area with send / stop controls
- Error state with retry

### Learning Cockpit

- Profile summary card
- Learning path progress
- Recent resources
- AI tutor quick access
- Trace/debug panel (dev mode)

## Rules

- All AI calls via backend API (never direct LLM)
- SSE via shared composable, not raw EventSource in components
- Loading / error / empty states on every AI-powered view
- Stop button cancels in-flight SSE stream
- TypeScript types for all API responses
- For medium-fidelity prototypes, keep user-facing page text Chinese while preserving English engineering identifiers such as API paths, `traceId`, `chunkId`, `documentId`, and state enums.
- Use Indigo/Blue for normal system actions, Violet/Purple for AI/RAG/Agent actions, Emerald for success, Amber for pending/warning/degraded, Red for failed/rejected/no-source/down, and Slate for loading/empty/disabled.
- Do not wire target-state prototype controls to backend calls before the API contract supports them; render them disabled with clear context instead.
- Admin/operations charts can be CSS placeholders when backend metrics are not available, but the UI must state that they are pending backend API integration.

## Component Structure

```
components/
  ai/
    ChatMessage.vue
    ChatInput.vue
    CitationPanel.vue
    StreamingIndicator.vue
  cockpit/
    ProfileCard.vue
    PathProgress.vue
```
