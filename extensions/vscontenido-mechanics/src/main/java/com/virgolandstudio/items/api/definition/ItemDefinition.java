package com.virgolandstudio.items.api.definition;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.behavior.BehaviorDefinition;
import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.api.state.StateFieldDefinition;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ItemDefinition(
        int schemaVersion,
        NamespacedId id,
        String displayName,
        String baseMaterial,
        String family,
        Set<String> tags,
        Map<String, Double> baseStats,
        List<StateFieldDefinition> state,
        List<ComponentConfig> components,
        List<BehaviorDefinition> behaviors
) {
    public ItemDefinition {
        if (schemaVersion < 1) throw new IllegalArgumentException("schemaVersion must be >= 1");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(baseMaterial, "baseMaterial");
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        baseStats = baseStats == null ? Map.of() : Map.copyOf(baseStats);
        state = state == null ? List.of() : List.copyOf(state);
        components = components == null ? List.of() : List.copyOf(components);
        behaviors = behaviors == null ? List.of() : List.copyOf(behaviors);
    }
}
