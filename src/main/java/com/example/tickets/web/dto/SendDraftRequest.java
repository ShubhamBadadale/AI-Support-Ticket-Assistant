package com.example.tickets.web.dto;

public record SendDraftRequest(String finalText, String idempotencyKey) {}
