# Learning Agent Initialization Design

## Goal

Initialize a project skeleton for a large-model personalized learning resource generation and multi-agent adaptive learning system.

## Scope

This initialization creates a Vue 3 placeholder frontend and documentation-first backend contract. It does not implement backend business logic.

## Primary References

Future development prioritizes the two user-provided documents:

- Research and open-source support for LLM + personalized learning + multi-agent orchestration + RAG + assessment feedback loop.
- Agent / RAG / MCP / Skill engineering notes covering tool design, enterprise RAG, MCP exposure, traceability, permissions, evaluation, cost, and monitoring.

## Architecture

The recommended first backend shape is a modular monolith with clear module boundaries:

- Learner Profile
- Knowledge Graph
- Learning Path
- Resource Generation
- Course RAG
- Assessment
- Agent Runtime
- Analytics

The frontend is a placeholder workbench showing student, teacher, and admin surfaces, with student modules visible first.

## Interface Boundaries

The documentation reserves APIs for:

- Dialogue-based profile extraction.
- Learning path generation.
- Resource generation tasks.
- Course RAG document indexing and query.
- Tutor SSE streaming.
- Assessment answer submission.
- Agent trace inspection.

## Error And Trust Strategy

The backend should not rely on prompts for permissions or correctness. RAG retrieval must hard-filter permissions, resources must carry citations, and Agent execution must persist trace metadata.

## Verification

The frontend placeholder must render key project terms and pass component tests and production build.
