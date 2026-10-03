package com.example.tickets.service;

import com.example.tickets.domain.AuditEvent;
import com.example.tickets.repo.AuditEventRepository;
import com.example.tickets.security.CurrentUser;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditEventRepository repo;
    private final CurrentUser currentUser;
    public AuditService(AuditEventRepository repo, CurrentUser currentUser) {
        this.repo = repo; this.currentUser = currentUser;
    }
    public void log(String action, String entityType, String entityId, String beforeJson, String afterJson) {
        try {
            var u = currentUser.require();
            AuditEvent e = new AuditEvent();
            e.setActorId(u.getId()); e.setActorUsername(u.getUsername());
            e.setAction(action); e.setEntityType(entityType); e.setEntityId(entityId);
            e.setBeforeJson(beforeJson); e.setAfterJson(afterJson);
            repo.save(e);
        } catch (Exception ignored) {
            AuditEvent e = new AuditEvent();
            e.setAction(action); e.setEntityType(entityType); e.setEntityId(entityId);
            e.setBeforeJson(beforeJson); e.setAfterJson(afterJson);
            repo.save(e);
        }
    }
}
