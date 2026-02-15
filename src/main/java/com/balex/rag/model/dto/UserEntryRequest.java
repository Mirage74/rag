package com.balex.rag.model.dto;

public record UserEntryRequest(
        String content,
        Boolean onlyContext,
        Integer topK,
        Double topP
) {}
