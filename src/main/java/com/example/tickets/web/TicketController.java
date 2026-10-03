package com.example.tickets.web;

import com.example.tickets.domain.*;
import com.example.tickets.repo.*;
import com.example.tickets.security.CurrentUser;
import com.example.tickets.service.*;
import com.example.tickets.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService service;
    private final TicketRepository tickets;
    private final MessageRepository messages;
    private final InternalNoteRepository notes;
    private final TriageSuggestionRepository triageRepo;
    private final ReplyDraftRepository drafts;
    private final DraftCitationRepository citations;
    private final AiJobRepository jobs;
    private final TriageService triageService;
    private final DraftService draftService;
    private final CurrentUser currentUser;

    public TicketController(TicketService service, TicketRepository tickets, MessageRepository messages,
                            InternalNoteRepository notes, TriageSuggestionRepository triageRepo,
                            ReplyDraftRepository drafts, DraftCitationRepository citations, AiJobRepository jobs,
                            TriageService triageService, DraftService draftService, CurrentUser currentUser) {
        this.service = service; this.tickets = tickets; this.messages = messages; this.notes = notes;
        this.triageRepo = triageRepo; this.drafts = drafts; this.citations = citations; this.jobs = jobs;
        this.triageService = triageService; this.draftService = draftService; this.currentUser = currentUser;
    }

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody CreateTicketRequest req) {
        Ticket t = service.create(req.subject(), req.description());
        return ticketJson(t);
    }

    @GetMapping
    public Map<String, Object> queue(@RequestParam(required = false) String status,
                                     @RequestParam(required = false) String category,
                                     @RequestParam(required = false) String urgency,
                                     @RequestParam(required = false) Long assigneeId,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        TicketStatus st = status == null ? null : TicketStatus.valueOf(status);
        Category c = category == null ? null : Category.valueOf(category);
        Urgency u = urgency == null ? null : Urgency.valueOf(urgency);
        Page<Ticket> p = service.queue(st, c, u, assigneeId, q, page, size);
        return Map.of("content", p.getContent().stream().map(this::ticketJson).toList(),
            "page", p.getNumber(), "totalPages", p.getTotalPages(), "totalElements", p.getTotalElements());
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Ticket t = service.get(id);
        var me = currentUser.require();
        boolean staff = me.getRole() != Role.CUSTOMER;
        Map<String, Object> m = new LinkedHashMap<>(ticketJson(t));
        // customers never receive internal notes or draft content (FR-05)
        m.put("messages", messages.findByTicketIdOrderByCreatedAtAsc(id).stream().map(x ->
            Map.of("id", x.getId(), "body", x.getBody(), "fromAgent", x.isFromAgent(),
                "author", x.getAuthor().getUsername(), "createdAt", x.getCreatedAt().toString())).toList());
        if (staff) {
            m.put("internalNotes", notes.findByTicketIdOrderByCreatedAtAsc(id).stream().map(x ->
                Map.of("id", x.getId(), "body", x.getBody(), "author", x.getAuthor().getUsername(),
                    "createdAt", x.getCreatedAt().toString())).toList());
            m.put("triageSuggestions", triageRepo.findByTicketIdOrderByCreatedAtDesc(id).stream().map(s ->
                Map.of("id", s.getId(), "conversationVersion", s.getConversationVersion(),
                    "category", s.getSuggestedCategory().name(), "urgency", s.getSuggestedUrgency().name(),
                    "categoryReason", String.valueOf(s.getCategoryReason()), "urgencyReason", String.valueOf(s.getUrgencyReason()),
                    "applied", s.isApplied(), "stale", s.isStale() || s.getConversationVersion() != t.getConversationVersion())).toList());
            m.put("drafts", drafts.findByTicketIdOrderByCreatedAtDesc(id).stream().map(d ->
                Map.of("id", d.getId(), "conversationVersion", d.getConversationVersion(),
                    "draftText", d.getDraftText(), "status", d.getStatus().name(),
                    "needsClarification", d.isNeedsClarification(), "modelId", String.valueOf(d.getModelId()),
                    "citations", citations.findByDraftId(d.getId()).stream().map(c ->
                        Map.of("chunkId", c.getChunkId(), "articleVersionId", c.getArticleVersionId(),
                            "title", String.valueOf(c.getArticleTitle()), "excerpt", String.valueOf(c.getExcerpt()))).toList())).toList());
            m.put("jobs", jobs.findByTicketIdOrderByCreatedAtDesc(id).stream().map(j ->
                Map.of("id", j.getId(), "type", j.getType().name(), "status", j.getStatus().name(),
                    "attempts", j.getAttempts(), "lastError", String.valueOf(j.getLastError()))).toList());
        }
        return m;
    }

    @PostMapping("/{id}/claim")
    public Map<String, Object> claim(@PathVariable Long id) { return ticketJson(service.claim(id)); }

    @PostMapping("/{id}/assign")
    public Map<String, Object> assign(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long aid = body.get("assigneeId") == null ? null : Long.valueOf(body.get("assigneeId").toString());
        return ticketJson(service.assign(id, aid));
    }

    @PostMapping("/{id}/status")
    public Map<String, Object> status(@PathVariable Long id, @RequestBody ChangeStatusRequest req) {
        return ticketJson(service.changeStatus(id, TicketStatus.valueOf(req.status()), req.expectedVersion()));
    }

    @PostMapping("/{id}/messages")
    public Map<String, Object> message(@PathVariable Long id, @Valid @RequestBody AddMessageRequest req) {
        Message m = service.addMessage(id, req.body(), req.internal());
        if (m == null) return Map.of("internalNote", true);
        return Map.of("id", m.getId(), "body", m.getBody());
    }

    @PostMapping("/{id}/reopen")
    public Map<String, Object> reopen(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ticketJson(service.reopen(id, body.getOrDefault("reason", "")));
    }

    @PostMapping("/{id}/triage/{sid}/apply")
    public Map<String, Object> applyTriage(@PathVariable Long id, @PathVariable Long sid, @RequestBody Map<String, Object> body) {
        boolean c = !Boolean.FALSE.equals(body.getOrDefault("category", true));
        boolean u = !Boolean.FALSE.equals(body.getOrDefault("urgency", true));
        return ticketJson(triageService.apply(sid, c, u));
    }

    @PostMapping("/{id}/triage/override")
    public Map<String, Object> override(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Category c = body.get("category") == null ? null : Category.valueOf(body.get("category"));
        Urgency u = body.get("urgency") == null ? null : Urgency.valueOf(body.get("urgency"));
        return ticketJson(triageService.override(id, c, u));
    }

    @PostMapping("/{id}/drafts")
    public Map<String, Object> requestDraft(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String key = body == null ? null : body.get("idempotencyKey");
        var j = draftService.requestDraft(id, key);
        return Map.of("jobId", j.getId(), "status", j.getStatus().name());
    }

    @PostMapping("/{id}/manual-reply")
    public Map<String, Object> manualReply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Message m = draftService.manualReply(id, body.get("body"));
        return Map.of("id", m.getId());
    }

    private Map<String, Object> ticketJson(Ticket t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId()); m.put("ticketNumber", t.getTicketNumber());
        m.put("subject", t.getSubject()); m.put("status", t.getStatus().name());
        m.put("confirmedCategory", t.getConfirmedCategory().name());
        m.put("confirmedUrgency", t.getConfirmedUrgency().name());
        m.put("assignee", t.getAssignee() == null ? null : t.getAssignee().getUsername());
        m.put("conversationVersion", t.getConversationVersion());
        m.put("version", t.getVersion());
        m.put("createdAt", t.getCreatedAt().toString());
        return m;
    }
}
