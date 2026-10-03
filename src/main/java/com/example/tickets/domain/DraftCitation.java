package com.example.tickets.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "draft_citations")
public class DraftCitation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ReplyDraft draft;
    private Long articleVersionId;
    private Long chunkId;
    @Column(length = 1000)
    private String excerpt;
    @Column(length = 200)
    private String articleTitle;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ReplyDraft getDraft() { return draft; }
    public void setDraft(ReplyDraft draft) { this.draft = draft; }
    public Long getArticleVersionId() { return articleVersionId; }
    public void setArticleVersionId(Long articleVersionId) { this.articleVersionId = articleVersionId; }
    public Long getChunkId() { return chunkId; }
    public void setChunkId(Long chunkId) { this.chunkId = chunkId; }
    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }
    public String getArticleTitle() { return articleTitle; }
    public void setArticleTitle(String articleTitle) { this.articleTitle = articleTitle; }
}
