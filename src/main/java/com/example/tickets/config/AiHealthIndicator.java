package com.example.tickets.config;

import com.example.tickets.ai.AiRouter;
import org.springframework.boot.actuate.health.*;
import org.springframework.stereotype.Component;

@Component("ai")
public class AiHealthIndicator implements HealthIndicator {
    private final AiRouter router;
    public AiHealthIndicator(AiRouter router) { this.router = router; }

    @Override
    public Health health() {
        try {
            var p = router.active();
            if (p.isAvailable()) return Health.up().withDetail("provider", p.name()).build();
            return Health.down().withDetail("provider", p.name()).withDetail("reason", "AI unavailable; manual workflow active").build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
