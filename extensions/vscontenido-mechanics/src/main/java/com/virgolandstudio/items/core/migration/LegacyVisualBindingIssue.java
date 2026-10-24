package com.virgolandstudio.items.core.migration;

import com.virgolandstudio.items.api.definition.v2.ItemVisualReference;

import java.util.Objects;

/** Legacy visual binding that cannot be migrated without an explicit creator/admin choice. */
public record LegacyVisualBindingIssue(
        String legacyContext,
        ItemVisualReference reference,
        String reason
) {
    public LegacyVisualBindingIssue {
        legacyContext = Objects.requireNonNull(legacyContext, "legacyContext").trim();
        if (legacyContext.isEmpty()) throw new IllegalArgumentException("legacyContext must not be blank");
        Objects.requireNonNull(reference, "reference");
        reason = Objects.requireNonNull(reason, "reason").trim();
        if (reason.isEmpty()) throw new IllegalArgumentException("reason must not be blank");
    }
}
