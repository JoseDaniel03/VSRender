package com.virgolandstudio.items.api.definition.v2;

import com.virgolandstudio.items.api.component.ComponentConfig;

import java.util.List;
import java.util.Map;

/**
 * Component configuration owned by Items.
 * compatibilityBaseStats preserves the current v1 root stats during migration and is not a new gameplay concept.
 */
public record ItemComponentSet(
        List<ComponentConfig> configs,
        Map<String, Double> compatibilityBaseStats
) {
    public ItemComponentSet {
        configs = configs == null ? List.of() : List.copyOf(configs);
        compatibilityBaseStats = compatibilityBaseStats == null ? Map.of() : Map.copyOf(compatibilityBaseStats);
    }
}
