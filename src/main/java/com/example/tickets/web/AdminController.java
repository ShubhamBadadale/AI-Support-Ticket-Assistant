package com.example.tickets.web;

import com.example.tickets.domain.Role;
import com.example.tickets.domain.User;
import com.example.tickets.repo.*;
import com.example.tickets.security.CurrentUser;
import com.example.tickets.service.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final CurrentUser currentUser;
    private final ReportService reports;
    private final AuditEventRepository audits;
    private final AiJobRepository jobs;

    public AdminController(UserRepository users, PasswordEncoder encoder, CurrentUser currentUser,
                           ReportService reports, AuditEventRepository audits, AiJobRepository jobs) {
        this.users = users; this.encoder = encoder; this.currentUser = currentUser;
        this.reports = reports; this.audits = audits; this.jobs = jobs;
    }

    private void requireAdmin() {
        if (currentUser.require().getRole() != Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        requireAdmin();
        return users.findAll().stream().map(u -> Map.<String, Object>of("id", u.getId(), "username", u.getUsername(), "role", u.getRole().name())).toList();
    }

    @PostMapping("/users")
    public Map<String, Object> createUser(@RequestBody Map<String, String> body) {
        requireAdmin();
        User u = new User();
        u.setUsername(body.get("username"));
        u.setPasswordHash(encoder.encode(body.get("password")));
        u.setRole(Role.valueOf(body.getOrDefault("role", "CUSTOMER")));
        users.save(u);
        return Map.of("id", u.getId(), "username", u.getUsername());
    }

    @GetMapping("/reports/summary")
    public Map<String, Object> summary() {
        if (currentUser.require().getRole() == Role.CUSTOMER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return reports.summary();
    }

    @GetMapping("/audit")
    public Map<String, Object> audit(@RequestParam(defaultValue = "0") int page) {
        requireAdmin();
        var p = audits.findAllByOrderByTimestampDesc(PageRequest.of(page, 50));
        return Map.of("content", p.getContent(), "totalElements", p.getTotalElements());
    }

    @PostMapping("/jobs/{id}/retry")
    public Map<String, Object> retry(@PathVariable Long id) {
        requireAdmin();
        var j = jobs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        j.setStatus(com.example.tickets.domain.JobStatus.QUEUED);
        jobs.save(j);
        return Map.of("ok", true);
    }

    @GetMapping("/jobs")
    public List<Map<String, Object>> allJobs() {
        requireAdmin();
        return jobs.findAll().stream().map(j -> Map.<String, Object>of("id", j.getId(), "type", j.getType().name(),
            "status", j.getStatus().name(), "ticketId", String.valueOf(j.getTicketId()), "attempts", j.getAttempts(),
            "lastError", String.valueOf(j.getLastError()))).toList();
    }
}
