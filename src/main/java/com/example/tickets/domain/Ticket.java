package com.example.tickets.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tickets", indexes = {
    @Index(columnList = "ticketNumber", unique = true),
    @Index(columnList = "status"), @Index(columnList = "assignee_id")
})
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 20)
    private String ticketNumber;
    @Column(nullable = false, length = 150)
    private String subject;
    @Column(nullable = false, length = 5000)
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private TicketStatus status = TicketStatus.OPEN;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Category confirmedCategory = Category.OTHER;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Urgency confirmedUrgency = Urgency.NORMAL;
    @ManyToOne(fetch = FetchType.LAZY)
    private User assignee;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User createdBy;
    @Column(nullable = false)
    private int conversationVersion = 1;
    @Version
    private Long version;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate public void touch() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public Category getConfirmedCategory() { return confirmedCategory; }
    public void setConfirmedCategory(Category confirmedCategory) { this.confirmedCategory = confirmedCategory; }
    public Urgency getConfirmedUrgency() { return confirmedUrgency; }
    public void setConfirmedUrgency(Urgency confirmedUrgency) { this.confirmedUrgency = confirmedUrgency; }
    public User getAssignee() { return assignee; }
    public void setAssignee(User assignee) { this.assignee = assignee; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public int getConversationVersion() { return conversationVersion; }
    public void setConversationVersion(int conversationVersion) { this.conversationVersion = conversationVersion; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
