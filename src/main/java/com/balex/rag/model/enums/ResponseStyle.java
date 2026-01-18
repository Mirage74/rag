package com.balex.rag.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ResponseStyle {
    CONCISE("Answer briefly."),
    DETAILED("Provide detailed explanations.");

    private final String instruction;
}