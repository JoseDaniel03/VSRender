package com.virgolandstudio.items.core.validation;

import java.util.Objects;

/** Structured v2 authoring/migration issue suitable for Studio diagnostics. */
public record ItemDefinitionV2Issue(
        ItemDefinitionV2IssueSeverity severity,
        String path,
        String message
) {
    public ItemDefinitionV2Issue {
        Objects.requireNonNull(severity, "severity");
        path = requireText(path, "path");
        message = requireText(message, "message");
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return normalized;
    }
}
