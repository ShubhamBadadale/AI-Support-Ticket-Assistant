package com.example.tickets.service;

import com.example.tickets.domain.*;
import com.example.tickets.repo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demo dataset (PRD 13): 15 articles, 30 tickets, 2 customers, 2 agents, 1 admin.
 * Evaluation's 40 held-out cases are NOT seeded here — keep them separate.
 */
@Component
public class SeedData implements CommandLineRunner {
    private final UserRepository users;
    private final TicketRepository tickets;
    private final MessageRepository messages;
    private final KnowledgeArticleRepository articles;
    private final ArticleVersionRepository versions;
    private final PasswordEncoder encoder;
    private final AiJobService jobs;
    private final boolean enabled;

    public SeedData(UserRepository users, TicketRepository tickets, MessageRepository messages,
                    KnowledgeArticleRepository articles, ArticleVersionRepository versions,
                    PasswordEncoder encoder, AiJobService jobs,
                    @Value("${app.seed.enabled:true}") boolean enabled) {
        this.users = users; this.tickets = tickets; this.messages = messages;
        this.articles = articles; this.versions = versions; this.encoder = encoder; this.jobs = jobs;
        this.enabled = enabled;
    }

    @Override @Transactional
    public void run(String... args) {
        if (!enabled || users.count() > 0) return;
        User c1 = user("customer1", "password", Role.CUSTOMER);
        User c2 = user("customer2", "password", Role.CUSTOMER);
        User a1 = user("agent1", "password", Role.AGENT);
        User a2 = user("agent2", "password", Role.AGENT);
        User ad = user("admin", "password", Role.ADMIN);

        String[][] docs = {
            {"Account recovery", "If you are locked out of your account, use the password reset link on the sign-in page. Reset links expire after 30 minutes. If you do not receive the email, check spam, then contact support with your account email. Never share one-time codes. Support will never ask for your password."},
            {"Password reset policy", "Password resets require verifying the account email. We send a single-use link valid for 30 minutes. After three failed attempts the account locks for 15 minutes. Contact support to unlock with identity verification."},
            {"Two-factor authentication", "Enable 2FA from Settings > Security. Use an authenticator app. Save backup codes in a safe place. If you lose your device, use a backup code, then re-enroll a new device. Support cannot disable 2FA without verification."},
            {"Billing and invoices", "Invoices are issued monthly. View them under Billing > Invoices. Charges appear within 3 business days. For billing questions include the invoice number and date. Refunds follow the refund policy article."},
            {"Refund policy", "Refunds are reviewed case by case within 14 days of charge. To request a refund, provide the invoice number, reason, and purchase date. An agent must approve every refund; refunds are never automatic. Approved refunds take 5-10 business days."},
            {"Subscription upgrade", "Upgrade from Billing > Plan. Upgrades apply immediately with prorated charges. Downgrades apply at the next cycle. Changing plans does not delete data."},
            {"API timeout troubleshooting", "If API calls time out after 30 seconds, check network, retry with backoff of 1s then 3s, and reduce batch size. Verify your API key and endpoint region. Share request IDs with support for investigation."},
            {"App crash on startup", "Update to the latest version, clear cache, and restart. On mobile, reinstall if the crash persists. Include device model, OS version, and crash logs when contacting support."},
            {"Data export guide", "Export data from Settings > Export. Exports are CSV files delivered by email within an hour. Large exports may be split. Contact support if the export fails twice."},
            {"Notification setup", "Configure notifications from Settings > Notifications. Enable email or push per event. Test with the Send test button. Check spam filters if emails are missing."},
            {"Getting started", "Create an account, verify email, complete the onboarding checklist, invite teammates, and connect your first integration. The setup wizard takes about 10 minutes."},
            {"Roles and permissions", "Customers manage their own tickets. Agents handle assigned tickets. Admins manage users, assignments and knowledge articles. Internal notes are never visible to customers."},
            {"Service status and outages", "Check the status page for outages. If many users are affected, we post updates there. Individual timeouts are usually local; widespread outages require immediate human review and are announced."},
            {"Security incident reporting", "If you suspect unauthorized access, change your password immediately, revoke sessions, enable 2FA, and report to support with timestamps. Do not share passwords in tickets. We investigate reported incidents urgently."},
            {"Contacting support", "Submit tickets with a clear subject (5-150 chars) and description (20-5000 chars). Include steps, expected vs actual behavior, and error text. Agents reply in the ticket thread; you can reopen resolved tickets with new details."}
        };
        for (String[] d : docs) {
            KnowledgeArticle a = new KnowledgeArticle();
            a.setTitle(d[0]); a.setActive(true); a.setCreatedBy(ad);
            articles.save(a);
            ArticleVersion v = new ArticleVersion();
            v.setArticle(a); v.setVersionNumber(1);
            v.setContent(d[0] + ". " + d[1] + " " + d[1]);
            v.setIndexStatus(ArticleIndexStatus.PENDING);
            versions.save(v);
            jobs.enqueueIndex(v.getId());
        }

        String[][] tix = {
            {"Locked out of my account", "I tried to sign in 5 times and now I am locked out of my account. Password reset email never arrived. Please help, I need access for work today.", "customer1"},
            {"Refund for double charge", "I was charged twice on invoice INV-2041 dated last week. Please refund the duplicate charge of $49.", "customer1"},
            {"API returns 500 errors", "Our integration gets HTTP 500 and timeouts since this morning on the orders endpoint. Request id req_abc123. This blocks our production sync.", "customer2"},
            {"How to enable 2FA?", "How do I set up two-factor authentication for my team? We want authenticator app setup steps.", "customer2"},
            {"Cannot export data", "Export from Settings fails every time with timeout. Tried twice, file never arrives by email.", "customer1"}
        };
        int n = 0;
        for (int i = 0; i < 30; i++) {
            String[] s = tix[i % tix.length];
            User author = s[2].equals("customer1") ? c1 : c2;
            Ticket t = new Ticket();
            t.setTicketNumber("DEMO-" + String.format("%04d", i + 1));
            t.setSubject(s[0] + (i >= tix.length ? " (case " + (i + 1) + ")" : ""));
            t.setDescription(s[1] + " Additional context case " + (i + 1) + " with sufficient length for validation.");
            t.setStatus(i % 4 == 0 ? TicketStatus.OPEN : i % 4 == 1 ? TicketStatus.IN_PROGRESS : i % 4 == 2 ? TicketStatus.WAITING_FOR_CUSTOMER : TicketStatus.RESOLVED);
            t.setConfirmedCategory(Role.ADMIN != null ? com.example.tickets.domain.Category.OTHER : null);
            t.setConfirmedUrgency(com.example.tickets.domain.Urgency.NORMAL);
            t.setCreatedBy(author);
            if (i % 2 == 0) t.setAssignee(i % 4 == 0 ? a1 : a2);
            t.setConversationVersion(1);
            tickets.save(t);
            Message m = new Message(); m.setTicket(t); m.setAuthor(author); m.setBody(s[1]); m.setFromAgent(false);
            messages.save(m);
            n++;
        }
    }

    private User user(String name, String pw, Role r) {
        User u = new User();
        u.setUsername(name); u.setPasswordHash(encoder.encode(pw)); u.setRole(r); u.setEnabled(true);
        return users.save(u);
    }
}
