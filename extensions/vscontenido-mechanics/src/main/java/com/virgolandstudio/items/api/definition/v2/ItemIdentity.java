package com.virgolandstudio.items.api.definition.v2;

import com.virgolandstudio.items.api.NamespacedId;

import java.util.Objects;
import java.util.Set;

/** Stable logical identity. Family is organizational metadata and must not drive gameplay. */
public record ItemIdentity(
        NamespacedId id,
        String displayName,
        String family,
        Set<String> tags
) {
    public ItemIdentity {
        Objects.requireNonNull(id, "id");
        displayName = requireText(displayName, "displayName");
        family = family == null || family.isBlank() ? "item" : family.trim();
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return normalized;
    }
}
