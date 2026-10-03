package com.example.tickets.service;

import com.example.tickets.domain.*;
import com.example.tickets.repo.*;
import com.example.tickets.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class DraftService {
    private final ReplyDraftRepository drafts;
    private final DraftCitationRepository citations;
    private final TicketRepository tickets;
    private final MessageRepository messages;
    private final ArticleVersionRepository versions;
    private final KnowledgeArticleRepository articles;
    private final CurrentUser currentUser;
    private final AuditService audit;
    private final AiJobService jobs;

    public DraftService(ReplyDraftRepository drafts, DraftCitationRepository citations, TicketRepository tickets,
                        MessageRepository messages, ArticleVersionRepository versions, KnowledgeArticleRepository articles,
                        CurrentUser currentUser, AuditService audit, AiJobService jobs) {
        this.drafts = drafts; this.citations = citations; this.tickets = tickets; this.messages = messages;
        this.versions = versions; this.articles = articles; this.currentUser = currentUser; this.audit = audit; this.jobs = jobs;
    }

    public AiJob requestDraft(Long ticketId, String idempotencyKey) {
        Ticket t = tickets.findById(ticketId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var me = currentUser.require();
        requireAssignedOrAdmin(t, me);
        return jobs.enqueueDraft(t.getId(), t.getConversationVersion(), idempotencyKey);
    }

    @Transactional
    public ReplyDraft edit(Long draftId, String newText) {
        ReplyDraft d = drafts.findById(draftId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requireAssignedOrAdmin(d.getTicket(), currentUser.require());
        checkFresh(d);
        if (newText == null || newText.length() < 1 || newText.length() > 5000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "draft 1-5000 chars");
        d.setDraftText(newText);
        return d;
    }

    @Transactional
    public void reject(Long draftId) {
        ReplyDraft d = drafts.findById(draftId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requireAssignedOrAdmin(d.getTicket(), currentUser.require());
        d.setStatus(DraftStatus.REJECTED);
        audit.log("DRAFT_REJECTED", "ReplyDraft", d.getId().toString(), null, null);
    }

    /** Explicit send (FR-09). Idempotent on (key + payload): same key+payload -> single reply; same key + changed payload -> 409. */
    @Transactional
    public Message send(Long draftId, String finalText, String idempotencyKey) {
        ReplyDraft d = drafts.findById(draftId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Ticket t = d.getTicket();
        var me = currentUser.require();
        requireAssignedOrAdmin(t, me);
        checkFresh(d);
        if (finalText == null || finalText.length() < 1 || finalText.length() > 5000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reply 1-5000 chars");
        if (idempotencyKey != null && d.getIdempotencyKey() != null && idempotencyKey.equals(d.getIdempotencyKey())
                && d.getStatus() == DraftStatus.SENT) {
            // idempotent replay: return existing message with same body if payload matches, else conflict
            List<Message> existing = messages.findByTicketIdOrderByCreatedAtAsc(t.getId());
            for (int i = existing.size() - 1; i >= 0; i--) {
                if (existing.get(i).getBody().equals(finalText)) return existing.get(i);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "idempotency key reused with different payload");
        }
        if (idempotencyKey != null) {
            var other = drafts.findByIdempotencyKeyAndTicketId(idempotencyKey, t.getId());
            if (other.isPresent() && !other.get().getId().equals(d.getId()) && other.get().getStatus() == DraftStatus.SENT
                    && !other.get().getDraftText().equals(finalText))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "idempotency key reused with different payload");
        }
        Message m = new Message();
        m.setTicket(t); m.setAuthor(me); m.setBody(finalText); m.setFromAgent(true);
        messages.save(m);
        // sending does NOT auto-resolve (business rule)
        d.setStatus(DraftStatus.SENT);
        d.setDraftText(finalText);
        d.setIdempotencyKey(idempotencyKey != null ? idempotencyKey : d.getIdempotencyKey());
        audit.log("REPLY_SENT", "Ticket", t.getId().toString(), "draft:" + d.getId(), "msg:" + m.getId());
        return m;
    }

    /** Manual reply without AI draft (works when AI unavailable — FR-11). */
    @Transactional
    public Message manualReply(Long ticketId, String body) {
        Ticket t = tickets.findById(ticketId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var me = currentUser.require();
        requireAssignedOrAdmin(t, me);
        if (body == null || body.length() < 1 || body.length() > 5000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reply 1-5000 chars");
        Message m = new Message(); m.setTicket(t); m.setAuthor(me); m.setBody(body); m.setFromAgent(true);
        messages.save(m);
        audit.log("REPLY_SENT_MANUAL", "Ticket", t.getId().toString(), null, "msg:" + m.getId());
        return m;
    }

    private void checkFresh(ReplyDraft d) {
        Ticket t = d.getTicket();
        if (d.getStatus() == DraftStatus.SENT || d.getStatus() == DraftStatus.REJECTED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "draft already " + d.getStatus());
        if (d.getStatus() == DraftStatus.STALE || d.getConversationVersion() != t.getConversationVersion())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "draft is stale; regenerate");
        // source changed? deactivation or replacement invalidates (FR-10)
        for (DraftCitation c : citations.findByDraftId(d.getId())) {
            if (c.getArticleVersionId() == null) continue;
            var v = versions.findById(c.getArticleVersionId()).orElse(null);
            if (v == null) { d.setStatus(DraftStatus.STALE); throw new ResponseStatusException(HttpStatus.CONFLICT, "draft is stale; source removed"); }
            var a = articles.findById(v.getArticle().getId()).orElse(null);
            if (a == null || !a.isActive()) { d.setStatus(DraftStatus.STALE); throw new ResponseStatusException(HttpStatus.CONFLICT, "draft is stale; source deactivated"); }
            if (a.getPublishedVersionId() == null || !a.getPublishedVersionId().equals(v.getId())) {
                d.setStatus(DraftStatus.STALE);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "draft is stale; source updated");
            }
        }
    }

    private void requireAssignedOrAdmin(Ticket t, com.example.tickets.domain.User me) {
        if (me.getRole() == Role.ADMIN) return;
        if (me.getRole() != Role.AGENT) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "agent required");
        if (t.getAssignee() == null || !t.getAssignee().getId().equals(me.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "only assigned agent or admin");
    }
}
