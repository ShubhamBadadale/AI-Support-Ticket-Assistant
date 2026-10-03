package com.example.tickets.web;

import com.example.tickets.domain.Message;
import com.example.tickets.repo.*;
import com.example.tickets.service.DraftService;
import com.example.tickets.web.dto.SendDraftRequest;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/drafts")
public class DraftController {
    private final DraftService drafts;
    private final ReplyDraftRepository repo;
    private final DraftCitationRepository citations;

    public DraftController(DraftService drafts, ReplyDraftRepository repo, DraftCitationRepository citations) {
        this.drafts = drafts; this.repo = repo; this.citations = citations;
    }

    @PutMapping("/{id}")
    public Map<String, Object> edit(@PathVariable Long id, @RequestBody Map<String, String> body) {
        var d = drafts.edit(id, body.get("draftText"));
        return Map.of("id", d.getId(), "draftText", d.getDraftText());
    }

    @PostMapping("/{id}/reject")
    public Map<String, Object> reject(@PathVariable Long id) {
        drafts.reject(id);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/send")
    public Map<String, Object> send(@PathVariable Long id, @RequestBody SendDraftRequest req) {
        Message m = drafts.send(id, req.finalText(), req.idempotencyKey());
        return Map.of("messageId", m.getId());
    }

    @PostMapping("/{id}/regenerate")
    public Map<String, Object> regenerate(@PathVariable Long id) {
        var d = repo.findById(id).orElseThrow();
        var job = drafts.requestDraft(d.getTicket().getId(), null);
        return Map.of("jobId", job.getId());
    }
}
