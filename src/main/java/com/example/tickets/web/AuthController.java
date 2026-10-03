package com.example.tickets.web;

import com.example.tickets.domain.Role;
import com.example.tickets.domain.User;
import com.example.tickets.repo.UserRepository;
import com.example.tickets.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authManager;
    private final UserRepository users;
    private final CurrentUser currentUser;

    public AuthController(AuthenticationManager authManager, UserRepository users, CurrentUser currentUser) {
        this.authManager = authManager; this.users = users; this.currentUser = currentUser;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body, HttpServletRequest req) {
        Authentication a = authManager.authenticate(new UsernamePasswordAuthenticationToken(body.get("username"), body.get("password")));
        SecurityContextHolder.getContext().setAuthentication(a);
        req.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
        User u = users.findByUsername(a.getName()).orElseThrow();
        return Map.of("username", u.getUsername(), "role", u.getRole().name(), "id", u.getId());
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        try {
            User u = currentUser.require();
            return Map.of("username", u.getUsername(), "role", u.getRole().name(), "id", u.getId());
        } catch (Exception e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "not logged in"); }
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return Map.of("ok", true);
    }
}
