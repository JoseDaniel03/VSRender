package com.virgolandstudio.items.core.registry;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.schema.ComponentSchema;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ComponentSchemaRegistry {
    private final Map<NamespacedId, ComponentSchema> schemas = new LinkedHashMap<>();

    public void register(ComponentSchema schema) {
        ComponentSchema enriched = BuiltinSchemaEnricher.enrich(schema);
        if (schemas.putIfAbsent(enriched.id(), enriched) != null) {
            throw new IllegalStateException("Component schema already registered: " + enriched.id());
        }
    }

    public Optional<ComponentSchema> find(NamespacedId id) {
        return Optional.ofNullable(schemas.get(id));
    }

    public Collection<ComponentSchema> all() {
        return List.copyOf(schemas.values());
    }
}
