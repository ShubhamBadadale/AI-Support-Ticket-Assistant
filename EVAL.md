# Evaluation (PRD §4, §13)

No results claimed before measurement. Run after implementation:

1. Freeze 60 synthetic cases: 20 tuning + 40 held-out (30 answerable, 10 insufficient-source). Include ambiguous urgency + prompt-injection in tickets and articles. Document category distribution + label disagreements.
2. Deterministic API tests use `StubAiProvider` (fixed responses).
3. Real-model run: set `AI_PROVIDER=openai`, record model id, `AI_PROMPT_VERSION`, config, date. Measure: category accuracy (target ≥80%), top-5 retrieval hit (target ≥90% answerable), 10/10 insufficient → clarification/handoff, 0 invented refunds/account actions/policies, effort paired comparison (≥8 scenarios/condition, 2 reviewers, counterbalanced), role/ownership matrix.
4. Record token use + provider charges; do not claim cost estimates without data. Report hardware + dataset size with latency (p95 <750ms ordinary API @10rps/5min local; p95 AI job <60s normal demo).

Template result row: `case_id | expected_category | predicted | expected_article | top5_hit | draft_supported? | invented_claim? | notes`.
