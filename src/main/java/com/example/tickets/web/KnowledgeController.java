package com.example.tickets.web;

import com.example.tickets.domain.*;
import com.example.tickets.repo.*;
import com.example.tickets.service.*;
import com.example.tickets.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/articles")
public class KnowledgeController {
    private final KnowledgeService service;
    private final KnowledgeArticleRepository articles;
    private final ArticleVersionRepository versions;
    private final RetrievalService retrieval;

    public KnowledgeController(KnowledgeService service, KnowledgeArticleRepository articles,
                               ArticleVersionRepository versions, RetrievalService retrieval) {
        this.service = service; this.articles = articles; this.versions = versions; this.retrieval = retrieval;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return articles.findAll().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId()); m.put("title", a.getTitle()); m.put("active", a.isActive());
            m.put("publishedVersionId", a.getPublishedVersionId());
            var vs = versions.findByArticleIdOrderByVersionNumberDesc(a.getId());
            m.put("latestStatus", vs.isEmpty() ? null : vs.get(0).getIndexStatus().name());
            m.put("latestVersion", vs.isEmpty() ? null : vs.get(0).getVersionNumber());
            return m;
        }).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        var a = articles.findById(id).orElseThrow();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId()); m.put("title", a.getTitle()); m.put("active", a.isActive());
        m.put("publishedVersionId", a.getPublishedVersionId());
        m.put("versions", versions.findByArticleIdOrderByVersionNumberDesc(id).stream().map(v ->
            Map.of("id", v.getId(), "versionNumber", v.getVersionNumber(), "status", v.getIndexStatus().name(),
                "content", v.getContent(), "error", String.valueOf(v.getIndexError()))).toList());
        return m;
    }

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody CreateArticleRequest req) {
        var a = service.create(req.title(), req.content());
        return Map.of("id", a.getId(), "title", a.getTitle());
    }

    @PutMapping("/{id}")
    public Map<String, Object> edit(@PathVariable Long id, @Valid @RequestBody CreateArticleRequest req) {
        var v = service.edit(id, req.title(), req.content());
        return Map.of("versionId", v.getId(), "status", v.getIndexStatus().name());
    }

    @PostMapping("/{id}/deactivate")
    public Map<String, Object> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return Map.of("ok", true);
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String q) {
        return retrieval.retrieve(q, 5).stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("chunkId", c.chunk().getId()); m.put("title", c.title()); m.put("score", c.score());
            m.put("excerpt", c.chunk().getContent().substring(0, Math.min(500, c.chunk().getContent().length())));
            return m;
        }).toList();
    }
}
