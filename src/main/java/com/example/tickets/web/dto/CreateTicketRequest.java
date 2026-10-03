package com.example.tickets.web.dto;

import jakarta.validation.constraints.Size;

public record CreateTicketRequest(@Size(min = 5, max = 150) String subject, @Size(min = 20, max = 5000) String description) {}
