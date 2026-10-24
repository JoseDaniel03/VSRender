package com.virgolandstudio.items.core.migration;

import com.virgolandstudio.items.api.definition.v2.ItemVisualContext;
import com.virgolandstudio.items.api.definition.v2.ItemVisualReference;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Non-destructive migration plan. Unresolved bindings remain visible instead of being guessed. */
public record LegacyVisualMigrationPlan(
        ItemVisualReference defaultVisual,
        Map<ItemVisualContext, ItemVisualReference> overrides,
        List<LegacyVisualBindingIssue> unresolved
) {
    public LegacyVisualMigrationPlan {
        var normalized = new EnumMap<ItemVisualContext, ItemVisualReference>(ItemVisualContext.class);
        if (overrides != null) normalized.putAll(overrides);
        overrides = Map.copyOf(normalized);
        unresolved = unresolved == null ? List.of() : List.copyOf(unresolved);
    }

    public boolean requiresInput() {
        return !unresolved.isEmpty();
    }
}
