package com.example.tickets.service;

import com.example.tickets.ai.*;
import com.example.tickets.domain.*;
import com.example.tickets.repo.*;
import org.slf4j.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
public class AiJobService {
    private static final Logger log = LoggerFactory.getLogger(AiJobService.class);
    private final AiJobRepository jobs;
    private final TicketRepository tickets;
    private final MessageRepository messages;
    private final TriageSuggestionRepository triageRepo;
    private final ReplyDraftRepository drafts;
    private final DraftCitationRepository citations;
    private final ArticleVersionRepository versions;
    private final KnowledgeArticleRepository articles;
    private final ArticleChunkRepository chunks;
    private final AiRouter router;
    private final RetrievalService retrieval;
    private final org.springframework.beans.factory.ObjectProvider<KnowledgeService> knowledge;

    public AiJobService(AiJobRepository jobs, TicketRepository tickets, MessageRepository messages,
                        TriageSuggestionRepository triageRepo, ReplyDraftRepository drafts,
                        DraftCitationRepository citations, ArticleVersionRepository versions,
                        KnowledgeArticleRepository articles, ArticleChunkRepository chunks,
                        AiRouter router, RetrievalService retrieval,
                        org.springframework.beans.factory.ObjectProvider<KnowledgeService> knowledge) {
        this.jobs = jobs; this.tickets = tickets; this.messages = messages; this.triageRepo = triageRepo;
        this.drafts = drafts; this.citations = citations; this.versions = versions; this.articles = articles;
        this.chunks = chunks; this.router = router; this.retrieval = retrieval; this.knowledge = knowledge;
    }

    public AiJob enqueueTriage(Long ticketId, int convVersion) {
        AiJob j = new AiJob(); j.setType(JobType.TRIAGE); j.setStatus(JobStatus.QUEUED);
        j.setTicketId(ticketId); j.setConversationVersion(convVersion);
        jobs.save(j); processAsync(j.getId());
        return j;
    }

    public AiJob enqueueDraft(Long ticketId, int convVersion, String idempotencyKey) {
        AiJob j = new AiJob(); j.setType(JobType.DRAFT_REPLY); j.setStatus(JobStatus.QUEUED);
        j.setTicketId(ticketId); j.setConversationVersion(convVersion);
        jobs.save(j); processDraftAsync(j.getId(), idempotencyKey);
        return j;
    }

    public AiJob enqueueIndex(Long versionId) {
        AiJob j = new AiJob(); j.setType(JobType.INDEX_ARTICLE); j.setStatus(JobStatus.QUEUED);
        j.setArticleVersionId(versionId);
        jobs.save(j); processIndexAsync(j.getId());
        return j;
    }

    public AiJob retry(Long jobId) {
        AiJob j = jobs.findById(jobId).orElseThrow();
        j.setStatus(JobStatus.QUEUED); j.setLastError(null);
        jobs.save(j);
        if (j.getType() == JobType.TRIAGE) processAsync(j.getId());
        else if (j.getType() == JobType.DRAFT_REPLY) processDraftAsync(j.getId(), null);
        else processIndexAsync(j.getId());
        return j;
    }

    @Async("aiExecutor")
    public void processAsync(Long jobId) { runWithRetry(jobId, this::runTriage); }

    @Async("aiExecutor")
    public void processDraftAsync(Long jobId, String idempotencyKey) { runWithRetry(jobId, j -> runDraft(j, idempotencyKey)); }

    @Async("aiExecutor")
    public void processIndexAsync(Long jobId) { runWithRetry(jobId, this::runIndex); }

    private void runWithRetry(Long jobId, java.util.function.Consumer<AiJob> fn) {
        AiJob j = jobs.findById(jobId).orElse(null);
        if (j == null) return;
        int maxAttempts = 3; // initial + 2 retries (FR-11), delays 1s and 3s for transient
        while (j.getAttempts() < maxAttempts) {
            try {
                j.setStatus(JobStatus.RUNNING); j.setAttempts(j.getAttempts() + 1); jobs.save(j);
                // timeout guard 30s per model request is enforced inside providers; record elapsed in runDraft
                fn.accept(j);
                j.setStatus(JobStatus.SUCCEEDED); j.setLastError(null); jobs.save(j);
                return;
            } catch (Exception e) {
                boolean transientErr = isTransient(e);
                j.setLastError(truncate(e.getMessage()));
                if (!transientErr || j.getAttempts() >= maxAttempts) {
                    j.setStatus(JobStatus.FAILED); jobs.save(j);
                    log.warn("AI job {} failed permanently: {}", jobId, e.toString());
                    return;
                }
                j.setStatus(JobStatus.RETRYING); jobs.save(j);
                try { Thread.sleep(j.getAttempts() == 1 ? 1000 : 3000); } catch (InterruptedException ignored) {}
                j = jobs.findById(jobId).orElse(null);
                if (j == null) return;
            }
        }
    }

    private boolean isTransient(Exception e) {
        String m = String.valueOf(e.getMessage()).toLowerCase();
        return m.contains("timeout") || m.contains("rate") || m.contains("429") || m.contains("503") || m.contains("temporar");
    }

    private String truncate(String s) { return s == null ? null : s.substring(0, Math.min(s.length(), 1500)); }

    @Transactional
    protected void runTriage(AiJob j) {
        Ticket t = tickets.findById(j.getTicketId()).orElseThrow(() -> new RuntimeException("ticket gone"));
        StringBuilder conv = new StringBuilder(t.getSubject()).append("\n").append(t.getDescription()).append("\n");
        messages.findByTicketIdOrderByCreatedAtAsc(t.getId()).forEach(m -> conv.append(m.isFromAgent() ? "AGENT: " : "CUSTOMER: ").append(m.getBody()).append("\n"));
        AiProvider p;
        try { p = router.active(); }
        catch (Exception e) { throw new RuntimeException("transient provider unavailable: timeout", e); }
        TriageResult r;
        try { r = p.triage(t.getSubject(), conv.toString()); }
        catch (Exception e) { throw new RuntimeException("transient triage timeout", e); }
        if (r.category() == null || r.urgency() == null) throw new RuntimeException("invalid AI output: enum validation failed");
        // mark older suggestions for newer conv stale
        TriageSuggestion s = new TriageSuggestion();
        s.setTicket(t); s.setConversationVersion(j.getConversationVersion() != null ? j.getConversationVersion() : t.getConversationVersion());
        s.setSuggestedCategory(r.category()); s.setSuggestedUrgency(r.urgency());
        s.setCategoryReason(truncate(r.categoryReason())); s.setUrgencyReason(truncate(r.urgencyReason()));
        s.setStale(s.getConversationVersion() != t.getConversationVersion());
        triageRepo.save(s);
    }

    @Transactional
    protected void runDraft(AiJob j, String idempotencyKey) {
        Ticket t = tickets.findById(j.getTicketId()).orElseThrow(() -> new RuntimeException("ticket gone"));
        StringBuilder conv = new StringBuilder();
        messages.findByTicketIdOrderByCreatedAtAsc(t.getId()).forEach(m -> conv.append(m.isFromAgent() ? "AGENT: " : "CUSTOMER: ").append(m.getBody()).append("\n"));
        var ctx = retrieval.retrieve(t.getSubject() + " " + conv, 5);
        AiProvider p = router.active();
        long start = System.currentTimeMillis();
        DraftResult r;
        try { r = p.draftReply(conv.toString(), ctx); }
        catch (Exception e) { throw new RuntimeException("transient draft timeout", e); }
        long elapsed = System.currentTimeMillis() - start;
        // validate citations belong to retrieved context (FR-08)
        java.util.Set<Long> allowed = new java.util.HashSet<>();
        ctx.forEach(c -> allowed.add(c.chunk().getId()));
        List<Long> used = r.usedChunkIds() == null ? List.of() : r.usedChunkIds().stream().filter(allowed::contains).toList();

        ReplyDraft d = new ReplyDraft();
        d.setTicket(t); d.setConversationVersion(t.getConversationVersion());
        d.setDraftText(r.draftText() == null ? "" : r.draftText().substring(0, Math.min(r.draftText().length(), 5000)));
        d.setNeedsClarification(r.needsClarification());
        d.setModelId(r.modelId()); d.setPromptVersion("v1"); d.setElapsedMs(elapsed);
        d.setIdempotencyKey(idempotencyKey);
        d.setSourceVersions(ctx.stream().map(c -> c.articleVersionId() + ":" + c.chunk().getId()).reduce((a, b) -> a + "," + b).orElse(""));
        d.setStatus(DraftStatus.DRAFT);
        drafts.save(d);
        for (Long cid : used) {
            var chunk = chunks.findById(cid).orElse(null);
            if (chunk == null) continue;
            DraftCitation dc = new DraftCitation();
            dc.setDraft(d); dc.setChunkId(cid); dc.setArticleVersionId(chunk.getArticleVersion().getId());
            dc.setExcerpt(chunk.getContent().substring(0, Math.min(800, chunk.getContent().length())));
            var ver = versions.findById(chunk.getArticleVersion().getId()).orElse(null);
            if (ver != null) {
                var art = articles.findById(ver.getArticle().getId()).orElse(null);
                if (art != null) dc.setArticleTitle(art.getTitle());
            }
            citations.save(dc);
        }
    }

    @Transactional
    protected void runIndex(AiJob j) {
        knowledge.getObject().indexVersion(j.getArticleVersionId());
    }

    @Transactional
    public void markDraftsStale(Long ticketId, String reason) {
        drafts.findByTicketIdOrderByCreatedAtDesc(ticketId).stream()
            .filter(d -> d.getStatus() == DraftStatus.DRAFT)
            .forEach(d -> d.setStatus(DraftStatus.STALE));
    }

    @Transactional
    public void markDraftsStaleForArticle(Long articleId) {
        // any DRAFT citing this article becomes STALE (FR-10)
        for (ReplyDraft d : drafts.findAll()) {
            if (d.getStatus() != DraftStatus.DRAFT) continue;
            boolean cites = citations.findByDraftId(d.getId()).stream().anyMatch(c -> {
                var v = c.getArticleVersionId() == null ? null : versions.findById(c.getArticleVersionId()).orElse(null);
                return v != null && v.getArticle().getId().equals(articleId);
            });
            if (cites) d.setStatus(DraftStatus.STALE);
        }
    }
}
