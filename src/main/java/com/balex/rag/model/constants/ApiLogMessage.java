package com.balex.rag.model.constants;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ApiLogMessage {
    NAME_OF_CURRENT_METHOD("Current method: {}");
    private final String value;
}

