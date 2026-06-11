# Learning Agent Initialization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the phase-0 foundation for the personalized learning multi-agent system and define the north-star target: a full AI Learning OS with dialogue-based learner profiling, multi-agent resource generation, Course RAG, adaptive assessment, traceable execution, and reviewable learning analytics.

**Architecture:** Keep backend work as contracts and documentation for now. Treat the current Vue shell as the visible entry point for a larger AI Learning OS roadmap, and treat the docs as the source of truth for the future Spring Boot, Agent, and RAG implementation boundaries.

**Tech Stack:** Vue 3, Vite, TypeScript, Vitest, Vue Test Utils, lucide-vue-next, Markdown docs.

---

## Target

- **Phase 0 target:** runnable Vue placeholder frontend, explicit backend contracts, and architecture docs for handoff.
- **North-star target:** a complete learning platform where learning goals drive profile extraction, knowledge diagnosis, Course RAG, path planning, resource generation, assessment feedback, and path replanning.
- **Governance target:** every AI-producing step must be traceable, reviewable, and grounded in cited source data.

## File Structure

- `frontend/src/App.vue`: Placeholder workbench UI for student modules and backend boundaries.
- `frontend/src/style.css`: Responsive operational dashboard styling.
- `frontend/src/App.spec.ts`: Component smoke test for core system labels.
- `frontend/package.json`: Adds test script and frontend dependencies.
- `docs/research/reference-priority.md`: Captures the two prioritized user documents.
- `docs/architecture/overview.md`: Backend module and observability boundaries.
- `docs/api/contract.md`: REST and SSE API boundary draft.
- `docs/data/model.md`: Table-level data model draft.
- `README.md`: Project entry point.

## Tasks

- [x] Create Vue 3 + Vite + TypeScript frontend scaffold.
- [x] Add Vitest, Vue Test Utils, jsdom, and lucide-vue-next.
- [x] Write a component test for the platform placeholder labels.
- [x] Replace the default Vite page with the learning multi-agent workbench.
- [x] Add priority reference, architecture, API contract, and data model docs.
- [x] Run frontend tests.
- [x] Run frontend production build.
