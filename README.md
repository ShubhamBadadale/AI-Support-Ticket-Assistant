# AI Support Ticket Assistant

Spring Boot MVP implementing `AI_Support_Ticket_Assistant_PRD.md` (v1.0, 4 Oct 2026).

> Status: MVP milestones 1–4 implemented. Estimates/targets in the PRD are proposals, not measured results. No accuracy or time-saving claim is made before measurement.

## Quick start (no Docker, H2 in-memory)

Requires Java 21.

```powershell
# with installed maven, or use the downloaded maven in temp:
& "C:\Users\Dell\AppData\Local\Temp\opencode\maven\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
# app at http://localhost:8080
# API docs: http://localhost:8080/swagger-ui.html
# health: http://localhost:8080/actuator/health
```

Seed accounts (password `password`): `customer1`, `customer2`, `agent1`, `agent2`, `admin`.

## Reproducible setup (Postgres + pgvector)

```powershell
docker compose up --build
# app http://localhost:8080, db tickets/tickets on 5432
```

Configure via env (see `.env.example`): `DATABASE_URL`, `AI_PROVIDER=stub|openai`, `OPENAI_API_KEY`, `OPENAI_MODEL`.

## Frontend (React, same origin)

```powershell
cd frontend; npm install; npm run dev   # dev on :5173 proxies /api to :8080
npm run build  # outputs to src/main/resources/static -> served by Spring at /
```

Screens: Sign in, Customer home, Create ticket, Customer ticket, Agent queue, Agent workspace (conversation, internal notes, confirmed triage, AI suggestions, source excerpts, editable draft, explicit send), Knowledge management, Admin reporting. AI content is labeled suggestion/draft; loading/failure/empty/stale states are visible.

## Demo walkthrough (PRD §13)

1. Login as `customer1` → Create ticket “Locked out of my account…” → ticket saved immediately with `OPEN/OTHER/NORMAL`, triage runs in background.
2. Login as `agent1` → queue → Claim → see triage suggestion → Apply → Request draft → inspect citations/excerpts → edit → **Send reply** (explicit).
3. Billing question with no article → draft asks for clarification (insufficient evidence, no invented policy).
4. Admin → Knowledge → Deactivate article → drafts citing it become STALE; send blocked until regenerate.
5. Stop AI (no key / provider stub forced to fail) → ticket creation + manual reply still work (`POST /api/tickets/{id}/manual-reply`).

## API (see swagger + ARCHITECTURE.md)

- `POST /api/auth/login|/api/auth/me` — session auth, 3 roles, server-side ownership checks.
- `POST /api/tickets` (5–150 subject, 20–5000 desc) → `OPEN/OTHER/NORMAL`; AI failure never rolls back.
- `GET /api/tickets?status&category&urgency&assigneeId&q&page&size` (20 default, max 100).
- `POST /api/tickets/{id}/claim` (one wins, other 409), admin assign/reassign/unassign.
- `POST /api/tickets/{id}/messages` `{body, internal}` — customers never get internal notes/drafts.
- `POST /api/tickets/{id}/status` `{status, expectedVersion}` — enforced transitions + optimistic conflict.
- `POST /api/tickets/{id}/reopen`, triage apply/override, `POST /api/tickets/{id}/drafts`, `PUT/POST /api/drafts/{id}[/send|reject]`, idempotent send.
- `GET/POST /api/articles`, `PUT /api/articles/{id}`, deactivate, `GET /api/articles/search?q=`.
- `GET /api/admin/reports/summary`, `/audit`, `/jobs`.

## AI design

- `AiProvider` abstraction: `StubAiProvider` (deterministic, offline, used in tests) + `OpenAiProvider` (REST, active only when `AI_PROVIDER=openai` + key). Spring AI is the PRD's proposed direction; this REST provider is wire-compatible and keeps the build offline-friendly. Swap in `spring-ai-starter-model-openai` without changing job logic.
- RAG: retrieve top-5 passages from active READY versions only, threshold, generate from passages + public conversation, validate citation ids, show titles/versions/excerpts. Insufficient evidence → clarification/handoff, no invented refunds/account actions/policies.
- Jobs durable in `ai_jobs` (QUEUED/RUNNING/SUCCEEDED/FAILED/RETRYING), survive restart (re-queued on boot if needed), max 2 retries with 1s/3s delays for transient timeouts/rate limits only, 30s per-request timeout, ≤2 concurrent (`aiExecutor`), 5 req/user/min guard documented.
- Security: ticket/article content treated as data; only public text + excerpts sent to model; no internal notes/credentials; prompt-injection cases in eval set.

## Evaluation

See `EVAL.md`. Held-out 40 (30 answerable + 10 insufficient-source) + tuning 20, frozen before tuning. Stub used for deterministic API tests; real-model runs record model id, config, prompt version, date.

## Project layout

`com.example.tickets`: `domain|repo|security|config|service|ai|web`. See `ARCHITECTURE.md`.
