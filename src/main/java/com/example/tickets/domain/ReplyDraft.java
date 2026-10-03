package com.example.tickets.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reply_drafts")
public class ReplyDraft {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Ticket ticket;
    @Column(nullable = false)
    private int conversationVersion;
    @Column(nullable = false, length = 5000)
    private String draftText;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private DraftStatus status = DraftStatus.DRAFT;
    @Column(nullable = false)
    private boolean needsClarification = false;
    @Column(length = 100)
    private String modelId;
    @Column(length = 50)
    private String promptVersion;
    private Long elapsedMs;
    @Column(length = 100)
    private String idempotencyKey;
    @Column(length = 2000)
    private String sourceVersions;
    @ManyToOne(fetch = FetchType.LAZY)
    private User createdBy;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    public int getConversationVersion() { return conversationVersion; }
    public void setConversationVersion(int conversationVersion) { this.conversationVersion = conversationVersion; }
    public String getDraftText() { return draftText; }
    public void setDraftText(String draftText) { this.draftText = draftText; }
    public DraftStatus getStatus() { return status; }
    public void setStatus(DraftStatus status) { this.status = status; }
    public boolean isNeedsClarification() { return needsClarification; }
    public void setNeedsClarification(boolean needsClarification) { this.needsClarification = needsClarification; }
    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public Long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(Long elapsedMs) { this.elapsedMs = elapsedMs; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getSourceVersions() { return sourceVersions; }
    public void setSourceVersions(String sourceVersions) { this.sourceVersions = sourceVersions; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
