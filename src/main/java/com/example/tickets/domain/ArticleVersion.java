package com.example.tickets.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "article_versions")
public class ArticleVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private KnowledgeArticle article;
    @Column(nullable = false)
    private int versionNumber;
    @Column(nullable = false, length = 20000)
    private String content;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ArticleIndexStatus indexStatus = ArticleIndexStatus.PENDING;
    @Column(length = 2000)
    private String indexError;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public KnowledgeArticle getArticle() { return article; }
    public void setArticle(KnowledgeArticle article) { this.article = article; }
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public ArticleIndexStatus getIndexStatus() { return indexStatus; }
    public void setIndexStatus(ArticleIndexStatus indexStatus) { this.indexStatus = indexStatus; }
    public String getIndexError() { return indexError; }
    public void setIndexError(String indexError) { this.indexError = indexError; }
    public Instant getCreatedAt() { return createdAt; }
}
