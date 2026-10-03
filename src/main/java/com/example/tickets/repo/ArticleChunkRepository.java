package com.example.tickets.repo;

import com.example.tickets.domain.ArticleChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArticleChunkRepository extends JpaRepository<ArticleChunk, Long> {
    List<ArticleChunk> findByArticleVersionId(Long versionId);
    List<ArticleChunk> findByArticleVersionArticleIdAndArticleVersionIndexStatus(Long articleId, com.example.tickets.domain.ArticleIndexStatus status);
}
