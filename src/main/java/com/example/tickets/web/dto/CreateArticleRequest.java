package com.example.tickets.web.dto;

import jakarta.validation.constraints.Size;

public record CreateArticleRequest(@Size(min = 5, max = 150) String title, @Size(min = 50, max = 20000) String content) {}
