package com.example.tickets.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiRouter {
    private final StubAiProvider stub;
    private final OpenAiProvider openai;
    private final String configured;

    public AiRouter(StubAiProvider stub, OpenAiProvider openai, @Value("${app.ai.provider:stub}") String configured) {
        this.stub = stub; this.openai = openai; this.configured = configured;
    }

    public AiProvider active() {
        if ("openai".equalsIgnoreCase(configured) && openai.isAvailable()) return openai;
        return stub;
    }
}
