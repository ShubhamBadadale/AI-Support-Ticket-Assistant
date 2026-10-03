package com.example.tickets.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "triage_suggestions")
public class TriageSuggestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Ticket ticket;
    @Column(nullable = false)
    private int conversationVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Category suggestedCategory;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Urgency suggestedUrgency;
    @Column(length = 500)
    private String categoryReason;
    @Column(length = 500)
    private String urgencyReason;
    @Column(nullable = false)
    private boolean applied = false;
    @Column(nullable = false)
    private boolean stale = false;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    public int getConversationVersion() { return conversationVersion; }
    public void setConversationVersion(int conversationVersion) { this.conversationVersion = conversationVersion; }
    public Category getSuggestedCategory() { return suggestedCategory; }
    public void setSuggestedCategory(Category suggestedCategory) { this.suggestedCategory = suggestedCategory; }
    public Urgency getSuggestedUrgency() { return suggestedUrgency; }
    public void setSuggestedUrgency(Urgency suggestedUrgency) { this.suggestedUrgency = suggestedUrgency; }
    public String getCategoryReason() { return categoryReason; }
    public void setCategoryReason(String categoryReason) { this.categoryReason = categoryReason; }
    public String getUrgencyReason() { return urgencyReason; }
    public void setUrgencyReason(String urgencyReason) { this.urgencyReason = urgencyReason; }
    public boolean isApplied() { return applied; }
    public void setApplied(boolean applied) { this.applied = applied; }
    public boolean isStale() { return stale; }
    public void setStale(boolean stale) { this.stale = stale; }
    public Instant getCreatedAt() { return createdAt; }
}
