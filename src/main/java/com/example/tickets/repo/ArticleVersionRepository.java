package com.example.tickets.repo;

import com.example.tickets.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArticleVersionRepository extends JpaRepository<ArticleVersion, Long> {
    List<ArticleVersion> findByArticleIdOrderByVersionNumberDesc(Long articleId);
}
