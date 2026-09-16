# Data Agent Demo

## 90-second demo flow

1. Open `http://localhost:9933/`.
2. Start with: **Which region generated the most revenue?**
3. Point out the streamed stages: schema discovery → context retrieval → SQL generation → guarded execution → explanation.
4. Ask a harder follow-up: **Compare revenue concentration across regions and summarize the pattern.**
5. Explain that deeper analysis can hand structured query results to an isolated Python container.
6. Mention the safety boundaries: generated SQL is validated/read-only and generated Python is sandboxed.

## What this demonstrates

- Natural-language analytics over a relational database.
- Agent orchestration rather than a single prompt-response call.
- Retrieval of business/schema context before SQL generation.
- Self-correction when generated SQL fails.
- Human-readable progress through SSE.
- Deterministic evaluation in CI without spending LLM tokens.

## Architecture

```text
Browser / API
    |
    v
Text-to-SQL Agent
    |---- Schema introspection
    |---- pgvector business-context retrieval
    |---- LLM SQL generation
    |---- SQL safety validator
    |---- Read-only executor
    |---- SQL repair/retry
    |---- Explanation
    |
    +---- optional Python analysis sandbox

PostgreSQL + pgvector
```

## Interview framing

The key design choice is to keep model output behind deterministic execution boundaries. The LLM can propose SQL and Python, but it does not receive unrestricted database or host access. SQL passes through validation/read-only execution, while Python runs in a constrained Docker sandbox.

## Known limitations / next production steps

- Add authentication and per-user authorization.
- Add persistent conversation/session state.
- Add observability for latency, token usage, retrieval quality and SQL repair rate.
- Expand the evaluation dataset with semantic answer checks.
- Use production secret management rather than local environment variables.
- Add rate limits and tenant-specific database access controls.
