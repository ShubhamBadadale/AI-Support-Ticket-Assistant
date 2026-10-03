package com.example.tickets.service;

import com.example.tickets.domain.*;
import com.example.tickets.repo.*;
import com.example.tickets.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class KnowledgeService {
    private final KnowledgeArticleRepository articles;
    private final ArticleVersionRepository versions;
    private final ArticleChunkRepository chunks;
    private final CurrentUser currentUser;
    private final AuditService audit;
    private final AiJobService jobs;

    public KnowledgeService(KnowledgeArticleRepository articles, ArticleVersionRepository versions,
                            ArticleChunkRepository chunks, CurrentUser currentUser, AuditService audit, AiJobService jobs) {
        this.articles = articles; this.versions = versions; this.chunks = chunks;
        this.currentUser = currentUser; this.audit = audit; this.jobs = jobs;
    }

    @Transactional
    public KnowledgeArticle create(String title, String content) {
        requireAdmin();
        validate(title, content);
        User me = currentUser.require();
        KnowledgeArticle a = new KnowledgeArticle();
        a.setTitle(title); a.setActive(true); a.setCreatedBy(me);
        articles.save(a);
        ArticleVersion v = new ArticleVersion();
        v.setArticle(a); v.setVersionNumber(1); v.setContent(content); v.setIndexStatus(ArticleIndexStatus.PENDING);
        versions.save(v);
        jobs.enqueueIndex(v.getId());
        audit.log("ARTICLE_CREATED", "KnowledgeArticle", a.getId().toString(), null, title);
        return a;
    }

    @Transactional
    public ArticleVersion edit(Long articleId, String title, String content) {
        requireAdmin();
        validate(title, content);
        KnowledgeArticle a = articles.findById(articleId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (title != null) a.setTitle(title);
        int next = versions.findByArticleIdOrderByVersionNumberDesc(articleId).stream().mapToInt(ArticleVersion::getVersionNumber).max().orElse(0) + 1;
        ArticleVersion v = new ArticleVersion();
        v.setArticle(a); v.setVersionNumber(next); v.setContent(content); v.setIndexStatus(ArticleIndexStatus.PENDING);
        versions.save(v);
        jobs.enqueueIndex(v.getId());
        audit.log("ARTICLE_EDITED", "KnowledgeArticle", a.getId().toString(), null, "v" + next);
        return v;
    }

    @Transactional
    public void deactivate(Long articleId) {
        requireAdmin();
        KnowledgeArticle a = articles.findById(articleId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        a.setActive(false);
        jobs.markDraftsStaleForArticle(articleId);
        audit.log("ARTICLE_DEACTIVATED", "KnowledgeArticle", a.getId().toString(), "active", "inactive");
    }

    /** Called async by AiJobService to chunk + mark READY (previous version stays usable until replacement ready — FR Journey C). */
    @Transactional
    public void indexVersion(Long versionId) {
        ArticleVersion v = versions.findById(versionId).orElseThrow();
        try {
            chunks.findByArticleVersionId(versionId).forEach(chunks::delete);
            var parts = RetrievalService.chunkContent(v.getContent(), 1200);
            int i = 0;
            for (String p : parts) {
                ArticleChunk c = new ArticleChunk();
                c.setArticleVersion(v); c.setChunkIndex(i++); c.setContent(p);
                chunks.save(c);
            }
            v.setIndexStatus(ArticleIndexStatus.READY);
            v.setIndexError(null);
            // publish: only swap published pointer after READY
            KnowledgeArticle a = v.getArticle();
            a.setPublishedVersionId(v.getId());
        } catch (Exception e) {
            v.setIndexStatus(ArticleIndexStatus.FAILED);
            v.setIndexError(e.getMessage());
        }
    }

    private void validate(String title, String content) {
        if (title == null || title.length() < 5 || title.length() > 150)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title 5-150 chars");
        if (content == null || content.length() < 50 || content.length() > 20000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content 50-20000 chars");
    }

    private void requireAdmin() {
        if (currentUser.require().getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "admin only");
    }
}
