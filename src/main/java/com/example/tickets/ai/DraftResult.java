package com.example.tickets.ai;

import java.util.List;

public record DraftResult(String draftText, boolean needsClarification, List<Long> usedChunkIds, String modelId) {}
