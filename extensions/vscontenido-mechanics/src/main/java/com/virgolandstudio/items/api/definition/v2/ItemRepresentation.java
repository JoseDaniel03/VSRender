package com.virgolandstudio.items.api.definition.v2;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Visual representation of an Item, independent from gameplay components.
 * A missing custom visual intentionally falls through to the vanilla fallback.
 */
public record ItemRepresentation(
        ItemFallback fallback,
        ItemVisualReference defaultVisual,
        Map<ItemVisualContext, ItemVisualReference> overrides
) {
    public ItemRepresentation {
        Objects.requireNonNull(fallback, "fallback");
        var normalized = new EnumMap<ItemVisualContext, ItemVisualReference>(ItemVisualContext.class);
        if (overrides != null) {
            overrides.forEach((context, reference) -> {
                Objects.requireNonNull(context, "visual context");
                Objects.requireNonNull(reference, "visual reference");
                if (context == ItemVisualContext.DEFAULT) {
                    throw new IllegalArgumentException("DEFAULT must be expressed through defaultVisual");
                }
                normalized.put(context, reference);
            });
        }
        overrides = Map.copyOf(normalized);
    }

    public Optional<ItemVisualReference> visualFor(ItemVisualContext context) {
        Objects.requireNonNull(context, "context");
        if (context == ItemVisualContext.DEFAULT) return Optional.ofNullable(defaultVisual);
        return Optional.ofNullable(overrides.getOrDefault(context, defaultVisual));
    }

    public static ItemRepresentation vanillaOnly(String material) {
        return new ItemRepresentation(new ItemFallback(material), null, Map.of());
    }
}
