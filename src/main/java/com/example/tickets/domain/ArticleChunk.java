package com.example.tickets.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "article_chunks")
public class ArticleChunk {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ArticleVersion articleVersion;
    @Column(nullable = false)
    private int chunkIndex;
    @Column(nullable = false, length = 4000)
    private String content;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ArticleVersion getArticleVersion() { return articleVersion; }
    public void setArticleVersion(ArticleVersion articleVersion) { this.articleVersion = articleVersion; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
