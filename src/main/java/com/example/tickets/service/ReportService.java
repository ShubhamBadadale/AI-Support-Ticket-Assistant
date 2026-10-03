package com.example.tickets.service;

import com.example.tickets.domain.*;
import com.example.tickets.repo.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ReportService {
    private final TicketRepository tickets;
    private final TriageSuggestionRepository triage;
    private final ReplyDraftRepository drafts;
    private final AiJobRepository jobs;

    public ReportService(TicketRepository tickets, TriageSuggestionRepository triage, ReplyDraftRepository drafts, AiJobRepository jobs) {
        this.tickets = tickets; this.triage = triage; this.drafts = drafts; this.jobs = jobs;
    }

    public Map<String, Object> summary() {
        List<com.example.tickets.domain.Ticket> all = tickets.findAll();
        Map<String, Long> byStatus = new HashMap<>(), byCategory = new HashMap<>();
        for (Ticket t : all) {
            byStatus.merge(t.getStatus().name(), 1L, Long::sum);
            byCategory.merge(t.getConfirmedCategory().name(), 1L, Long::sum);
        }
        long triageApplied = triage.findAll().stream().filter(TriageSuggestion::isApplied).count();
        long triageOverridden = all.size(); // confirmed values exist on every ticket; overridden tracked via audit
        long draftsSent = drafts.findAll().stream().filter(d -> d.getStatus() == DraftStatus.SENT).count();
        long draftsRejected = drafts.findAll().stream().filter(d -> d.getStatus() == DraftStatus.REJECTED).count();
        long failures = jobs.findAll().stream().filter(j -> j.getStatus() == JobStatus.FAILED).count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalTickets", all.size());
        m.put("byStatus", byStatus);
        m.put("byCategory", byCategory);
        m.put("triageApplied", triageApplied);
        m.put("draftsSent", draftsSent);
        m.put("draftsRejected", draftsRejected);
        m.put("jobFailures", failures);
        return m;
    }
}
