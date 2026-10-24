package com.virgolandstudio.items.core.accessory;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.accessory.AccessoryDefinition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registry of authored accessory definitions, independent from their inventory renderer. */
public final class AccessoryRegistry {
    private final Map<NamespacedId, AccessoryDefinition> byId = new LinkedHashMap<>();
    private final Map<NamespacedId, AccessoryDefinition> byItemDefinitionId = new LinkedHashMap<>();

    public void publish(AccessoryDefinition definition) {
        if (byId.containsKey(definition.id())) {
            throw new IllegalArgumentException("Duplicate accessory definition: " + definition.id());
        }
        if (byItemDefinitionId.containsKey(definition.itemDefinitionId())) {
            throw new IllegalArgumentException("Accessory item already mapped: " + definition.itemDefinitionId());
        }
        byId.put(definition.id(), definition);
        byItemDefinitionId.put(definition.itemDefinitionId(), definition);
    }

    public Optional<AccessoryDefinition> find(NamespacedId id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<AccessoryDefinition> findByItemDefinitionId(NamespacedId itemDefinitionId) {
        return Optional.ofNullable(byItemDefinitionId.get(itemDefinitionId));
    }

    public Collection<AccessoryDefinition> all() {
        return List.copyOf(byId.values());
    }
}
