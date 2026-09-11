# Data Agent

A learning-focused **Text-to-SQL Agent** built from scratch with Spring Boot and PostgreSQL/pgvector.

> This repository is an original implementation inspired by common Data Agent architecture patterns. It does not copy source code from third-party tutorial repositories.

## Goal

Build the complete agent incrementally:

`Question → Plan → Human approval → Schema context → SQL generation → Validation → Execution → Correction → Explanation`

Later milestones add RAG, embeddings, Python sandbox analysis and streaming responses.

## Current milestone — M1

- Spring Boot 3 / Java 21 backend
- PostgreSQL + pgvector local environment
- sample analytics database
- `/api/agent/health`
- `/api/agent/plan` endpoint
- explicit `AWAITING_APPROVAL` state as the first Human-in-the-Loop boundary
- environment-based database configuration

## Run locally

Requirements: Java 21, Maven, Docker.

```bash
docker compose up -d
mvn spring-boot:run
```

Health check:

```bash
curl http://localhost:9933/api/agent/health
```

Create a plan:

```bash
curl -X POST http://localhost:9933/api/agent/plan \
  -H 'Content-Type: application/json' \
  -d '{"question":"Which region generated the most revenue?"}'
```

## Roadmap

1. **M1 — Agent scaffold + HITL plan**
2. **M2 — Schema introspection + read-only SQL executor**
3. **M3 — LLM Text-to-SQL + validation/self-correction**
4. **M4 — RAG for schema/business context**
5. **M5 — Python sandbox for deeper analysis**
6. **M6 — SSE streaming UI + evaluation suite**

## Architecture principle

The model never receives unrestricted database write access. SQL execution will be isolated behind a read-only validation/execution layer, and high-impact steps are designed to expose explicit human approval points.

## Attribution

The learning direction was informed by the public `qifan777/data-agent-tutorial` project and the broader Spring AI Alibaba DataAgent ecosystem. This repository intentionally implements its own code rather than copying the tutorial source.
