package com.example.tickets.security;

import com.example.tickets.domain.User;
import com.example.tickets.repo.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final UserRepository users;
    public CurrentUser(UserRepository users) { this.users = users; }

    public User require() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a.getName().equals("anonymousUser"))
            throw new org.springframework.security.access.AccessDeniedException("unauthenticated");
        return users.findByUsername(a.getName()).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("unknown user"));
    }
}
