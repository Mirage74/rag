package com.balex.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "rag.expansion")
public record RagExpansionProperties(
        @DefaultValue("0.0") double temperature,
        @DefaultValue("1") int topK,
        @DefaultValue("0.1") double topP,
        @DefaultValue("1.0") double repeatPenalty
) {}
