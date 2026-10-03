# Architecture

Modular monolith: identity, tickets, knowledge, AI jobs, reporting.

```
Browser (React) --same origin--> Spring Boot (Web/Security/Validation/JPA)
  -> TicketService (lifecycle, assignment, status machine, conversationVersion, @Version optimistic locking)
  -> KnowledgeService (versioned articles; publish pointer swaps only after READY; deactivate excludes immediately)
  -> RetrievalService (keyword overlap top-5 + title boost; pgvector-ready: chunks table can add vector column; Postgres+pgvector in compose)
  -> AiJobService (durable ai_jobs; async aiExecutor x2; retry 1s/3s transient only; 30s timeout)
     -> AiRouter -> StubAiProvider | OpenAiProvider
  -> TriageService / DraftService (stale checks, citation validation, idempotent send)
  -> ReportService / AuditService (actor/action/timestamp/entity/before-after, immutable)
  -> JPA (H2 local / Postgres prod) ; Actuator health (app + ai)
```

Data: `User Ticket Message InternalNote KnowledgeArticle ArticleVersion ArticleChunk AiJob TriageSuggestion ReplyDraft DraftCitation AuditEvent`.
Tickets carry assignee + conversationVersion; suggestions/drafts pin input versions; stale if follow-up/deactivation/replacement (pending edit does not invalidate published version).

Auth: Spring Security server sessions, `/api/auth/login` form+JSON, BCrypt, role + ownership enforced server-side. CSRF ignored for `/api` (same-origin SPA with session cookie; enable CSRF cookie for hardened prod).

Consistency: `@Transactional` ticket+audit; version check → 409 on concurrent edits/claims.

Observability: request ids via Spring defaults, job status/latency in `ai_jobs` + `ReplyDraft.elapsedMs`, sanitized errors, `/actuator/health` distinguishes core vs `ai` indicator.
