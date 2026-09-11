# Data Agent

A learning-focused **Text-to-SQL Agent** built from scratch with Spring Boot and PostgreSQL/pgvector.

> This repository is an original implementation inspired by common Data Agent architecture patterns. It does not copy source code from third-party tutorial repositories.

## Goal

Build the complete agent incrementally:

`Question → Schema/RAG context → SQL generation → Validation → Execution → Self-correction → Python analysis → Explanation`

## Current milestone — M6

The project now includes:

- Spring Boot 3 / Java 21 backend
- PostgreSQL + pgvector local environment
- schema introspection
- guarded read-only SQL execution
- LLM-powered Text-to-SQL generation
- automatic SQL repair and retry
- pgvector RAG for business definitions/rules
- Docker-isolated Python analysis with no network, read-only filesystem, memory/CPU/PID limits and timeout
- SSE progress streaming
- lightweight browser UI
- deterministic SQL evaluation utilities and reusable evaluation cases
- GitHub Actions CI

## Run locally

Requirements: Java 21, Maven, Docker.

```bash
docker compose up -d
export LLM_API_KEY='your-key'
mvn spring-boot:run
```

Optional model configuration:

```bash
export LLM_BASE_URL='https://api.openai.com/v1'
export LLM_MODEL='gpt-5.6'
export EMBEDDING_MODEL='text-embedding-3-small'
```

Open the browser UI at:

```text
http://localhost:9933/
```

Ask a Text-to-SQL question:

```bash
curl -X POST http://localhost:9933/api/agent/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"Which region generated the most revenue?"}'
```

Stream agent progress with SSE:

```bash
curl -N 'http://localhost:9933/api/agent/stream?question=Which%20region%20generated%20the%20most%20revenue%3F'
```

Run the deeper SQL + Python analysis pipeline:

```bash
curl -X POST http://localhost:9933/api/agent/analyze \
  -H 'Content-Type: application/json' \
  -d '{"question":"Compare revenue concentration across regions and summarize the pattern."}'
```

The Python stage is not executed directly on the application host. Generated code runs in an ephemeral Docker container configured with no network, a read-only root filesystem, dropped Linux capabilities, PID/CPU/memory limits and a hard timeout.

## Evaluation

The repository includes deterministic SQL structure checks plus reusable evaluation cases under `src/test/resources/eval/cases.json`. These are designed to catch missing tables/aggregations and unsafe SQL tokens without requiring paid LLM calls in CI.

```bash
mvn test
```

## Roadmap

1. **M1 — Agent scaffold + HITL plan** ✅
2. **M2 — Schema introspection + read-only SQL executor** ✅
3. **M3 — LLM Text-to-SQL + validation/self-correction** ✅
4. **M4 — pgvector RAG for business context** ✅
5. **M5 — isolated Python sandbox for deeper analysis** ✅
6. **M6 — SSE streaming UI + evaluation suite** 🚧

## Architecture principle

The model never receives unrestricted database write access. SQL is isolated behind a read-only validation/execution layer. Python is isolated in a constrained Docker container rather than executed in the application JVM or host shell.

## Attribution

The learning direction was informed by the public `qifan777/data-agent-tutorial` project and the broader Spring AI Alibaba DataAgent ecosystem. This repository intentionally implements its own code rather than copying the tutorial source.
