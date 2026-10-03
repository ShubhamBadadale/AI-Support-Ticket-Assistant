# Product Requirements Document
# AI Support Ticket Assistant

**Version:** 1.0 — proposed MVP  
**Date:** 4 October 2026  
**Purpose:** Spring Boot + AI project for an internship and placement portfolio  
**Status:** Ready for scope review; estimates and targets are proposals, not measured results.

## 1. Product summary

Build a web application that helps a small software company's support team manage customer tickets. AI suggests a ticket category and urgency, finds relevant knowledge articles, and drafts a reply with source references. A support agent reviews, edits, and explicitly sends every customer-facing response.

**Example:** A customer reports being locked out of their account. The application saves the ticket immediately, suggests `ACCOUNT_ACCESS`, retrieves the company's account-recovery article, and prepares a reply. The agent checks the instructions and sends the final response through the application.

**Product promise:** Help an agent understand a ticket and prepare a supported response faster while preserving human control over ticket decisions.

## 2. Brief and assumptions

### Confirmed from the request

- The project is an AI support ticket assistant.
- Spring Boot is the backend framework.
- The project should be useful for a placement portfolio.

### Proposed assumptions

- One developer builds the first version, with an estimated eight weeks at 10–15 hours per week. This is a planning estimate and depends on existing skills.
- One fictional software company uses the application; there is no organization switching.
- English is the only supported language in the MVP.
- Customers and agents communicate inside the application.
- Seeded synthetic tickets and knowledge articles provide the demo dataset.
- A small web interface demonstrates the backend workflows.
- An external language model and embedding model are available through configurable credentials. Model selection and a monthly spending limit are deployment decisions.

## 3. Problem and intended users

Support agents repeatedly read similar requests, decide urgency, search for approved instructions, and write replies. New agents may struggle to find the right policy. A generic AI answer can sound convincing while inventing a company policy.

The application brings ticket management, relevant company information, and a reviewed draft into one workspace.

| User | Main need | Permissions |
|---|---|---|
| Customer | Submit a problem and follow the conversation | Create tickets; view and reply to own tickets; reopen own resolved tickets |
| Support agent | Work through a queue and respond accurately | View the support queue; claim unassigned tickets; handle assigned tickets; review AI suggestions |
| Administrator | Maintain the support environment | All agent capabilities; manage user roles, assignments, and knowledge articles; view reporting |

An agent can inspect another agent's ticket but cannot change it or send a reply. An administrator may reassign it. Customers cannot access internal notes, draft replies, AI reasoning, or other customers' tickets.

## 4. Goals and success measures

### Product goals

1. Provide a complete ticket lifecycle that works when AI is unavailable.
2. Make AI recommendations understandable and editable.
3. Ground reply drafts in the company's published knowledge articles.
4. Demonstrate backend engineering through authorization, persistence, asynchronous processing, error handling, and a documented API.

### Proposed release targets

| Measure | Target | How to measure |
|---|---|---|
| Category prediction | At least 80% accuracy | Compare predictions against reviewed labels on a held-out synthetic dataset |
| Relevant retrieval | Supporting article appears in the top five results for at least 90% of answerable cases | Review retrieval results against expected source articles |
| Unsupported requests | All ten designated insufficient-source cases request clarification or hand off to an agent | Manual review against a documented rubric |
| Critical unsupported claims | Zero invented refunds, account actions, or company policies in the release evaluation | Inspect every generated draft in the held-out dataset |
| Agent effort | Aim for a 20% reduction in median time to prepare an acceptable reply | Exploratory paired comparison with and without AI, using equivalent scenarios |
| Access control | All defined role and ownership scenarios pass | API integration checks |

Use 60 synthetic evaluation cases: 20 for tuning and 40 held out. The held-out set contains 30 answerable requests and ten requests requiring clarification or handoff; it also includes ambiguous urgency and malicious instructions. Freeze the held-out set before tuning. Document category distribution and disagreements in labels.

For the effort comparison, use at least eight scenarios per condition with two reviewers and counterbalance the order. Report sample size and limitations. No accuracy or time-saving result may be claimed before it is measured.

## 5. MVP scope

### Included

- Authentication and three roles.
- Customer ticket creation, conversation history, and reopening.
- Agent queue, filtering, assignment, internal notes, and status updates.
- AI category and urgency suggestions with brief reasons.
- Knowledge article creation, publishing, editing, and deactivation.
- Retrieval of relevant article passages and generation of a cited reply draft.
- Explicit agent review, editing, and sending.
- AI job progress, controlled retries, and manual fallback.
- Basic reporting and an audit history.
- Synthetic demo data, API documentation, and a reproducible local setup.

### Future scope

Email and WhatsApp integration, ticket attachments, PDF ingestion, multilingual replies, service-level automation, multiple organizations, automatic routing, duplicate detection, and model training are later enhancements.

The MVP's AI has no tools for issuing refunds, changing accounts, or executing customer requests. Sending a reply requires an authenticated agent action.

## 6. User journeys

### Journey A — submit and track a ticket

1. A customer signs in and enters a subject and description.
2. The server validates and saves the ticket, then returns a ticket number.
3. Triage runs in the background. The customer sees the ticket immediately.
4. The customer reads approved agent replies and adds follow-up messages.
5. A resolved ticket can be reopened with a new explanation.

### Journey B — investigate and reply

1. An agent filters the queue and claims an unassigned ticket.
2. The agent sees current ticket values alongside AI category and urgency suggestions.
3. The agent applies or overrides those suggestions.
4. The agent requests a reply draft for the latest conversation.
5. The system retrieves relevant articles and displays a draft, citations, and job status.
6. The agent inspects the sources, edits the draft, and selects **Send reply**.
7. The final reply appears in the customer conversation. The agent chooses the appropriate ticket status.

### Journey C — maintain knowledge

1. An administrator creates an article with a title and plain-text content.
2. Publishing starts background indexing; the article is unavailable to AI until indexing succeeds.
3. An edited article receives a new version. Its previous indexed version remains usable until the replacement is ready.
4. Deactivating an article excludes all its versions from new retrieval immediately.

## 7. Functional requirements and acceptance criteria

| ID | Requirement | Acceptance criteria |
|---|---|---|
| FR-01 | Authenticate users and enforce roles | All non-login endpoints require authentication. Customers can access only their own tickets. Role and ownership checks occur on the server. MVP accounts are seeded; administrators create additional users. |
| FR-02 | Create tickets | Subject is 5–150 characters; description is 20–5,000 characters. A saved ticket receives a unique number, `OPEN` status, `OTHER` category, and `NORMAL` urgency. AI failure cannot roll back ticket creation. |
| FR-03 | Browse the queue | Agents can filter by status, confirmed urgency, confirmed category, and assignee, and search by ticket number or subject. Results are paginated, with 20 per page by default and a maximum of 100. |
| FR-04 | Assign tickets | An agent can claim an unassigned ticket; an administrator can assign, reassign, or unassign it. Simultaneous claims produce one successful assignment and a conflict for the other request. |
| FR-05 | Manage conversations | Public messages and internal notes are distinct. Messages are 1–5,000 characters. Customers never receive internal notes or draft content through any API. |
| FR-06 | Suggest triage | Ticket creation and each customer public follow-up enqueue triage. The AI returns allowed category and urgency values plus short reasons. Suggestions do not overwrite confirmed values. An assigned agent or administrator can apply or override current suggestions; the audit log records the action. Results for an older conversation version are marked stale and cannot be applied. |
| FR-07 | Maintain knowledge articles | Administrators can create, edit, publish, and deactivate articles. Titles are 5–150 characters; content is 50–20,000 characters. Indexing displays `PENDING`, `READY`, or `FAILED`. New retrieval uses only active, ready versions. |
| FR-08 | Draft a supported reply | An assigned agent or administrator can request a draft. The screen shows supporting article titles and excerpts. Citations must refer only to retrieved passages. Insufficient evidence produces a clarification or handoff draft, without invented company instructions. |
| FR-09 | Review and send | The agent can edit, reject, or regenerate a draft. Sending requires an explicit action. A repeated request using the same idempotency key and payload creates only one reply; changing the payload with that key returns a conflict. |
| FR-10 | Avoid stale replies | Each draft records the conversation version and source versions used. A new public message, deactivation of a cited source, or replacement of its published version makes the draft stale. A pending article edit does not invalidate its still-published version. Sending a stale draft through the draft workflow is blocked until regeneration. The agent can still compose an independent manual reply. |
| FR-11 | Recover from AI failure | Show a readable error and retain ticket access. Retry transient timeouts or rate limits up to two additional attempts, with delays of one and three seconds. Invalid requests or output fail without automatic retry. Agents can initiate a new attempt. |
| FR-12 | Track outcomes | Record accepted or overridden triage, used or rejected drafts, and generation failures. Reporting groups tickets by status/category and summarizes these outcomes without counting unfinished drafts as accepted. |
| FR-13 | Record audit events | Store actor, action, timestamp, entity ID, and relevant before/after values for assignments, status changes, confirmed triage, article publishing, and reply sending. Audit events cannot be edited through the application. |

## 8. Business rules

### Categories and urgency

Allowed categories: `ACCOUNT_ACCESS`, `BILLING`, `TECHNICAL`, `HOW_TO`, and `OTHER`.

| Urgency | Proposed meaning |
|---|---|
| LOW | General guidance or non-blocking request |
| NORMAL | Individual issue with limited impact |
| HIGH | A customer cannot complete a core task and has no known workaround |
| URGENT | Reported security incident or widespread outage requiring immediate human review |

These are suggestions based on the ticket text. The AI cannot verify that an outage or incident actually occurred. Final urgency is the agent's decision; model-generated confidence is not displayed as a probability of correctness.

### Status transitions

| From | Allowed destination | Who may perform it |
|---|---|---|
| OPEN | IN_PROGRESS | Assigned agent or administrator |
| IN_PROGRESS | WAITING_FOR_CUSTOMER or RESOLVED | Assigned agent or administrator |
| WAITING_FOR_CUSTOMER | IN_PROGRESS or RESOLVED | Assigned agent or administrator |
| WAITING_FOR_CUSTOMER | OPEN | System, when the customer adds a public message |
| RESOLVED | OPEN | Customer through explicit reopening with a new message; assigned agent or administrator through reopening |

Other transitions return a validation error. Sending a reply does not automatically resolve a ticket. On a resolved ticket, the customer must use the reopen action to add another message. Reopening retains its current assignee. Internal notes do not change the conversation version used for AI drafting.

## 9. AI behavior

### Triage

Use the subject and public conversation as input. Return a structured category, urgency, and concise reason for each. Validate enum values and field lengths before storing suggestions. Malformed output is an AI job failure, not a ticket failure.

### Retrieval and reply drafting

Retrieval-augmented generation (RAG) means searching company articles first, then giving the relevant passages to the model as context.

1. Retrieve up to five passages from active, ready knowledge articles.
2. Apply a relevance threshold selected using the tuning set.
3. Generate a reply from the eligible passages and latest public conversation.
4. Return reply text, retrieved source identifiers actually used, and an indication that clarification is needed when appropriate.
5. Validate that every citation identifier belongs to the retrieved context.
6. Show the agent the source titles, versions, and excerpts for review.

Valid citation identifiers alone do not establish that a claim is supported. Evaluate factual support separately using the review rubric. A missing match or conflicting instructions should produce a request for more details or human review.

Treat ticket and article content as data. Instructions inside them must not alter permissions, reveal secrets, or trigger operations. Send only public ticket text and necessary article excerpts to the model; exclude internal notes, credentials, and unnecessary account details.

Record model identifier, prompt version, conversation/source versions, elapsed time, and token usage when provided. Do not record secrets or full sensitive prompts in general application logs.

## 10. Screens

| Screen | Required content |
|---|---|
| Sign in | Credentials and clear validation errors |
| Customer home | Own tickets with status and last activity |
| Create ticket | Subject and description with field limits |
| Customer ticket | Public conversation, reply box, and reopen action |
| Agent queue | Search, filters, pagination, assignment, and status |
| Agent workspace | Conversation, internal notes, confirmed triage, AI suggestions, source excerpts, editable draft, and explicit send action |
| Knowledge management | Article editor, active version, indexing progress, and deactivate action |
| Admin reporting | Ticket totals, status/category breakdown, AI outcomes, and failures |

AI content is labeled as a suggestion or draft. Loading, failure, empty results, and stale drafts each have a visible state. Forms support keyboard use, readable contrast, and meaningful labels.

## 11. Quality requirements

- **Reliability:** Ticket creation and manual replies remain functional if the AI provider is unavailable. Persist jobs so queued work can resume after a restart; duplicate job delivery must not create duplicate results.
- **Responsiveness:** Target a p95 below 750 ms for ordinary API requests at 10 requests/second in a five-minute local test. Exclude AI work; report hardware and dataset size with measurements.
- **AI latency:** Set a 30-second timeout per model request. Aim for p95 job completion within 60 seconds under normal demo load; retries may exceed this. Display progress while jobs run.
- **Security:** Hash passwords; enforce server-side authorization; keep provider keys on the backend. Enforce field limits and per-user AI request limits.
- **Cost:** Start with five generation requests per user per minute, at most two concurrent model jobs per application instance, and bounded prompts/outputs. Document actual token use and any provider charges before claiming a cost estimate.
- **Observability:** Capture request IDs, job status, latency, and sanitized errors. Health checks distinguish core application health from optional AI availability.
- **Consistency:** Use transactions for ticket updates and related audit events. Handle concurrent edits through a version check and an explicit conflict response.

## 12. Proposed technical direction

Use a Spring Boot application with modules for identity, tickets, knowledge, AI jobs, and reporting. A modular monolith keeps the first version manageable while making responsibilities clear.

| Component | Proposed choice | Purpose |
|---|---|---|
| Backend | Java + Spring Boot, Spring Web, Validation | REST API and business rules |
| Authorization | Spring Security with server sessions | Same-origin login and role enforcement; enable CSRF protection |
| Persistence | Spring Data JPA + PostgreSQL | Users, tickets, messages, articles, jobs, and audit events |
| AI integration | Spring AI | Structured model responses, embeddings, and RAG integration |
| Semantic retrieval | PostgreSQL with pgvector | Store and search article embeddings alongside relational data |
| UI | React, served under the same origin | Customer, agent, and administrator workflows |
| Local setup | Docker Compose | Reproducible app and database environment |

Spring AI's official documentation describes structured outputs, RAG, and PostgreSQL/PGVector support. Exact compatible dependency versions should be pinned during implementation; this PRD does not prescribe a version.

### Main data objects

`User`, `Ticket`, `Message`, `InternalNote`, `KnowledgeArticle`, `ArticleVersion`, `ArticleChunk`, `AiJob`, `TriageSuggestion`, `ReplyDraft`, `DraftCitation`, and `AuditEvent`.

Tickets carry an assignee and conversation version. Suggestions and drafts store the input versions used. Article publication must preserve version identity, and retrieval must exclude inactive sources. Job processing uses durable state rather than an in-memory queue alone.

Detailed database schemas, endpoint payloads, and an implementation task plan belong in the next technical design stage.

## 13. Evaluation and release verification

### Required checks

- Customer ownership and role boundaries, including internal-note exclusion.
- Valid/invalid status transitions and concurrent assignment.
- Ticket and manual reply creation while AI is unavailable.
- Knowledge publication, editing, deactivation, and indexing failures.
- Triage validation, empty retrieval, false citation identifiers, and unsupported policy claims.
- Prompt-injection cases in both tickets and knowledge articles.
- Stale drafts after customer follow-up or source changes.
- Duplicate reply requests and job recovery after a restart.

Use a fixed local model-response stub for deterministic API tests. Run the separate evaluation dataset against the chosen real model and record its identifier, configuration, prompt version, and date.

### Demo dataset and walkthrough

Prepare 15 fictional knowledge articles, 30 visible demo tickets, two customer accounts, two agent accounts, and one administrator account. Keep the 40 evaluation cases separate from visible demo examples.

The demo should show: a customer submitting an account-access ticket; an agent accepting triage and editing a cited draft; an unsupported billing question requesting clarification; an article being deactivated; and a provider failure followed by a manual reply.

## 14. Delivery milestones

| Milestone | Deliverable | Completion condition |
|---|---|---|
| 1 — Core workflow | Login, roles, tickets, assignment, messages, and statuses | Customer and agent complete a ticket lifecycle without AI |
| 2 — Triage | Background jobs and validated suggestions | Predictions are displayed, editable, and recoverable after failure |
| 3 — Knowledge and drafting | Versioned articles, retrieval, citations, and editable drafts | Supported, unsupported, and stale-source cases work end to end |
| 4 — Portfolio release | Reporting, evaluation, demo, documentation, and reproducible setup | Release checks pass and measured results are recorded |

Milestones describe deliverables; scheduling should be revised after the core workflow is working.

## 15. Risks and responses

| Risk | Response |
|---|---|
| AI invents a policy | Constrain context, validate citations, evaluate support, and require agent review |
| Poor source material produces poor drafts | Start with a reviewed knowledge set; show source versions and retrieval failures |
| Model access or cost blocks development | Isolate provider integration; use a response stub for workflow development |
| Features exceed available time | Deliver the ticket lifecycle, triage, and grounded drafts before future enhancements |
| Synthetic results overstate real-world usefulness | Label the dataset and measurements clearly; do not claim production accuracy |

## 16. MVP definition of done

- The three roles can complete their documented journeys.
- Authorization is enforced by the backend, and internal content is excluded from customer responses.
- All customer-facing replies require explicit agent submission.
- AI failures preserve the manual support workflow.
- Knowledge versions, citations, and stale drafts behave as specified.
- Required release checks and held-out evaluation are complete; failures are corrected or documented as release blockers.
- The repository includes setup instructions, environment-variable examples without secrets, API documentation, sample data, an architecture overview, and evaluation results.
- A new reviewer can run the demo from documented steps and understand its limitations.

## References

These support the proposed technical integration; product targets and scope are original proposals.

- [Spring AI project overview](https://spring.io/projects/spring-ai)
- [Spring AI: Retrieval Augmented Generation](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
- [Spring AI project README: supported capabilities](https://github.com/spring-projects/spring-ai/blob/main/README.md)
