package com.example.tickets.service;

import com.example.tickets.domain.*;
import com.example.tickets.repo.*;
import com.example.tickets.security.CurrentUser;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class TicketService {
    private final TicketRepository tickets;
    private final MessageRepository messages;
    private final InternalNoteRepository notes;
    private final UserRepository users;
    private final CurrentUser currentUser;
    private final AuditService audit;
    private final AiJobService jobs;

    public TicketService(TicketRepository tickets, MessageRepository messages, InternalNoteRepository notes,
                         UserRepository users, CurrentUser currentUser, AuditService audit, AiJobService jobs) {
        this.tickets = tickets; this.messages = messages; this.notes = notes; this.users = users;
        this.currentUser = currentUser; this.audit = audit; this.jobs = jobs;
    }

    @Transactional
    public Ticket create(String subject, String description) {
        if (subject == null || subject.length() < 5 || subject.length() > 150)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject must be 5-150 chars");
        if (description == null || description.length() < 20 || description.length() > 5000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description must be 20-5000 chars");
        User me = currentUser.require();
        Ticket t = new Ticket();
        t.setTicketNumber("T-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        t.setSubject(subject); t.setDescription(description);
        t.setStatus(TicketStatus.OPEN);
        t.setConfirmedCategory(Category.OTHER); t.setConfirmedUrgency(Urgency.NORMAL);
        t.setCreatedBy(me); t.setConversationVersion(1);
        tickets.save(t);
        Message m = new Message(); m.setTicket(t); m.setAuthor(me); m.setBody(description); m.setFromAgent(false);
        messages.save(m);
        audit.log("TICKET_CREATED", "Ticket", t.getId().toString(), null, t.getTicketNumber());
        // Enqueue triage — must not roll back ticket creation on AI failure (FR-02)
        try { jobs.enqueueTriage(t.getId(), t.getConversationVersion()); }
        catch (Exception ignored) {}
        return t;
    }

    public Ticket get(Long id) {
        Ticket t = tickets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ticket not found"));
        enforceRead(t);
        return t;
    }

    public Page<Ticket> queue(TicketStatus status, Category category, Urgency urgency, Long assigneeId, String q, int page, int size) {
        User me = currentUser.require();
        if (me.getRole() == Role.CUSTOMER) {
            Pageable p = PageRequest.of(page, Math.min(size, 100), Sort.by("updatedAt").descending());
            return tickets.findByCreatedById(me.getId(), p);
        }
        Pageable p = PageRequest.of(page, Math.min(size, 100), Sort.by("updatedAt").descending());
        return tickets.search(status, category, urgency, assigneeId, q, q, p);
    }

    @Transactional
    public Ticket claim(Long id) {
        Ticket t = tickets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        User me = currentUser.require();
        requireAgentOrAdmin(me);
        if (t.getAssignee() != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "already assigned");
        t.setAssignee(me);
        if (t.getStatus() == TicketStatus.OPEN) t.setStatus(TicketStatus.IN_PROGRESS);
        audit.log("TICKET_CLAIMED", "Ticket", t.getId().toString(), null, me.getUsername());
        return t;
    }

    @Transactional
    public Ticket assign(Long id, Long assigneeId) {
        Ticket t = tickets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        User me = currentUser.require();
        if (me.getRole() != Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "admin only");
        if (assigneeId == null) {
            String before = t.getAssignee() != null ? t.getAssignee().getUsername() : null;
            t.setAssignee(null);
            audit.log("TICKET_UNASSIGNED", "Ticket", t.getId().toString(), before, null);
            return t;
        }
        User a = users.findById(assigneeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "assignee not found"));
        // optimistic conflict detection via version check is handled by JPA @Version; simultaneous claims -> one wins
        t.setAssignee(a);
        audit.log("TICKET_ASSIGNED", "Ticket", t.getId().toString(), null, a.getUsername());
        return t;
    }

    @Transactional
    public Ticket changeStatus(Long id, TicketStatus to, Long expectedVersion) {
        Ticket t = tickets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        User me = currentUser.require();
        requireAssignedOrAdmin(t, me);
        if (expectedVersion != null && t.getVersion() != null && !t.getVersion().equals(expectedVersion))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "stale version, refresh and retry");
        TicketStatus from = t.getStatus();
        if (!allowed(from, to))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transition " + from + " -> " + to + " not allowed");
        t.setStatus(to);
        audit.log("STATUS_CHANGED", "Ticket", t.getId().toString(), from.name(), to.name());
        return t;
    }

    @Transactional
    public Message addMessage(Long ticketId, String body, boolean internal) {
        Ticket t = tickets.findById(ticketId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        User me = currentUser.require();
        if (body == null || body.length() < 1 || body.length() > 5000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must be 1-5000 chars");
        if (internal) {
            requireAssignedOrAdmin(t, me);
            InternalNote n = new InternalNote(); n.setTicket(t); n.setAuthor(me); n.setBody(body);
            notes.save(n);
            audit.log("INTERNAL_NOTE_ADDED", "Ticket", t.getId().toString(), null, "note:" + n.getId());
            return null;
        }
        if (me.getRole() == Role.CUSTOMER) {
            if (!t.getCreatedBy().getId().equals(me.getId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not your ticket");
            if (t.getStatus() == TicketStatus.RESOLVED)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resolved ticket: use reopen");
            Message m = new Message(); m.setTicket(t); m.setAuthor(me); m.setBody(body); m.setFromAgent(false);
            messages.save(m);
            t.setConversationVersion(t.getConversationVersion() + 1);
            if (t.getStatus() == TicketStatus.WAITING_FOR_CUSTOMER) t.setStatus(TicketStatus.OPEN);
            try { jobs.enqueueTriage(t.getId(), t.getConversationVersion()); } catch (Exception ignored) {}
            jobs.markDraftsStale(t.getId(), "customer follow-up v" + t.getConversationVersion());
            return m;
        } else {
            requireAssignedOrAdmin(t, me);
            Message m = new Message(); m.setTicket(t); m.setAuthor(me); m.setBody(body); m.setFromAgent(true);
            messages.save(m);
            t.setConversationVersion(t.getConversationVersion() + 1);
            return m;
        }
    }

    @Transactional
    public Ticket reopen(Long id, String reason) {
        Ticket t = tickets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        User me = currentUser.require();
        if (me.getRole() == Role.CUSTOMER) {
            if (!t.getCreatedBy().getId().equals(me.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            if (t.getStatus() != TicketStatus.RESOLVED) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only resolved tickets can be reopened");
            if (reason == null || reason.length() < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reopen reason required");
            Message m = new Message(); m.setTicket(t); m.setAuthor(me); m.setBody(reason); m.setFromAgent(false);
            messages.save(m);
            t.setStatus(TicketStatus.OPEN);
            t.setConversationVersion(t.getConversationVersion() + 1);
            try { jobs.enqueueTriage(t.getId(), t.getConversationVersion()); } catch (Exception ignored) {}
            audit.log("TICKET_REOPENED", "Ticket", t.getId().toString(), "RESOLVED", "OPEN");
            return t;
        }
        requireAssignedOrAdmin(t, me);
        t.setStatus(TicketStatus.OPEN);
        audit.log("TICKET_REOPENED", "Ticket", t.getId().toString(), null, "OPEN");
        return t;
    }

    private boolean allowed(TicketStatus from, TicketStatus to) {
        return switch (from) {
            case OPEN -> to == TicketStatus.IN_PROGRESS;
            case IN_PROGRESS -> to == TicketStatus.WAITING_FOR_CUSTOMER || to == TicketStatus.RESOLVED;
            case WAITING_FOR_CUSTOMER -> to == TicketStatus.IN_PROGRESS || to == TicketStatus.RESOLVED;
            case RESOLVED -> false;
        };
    }

    private void enforceRead(Ticket t) {
        User me = currentUser.require();
        if (me.getRole() == Role.CUSTOMER && !t.getCreatedBy().getId().equals(me.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not your ticket");
    }

    private void requireAgentOrAdmin(User me) {
        if (me.getRole() != Role.AGENT && me.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "agent or admin required");
    }

    private void requireAssignedOrAdmin(Ticket t, User me) {
        if (me.getRole() == Role.ADMIN) return;
        if (me.getRole() != Role.AGENT) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "agent required");
        if (t.getAssignee() == null || !t.getAssignee().getId().equals(me.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "only assigned agent or admin");
    }
}
