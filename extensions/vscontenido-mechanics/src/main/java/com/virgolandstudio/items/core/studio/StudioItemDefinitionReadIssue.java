package com.virgolandstudio.items.core.studio;

import java.util.Objects;

/** A malformed persisted definition kept visible to migration tooling instead of being silently discarded. */
public record StudioItemDefinitionReadIssue(int index, String message) {
    public StudioItemDefinitionReadIssue {
        if (index < 0) throw new IllegalArgumentException("index must be >= 0");
        message = Objects.requireNonNull(message, "message").trim();
        if (message.isEmpty()) throw new IllegalArgumentException("message must not be blank");
    }
}
