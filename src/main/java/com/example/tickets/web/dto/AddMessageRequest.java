package com.example.tickets.web.dto;

import jakarta.validation.constraints.Size;

public record AddMessageRequest(@Size(min = 1, max = 5000) String body, boolean internal) {}
