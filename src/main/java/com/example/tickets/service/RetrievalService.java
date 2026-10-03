package com.example.tickets.service;

import com.example.tickets.domain.*;
import com.example.tickets.repo.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RetrievalService {
    public record ScoredChunk(ArticleChunk chunk, String title, Long articleVersionId, double score) {}

    private final ArticleChunkRepository chunks;
    private final ArticleVersionRepository versions;
    private final KnowledgeArticleRepository articles;

    public RetrievalService(ArticleChunkRepository chunks, ArticleVersionRepository versions, KnowledgeArticleRepository articles) {
        this.chunks = chunks; this.versions = versions; this.articles = articles;
    }

    /** Keyword-overlap retrieval over active READY versions, top-5 with threshold. */
    public List<ScoredChunk> retrieve(String query, int topK) {
        if (query == null || query.isBlank()) return List.of();
        Set<String> qTokens = tokens(query);
        List<ScoredChunk> out = new ArrayList<>();
        for (KnowledgeArticle a : articles.findByActiveTrue()) {
            if (a.getPublishedVersionId() == null) continue;
            var v = versions.findById(a.getPublishedVersionId()).orElse(null);
            if (v == null || v.getIndexStatus() != ArticleIndexStatus.READY) continue;
            if (!a.isActive()) continue;
            for (ArticleChunk c : chunks.findByArticleVersionId(v.getId())) {
                Set<String> cTokens = tokens(c.getContent());
                double score = overlap(qTokens, cTokens);
                // title boost
                if (tokens(a.getTitle()).stream().anyMatch(qTokens::contains)) score += 0.2;
                if (score > 0.05) out.add(new ScoredChunk(c, a.getTitle(), v.getId(), score));
            }
        }
        out.sort((x, y) -> Double.compare(y.score(), x.score()));
        return out.stream().limit(Math.min(topK, 5)).toList();
    }

    private Set<String> tokens(String s) {
        Set<String> t = new HashSet<>();
        for (String w : s.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (w.length() > 2) t.add(w);
        }
        return t;
    }

    private double overlap(Set<String> q, Set<String> c) {
        if (q.isEmpty() || c.isEmpty()) return 0;
        long inter = q.stream().filter(c::contains).count();
        return (double) inter / Math.sqrt(q.size() * c.size());
    }

    public static List<String> chunkContent(String content, int maxLen) {
        List<String> out = new ArrayList<>();
        String[] paras = content.split("\n+");
        StringBuilder cur = new StringBuilder();
        for (String p : paras) {
            if (cur.length() + p.length() > maxLen && cur.length() > 0) {
                out.add(cur.toString().trim()); cur.setLength(0);
            }
            cur.append(p).append("\n");
            if (cur.length() >= maxLen) { out.add(cur.toString().trim()); cur.setLength(0); }
        }
        if (!cur.toString().isBlank()) out.add(cur.toString().trim());
        return out.isEmpty() ? List.of(content) : out;
    }
}
