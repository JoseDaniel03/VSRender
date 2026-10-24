package com.virgolandstudio.items.core.migration;

import com.virgolandstudio.items.api.definition.v2.ItemVisualContext;
import com.virgolandstudio.items.api.definition.v2.ItemVisualReference;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Maps legacy item visual contexts only when semantics are preserved exactly. HELD is intentionally unresolved. */
public final class LegacyItemVisualBindingMigration {
    private LegacyItemVisualBindingMigration() {}

    public static LegacyVisualMigrationPlan plan(Map<String, ItemVisualReference> legacyBindings) {
        var overrides = new EnumMap<ItemVisualContext, ItemVisualReference>(ItemVisualContext.class);
        var unresolved = new ArrayList<LegacyVisualBindingIssue>();
        ItemVisualReference defaultVisual = null;
        if (legacyBindings == null) return new LegacyVisualMigrationPlan(null, overrides, unresolved);

        for (var entry : legacyBindings.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            String context = entry.getKey().trim().toUpperCase(Locale.ROOT);
            ItemVisualReference reference = entry.getValue();
            switch (context) {
                case "DEFAULT" -> defaultVisual = uniqueDefault(defaultVisual, reference);
                case "INVENTORY" -> putUnique(overrides, ItemVisualContext.INVENTORY, reference);
                case "WORLD" -> putUnique(overrides, ItemVisualContext.WORLD, reference);
                case "EQUIPPED" -> putUnique(overrides, ItemVisualContext.EQUIPPED, reference);
                case "HELD" -> unresolved.add(new LegacyVisualBindingIssue(
                        "HELD",
                        reference,
                        "Legacy HELD cannot distinguish FIRST_PERSON from THIRD_PERSON; choose the target context explicitly"
                ));
                default -> unresolved.add(new LegacyVisualBindingIssue(
                        context.isEmpty() ? "<blank>" : context,
                        reference,
                        "Legacy context has no lossless Item v2 mapping"
                ));
            }
        }

        return new LegacyVisualMigrationPlan(defaultVisual, overrides, unresolved);
    }

    private static ItemVisualReference uniqueDefault(ItemVisualReference existing, ItemVisualReference incoming) {
        if (existing != null && !existing.equals(incoming)) {
            throw new IllegalArgumentException("Conflicting DEFAULT legacy visual bindings");
        }
        return incoming;
    }

    private static void putUnique(
            Map<ItemVisualContext, ItemVisualReference> target,
            ItemVisualContext context,
            ItemVisualReference reference
    ) {
        ItemVisualReference existing = target.putIfAbsent(context, reference);
        if (existing != null && !existing.equals(reference)) {
            throw new IllegalArgumentException("Conflicting legacy visual binding for " + context);
        }
    }
}
