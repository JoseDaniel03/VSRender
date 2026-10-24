package com.virgolandstudio.items.core.runtime;

import com.virgolandstudio.items.api.NamespacedId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Immutable provider-neutral view of one live item instance. */
public record ItemRuntimeSnapshot(
        UUID instanceId,
        NamespacedId definitionId,
        Map<String, Object> state
) {
    public ItemRuntimeSnapshot {
        state = Map.copyOf(new LinkedHashMap<>(state));
    }
}
