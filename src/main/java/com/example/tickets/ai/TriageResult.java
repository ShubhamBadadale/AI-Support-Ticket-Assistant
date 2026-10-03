package com.example.tickets.ai;

import com.example.tickets.domain.Category;
import com.example.tickets.domain.Urgency;

public record TriageResult(Category category, Urgency urgency, String categoryReason, String urgencyReason, String modelId) {}
