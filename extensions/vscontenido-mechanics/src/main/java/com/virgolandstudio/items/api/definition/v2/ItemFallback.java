package com.virgolandstudio.items.api.definition.v2;

import java.util.Locale;
import java.util.Objects;

/** Vanilla recovery representation used when no custom visual can be resolved. */
public record ItemFallback(String material) {
    public ItemFallback {
        Objects.requireNonNull(material, "material");
        material = material.trim().toUpperCase(Locale.ROOT);
        if (material.isEmpty()) throw new IllegalArgumentException("material must not be blank");
    }
}
