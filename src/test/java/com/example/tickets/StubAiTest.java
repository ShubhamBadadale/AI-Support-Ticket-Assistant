package com.example.tickets;

import com.example.tickets.ai.StubAiProvider;
import com.example.tickets.service.RetrievalService;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StubAiTest {
    @Test
    void triageClassifiesAccountAccess() {
        var p = new StubAiProvider();
        var r = p.triage("Locked out", "I cannot login, password reset never arrived, locked out");
        assertEquals(com.example.tickets.domain.Category.ACCOUNT_ACCESS, r.category());
    }

    @Test
    void emptyRetrievalProducesClarification() {
        var p = new StubAiProvider();
        var r = p.draftReply("something weird with no docs", List.of());
        assertTrue(r.needsClarification());
        assertTrue(r.draftText().toLowerCase().contains("more detail"));
    }

    @Test
    void chunkingKeepsContent() {
        var parts = RetrievalService.chunkContent("a\nb\nc", 2);
        assertFalse(parts.isEmpty());
    }
}
