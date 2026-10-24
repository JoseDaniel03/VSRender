package com.virgolandstudio.items.api.accessory;

import com.virgolandstudio.items.api.NamespacedId;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Data-only definition of an installable accessory.
 *
 * The inventory item that represents the part is deliberately referenced by id
 * instead of embedded here. This lets the same attachment runtime stay UI and
 * Paper independent while the web can author accessories as normal data.
 */
public record AccessoryDefinition(
        int schemaVersion,
        NamespacedId id,
        NamespacedId itemDefinitionId,
        String displayName,
        String slot,
        Set<String> compatibleFamilies,
        Set<String> requiredWeaponTags,
        List<AccessoryModifierDefinition> modifiers,
        Map<String, Object> settings
) {
    public AccessoryDefinition {
        if (schemaVersion < 1) throw new IllegalArgumentException("schemaVersion must be >= 1");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(itemDefinitionId, "itemDefinitionId");
        Objects.requireNonNull(displayName, "displayName");
        if (displayName.isBlank()) throw new IllegalArgumentException("displayName cannot be blank");
        slot = normalizeSlot(slot);
        compatibleFamilies = normalizeSet(compatibleFamilies);
        requiredWeaponTags = normalizeSet(requiredWeaponTags);
        modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
        settings = settings == null ? Map.of() : Map.copyOf(settings);
    }

    /** Backwards-compatible constructor used by stage-5 definitions. */
    public AccessoryDefinition(
            int schemaVersion,
            NamespacedId id,
            NamespacedId itemDefinitionId,
            String displayName,
            String slot,
            Set<String> compatibleFamilies,
            Set<String> requiredWeaponTags,
            Map<String, Object> settings
    ) {
        this(schemaVersion, id, itemDefinitionId, displayName, slot,
                compatibleFamilies, requiredWeaponTags, List.of(), settings);
    }

    private static String normalizeSlot(String raw) {
        Objects.requireNonNull(raw, "slot");
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) throw new IllegalArgumentException("slot cannot be blank");
        return normalized;
    }

    private static Set<String> normalizeSet(Set<String> values) {
        if (values == null || values.isEmpty()) return Set.of();
        var normalized = new LinkedHashSet<String>();
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            normalized.add(value.trim().toLowerCase(Locale.ROOT));
        }
        return Set.copyOf(normalized);
    }
}
