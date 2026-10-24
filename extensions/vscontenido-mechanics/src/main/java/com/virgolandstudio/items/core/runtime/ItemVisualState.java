package com.virgolandstudio.items.core.runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/** Provider-neutral visual-state resolution result for a live item instance. */
public record ItemVisualState(
        String sourceKey,
        Object sourceValue,
        String mappingKey,
        Map<String, Object> payload,
        boolean interpolate
) {
    public ItemVisualState {
        payload = Map.copyOf(new LinkedHashMap<>(payload));
    }
}
