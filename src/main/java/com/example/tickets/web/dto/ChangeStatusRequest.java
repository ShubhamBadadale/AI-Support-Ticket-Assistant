package com.example.tickets.web.dto;

public record ChangeStatusRequest(String status, Long expectedVersion) {}
