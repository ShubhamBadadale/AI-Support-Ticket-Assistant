package com.example.tickets.service;

import com.example.tickets.domain.*;
import com.example.tickets.repo.*;
import com.example.tickets.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TriageService {
    private final TriageSuggestionRepository repo;
    private final TicketRepository tickets;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public TriageService(TriageSuggestionRepository repo, TicketRepository tickets, CurrentUser currentUser, AuditService audit) {
        this.repo = repo; this.tickets = tickets; this.currentUser = currentUser; this.audit = audit;
    }

    @Transactional
    public Ticket apply(Long suggestionId, boolean applyCategory, boolean applyUrgency) {
        TriageSuggestion s = repo.findById(suggestionId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Ticket t = s.getTicket();
        var me = currentUser.require();
        if (me.getRole() == Role.CUSTOMER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (me.getRole() == Role.AGENT && (t.getAssignee() == null || !t.getAssignee().getId().equals(me.getId())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "only assigned agent or admin");
        if (s.isStale() || s.getConversationVersion() != t.getConversationVersion())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "suggestion is stale; regenerate");
        String before = t.getConfirmedCategory() + "/" + t.getConfirmedUrgency();
        if (applyCategory) t.setConfirmedCategory(s.getSuggestedCategory());
        if (applyUrgency) t.setConfirmedUrgency(s.getSuggestedUrgency());
        s.setApplied(true);
        audit.log("TRIAGE_APPLIED", "Ticket", t.getId().toString(), before, t.getConfirmedCategory() + "/" + t.getConfirmedUrgency());
        return t;
    }

    @Transactional
    public Ticket override(Long ticketId, Category category, Urgency urgency) {
        Ticket t = tickets.findById(ticketId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var me = currentUser.require();
        if (me.getRole() == Role.CUSTOMER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (me.getRole() == Role.AGENT && (t.getAssignee() == null || !t.getAssignee().getId().equals(me.getId())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "only assigned agent or admin");
        String before = t.getConfirmedCategory() + "/" + t.getConfirmedUrgency();
        if (category != null) t.setConfirmedCategory(category);
        if (urgency != null) t.setConfirmedUrgency(urgency);
        audit.log("TRIAGE_OVERRIDDEN", "Ticket", t.getId().toString(), before, t.getConfirmedCategory() + "/" + t.getConfirmedUrgency());
        return t;
    }
}
