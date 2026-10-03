package com.example.tickets.ai;

import com.example.tickets.domain.Category;
import com.example.tickets.domain.Urgency;
import com.example.tickets.service.RetrievalService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic rule-based stub used for workflow development, tests and offline demo.
 * Also acts as fallback when the real provider is unavailable (FR-11).
 * Prompt-injection hardened: treats ticket content as data, never executes instructions inside it.
 */
@Component
public class StubAiProvider implements AiProvider {
    @Value("${app.ai.stub-failure-rate:0.0}")
    double failureRate;

    @Override public String name() { return "stub-1.0"; }

    @Override public boolean isAvailable() { return true; }

    @Override
    public TriageResult triage(String subject, String conversation) {
        String text = ((subject == null ? "" : subject) + " " + (conversation == null ? "" : conversation)).toLowerCase(Locale.ROOT);
        Category cat = Category.OTHER;
        String catReason = "No specific account, billing, technical or how-to keywords found.";
        if (containsAny(text, "password", "login", "locked", "lock out", "sign in", "2fa", "otp", "account", "recover")) {
            cat = Category.ACCOUNT_ACCESS; catReason = "Mentions login, lockout or account recovery.";
        } else if (containsAny(text, "refund", "invoice", "bill", "charge", "payment", "subscription")) {
            cat = Category.BILLING; catReason = "Mentions billing, charges or refunds.";
        } else if (containsAny(text, "error", "crash", "bug", "500", "exception", "timeout", "api", "outage")) {
            cat = Category.TECHNICAL; catReason = "Mentions errors, crashes or API failures.";
        } else if (containsAny(text, "how to", "how do", "setup", "configure", "guide", "tutorial")) {
            cat = Category.HOW_TO; catReason = "Asks for guidance or configuration steps.";
        }
        Urgency urg = Urgency.NORMAL;
        String urgReason = "Individual issue with limited impact.";
        if (containsAny(text, "breach", "hacked", "intrusion", "security incident", "widespread outage", "everyone down", "all users")) {
            urg = Urgency.URGENT; urgReason = "Reports possible security incident or widespread outage; needs immediate human review.";
        } else if (containsAny(text, "cannot work", "blocked", "can't complete", "no workaround", "production down", "urgent", "asap")) {
            urg = Urgency.HIGH; urgReason = "Customer cannot complete a core task with no known workaround.";
        } else if (containsAny(text, "question", "general", "curious", "documentation")) {
            urg = Urgency.LOW; urgReason = "General guidance request.";
        }
        return new TriageResult(cat, urg, catReason, urgReason, name());
    }

    @Override
    public DraftResult draftReply(String conversation, List<RetrievalService.ScoredChunk> context) {
        if (context == null || context.isEmpty()) {
            return new DraftResult(
                "Thanks for contacting support. I don't have an approved article covering this request, so I need a bit more detail. Could you describe the exact steps, any error messages, and what you expected? An agent will review and follow up.",
                true, List.of(), name());
        }
        StringBuilder sb = new StringBuilder("Thanks for contacting support. Based on our published guidance:\n\n");
        for (var c : context.stream().limit(3).toList()) {
            String excerpt = c.chunk().getContent();
            if (excerpt.length() > 400) excerpt = excerpt.substring(0, 400) + "...";
            sb.append("- ").append(excerpt.replaceAll("\\s+", " ")).append(" [source: ").append(c.title()).append("]\n");
        }
        sb.append("\nIf these steps don't resolve it, please reply with what you tried and any error text, and an agent will investigate further.");
        List<Long> ids = context.stream().map(c -> c.chunk().getId()).toList();
        return new DraftResult(sb.toString(), false, ids, name());
    }

    private boolean containsAny(String text, String... keys) {
        for (String k : keys) if (text.contains(k)) return true;
        return false;
    }
}
