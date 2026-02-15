package com.balex.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "rag.defaults")
public record RagDefaultsProperties(
        @DefaultValue("true") boolean onlyContext,
        @DefaultValue("2") int topK,
        @DefaultValue("0.7") double topP,
        @DefaultValue("0.3") double temperature,
        @DefaultValue("1.1") double repeatPenalty,
        @DefaultValue("2") int searchTopK,
        @DefaultValue("0.3") double similarityThreshold
) {}