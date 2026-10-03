package com.example.tickets.ai;

import com.example.tickets.service.RetrievalService;
import java.util.List;

public interface AiProvider {
    String name();
    TriageResult triage(String subject, String conversation) throws Exception;
    DraftResult draftReply(String conversation, List<RetrievalService.ScoredChunk> context) throws Exception;
    boolean isAvailable();
}
