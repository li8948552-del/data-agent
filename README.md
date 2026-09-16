# Data Agent

An end-to-end **Text-to-SQL analytics agent** built with Spring Boot, PostgreSQL and pgvector.

> This repository is an original implementation inspired by common Data Agent architecture patterns. It does not copy source code from third-party tutorial repositories.

## Pipeline

`Question → Schema/RAG context → SQL generation → Validation → Execution → Self-correction → Python analysis → Explanation`

## Features

- Spring Boot 3 / Java 21 backend
- PostgreSQL + pgvector local environment
- schema introspection
- guarded read-only SQL execution
- LLM-powered Text-to-SQL generation
- automatic SQL repair and retry
- pgvector RAG for business definitions/rules
- Docker-isolated Python analysis with no network, read-only filesystem, memory/CPU/PID limits and timeout
- SSE progress streaming
- browser demo UI
- deterministic SQL evaluation utilities and reusable evaluation cases
- GitHub Actions CI
- production-style Docker image

## Quick start

Requirements: Java 21, Maven and Docker.

```bash
docker compose up -d
export LLM_API_KEY='your-key'
mvn spring-boot:run
```

Optional configuration is documented in `.env.example`.

Open:

```text
http://localhost:9933/
```

Try:

```text
Which region generated the most revenue?
```

The browser streams the agent's intermediate stages so schema retrieval, SQL generation, execution, repair and explanation are visible rather than hidden behind a loading spinner.

## API examples

Ask a Text-to-SQL question:

```bash
curl -X POST http://localhost:9933/api/agent/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"Which region generated the most revenue?"}'
```

Stream progress with SSE:

```bash
curl -N 'http://localhost:9933/api/agent/stream?question=Which%20region%20generated%20the%20most%20revenue%3F'
```

Run the deeper SQL + Python analysis pipeline:

```bash
curl -X POST http://localhost:9933/api/agent/analyze \
  -H 'Content-Type: application/json' \
  -d '{"question":"Compare revenue concentration across regions and summarize the pattern."}'
```

## Container image

Build the application image:

```bash
docker build -t data-agent .
```

The image uses a Maven build stage and a Java 21 runtime stage, and the application process runs as a non-root user.

> The optional Python-analysis stage itself launches constrained Docker containers, so that feature requires access to a Docker daemon. The core Text-to-SQL flow does not require host Python execution.

## Evaluation

Reusable evaluation cases live under `src/test/resources/eval/cases.json`. CI checks SQL structure and safety without requiring paid LLM calls.

```bash
mvn test
```

## Milestones

1. **M1 — Agent scaffold + HITL plan** ✅
2. **M2 — Schema introspection + read-only SQL executor** ✅
3. **M3 — LLM Text-to-SQL + validation/self-correction** ✅
4. **M4 — pgvector RAG for business context** ✅
5. **M5 — isolated Python sandbox for deeper analysis** ✅
6. **M6 — SSE streaming UI + evaluation suite** ✅
7. **M7 — demo/packaging hardening** 🚧

## Safety architecture

The model never receives unrestricted database write access. SQL is isolated behind validation and a read-only execution layer. Generated Python is isolated in a constrained Docker container rather than executed in the application JVM or directly on the host.

## Demo and interview walkthrough

See `docs/DEMO.md` for a 90-second demo flow, architecture explanation, interview framing and known production limitations.

## Attribution

The learning direction was informed by the public `qifan777/data-agent-tutorial` project and the broader Spring AI Alibaba DataAgent ecosystem. This repository intentionally implements its own code rather than copying the tutorial source.
