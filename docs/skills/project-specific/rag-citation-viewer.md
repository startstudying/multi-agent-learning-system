# Skill: RAG Citation Viewer

## When to Use

- Displaying RAG answer sources in chat UI
- Building citation sidebar or inline references
- Implementing click-to-source navigation

## Pattern

RAG responses include citations array:

```json
{
  "answer": "...",
  "citations": [
    {
      "documentId": "doc-123",
      "documentTitle": "Chapter 3: Loops",
      "chunkId": "chunk-456",
      "excerpt": "A for loop iterates...",
      "pageNumber": 42,
      "score": 0.87
    }
  ]
}
```

## Frontend Display

- Inline citation markers `[1]` `[2]` linked to source list
- Expandable citation panel below answer
- Click citation → highlight source document excerpt
- Show document title, page, relevance score

## Rules

- Never display RAG answer without citations
- Permission-filtered sources only (backend enforces)
- Citation UI component: reusable across chat, tutor, resource views

## Anti-Patterns

- Showing answer text only without sources
- Frontend constructing citations (backend provides them)
- Displaying excerpts user lacks permission to access
