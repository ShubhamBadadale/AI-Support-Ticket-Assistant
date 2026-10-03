package com.example.tickets.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ai_jobs")
public class AiJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private JobType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.QUEUED;
    private Long ticketId;
    private Long articleVersionId;
    private Integer conversationVersion;
    @Column(nullable = false)
    private int attempts = 0;
    @Column(length = 2000)
    private String lastError;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
    @PreUpdate public void touch() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public JobType getType() { return type; }
    public void setType(JobType type) { this.type = type; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Long getArticleVersionId() { return articleVersionId; }
    public void setArticleVersionId(Long articleVersionId) { this.articleVersionId = articleVersionId; }
    public Integer getConversationVersion() { return conversationVersion; }
    public void setConversationVersion(Integer conversationVersion) { this.conversationVersion = conversationVersion; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
