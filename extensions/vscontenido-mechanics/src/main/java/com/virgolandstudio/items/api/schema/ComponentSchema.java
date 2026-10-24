package com.virgolandstudio.items.api.schema;

import com.virgolandstudio.items.api.NamespacedId;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record ComponentSchema(
        int schemaVersion,
        NamespacedId id,
        String name,
        String description,
        String category,
        RuntimeMode runtimeMode,
        int tickInterval,
        RuntimeCost cost,
        Set<NamespacedId> requires,
        Set<NamespacedId> optional,
        Set<NamespacedId> conflicts,
        Map<String, FieldSchema> fields,
        List<String> triggers
) {
    public ComponentSchema {
        if (schemaVersion < 1) throw new IllegalArgumentException("schemaVersion must be >= 1");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        runtimeMode = runtimeMode == null ? RuntimeMode.EVENT : runtimeMode;
        cost = cost == null ? RuntimeCost.LOW : cost;
        if (tickInterval < 1) tickInterval = 1;
        requires = requires == null ? Set.of() : Set.copyOf(requires);
        optional = optional == null ? Set.of() : Set.copyOf(optional);
        conflicts = conflicts == null ? Set.of() : Set.copyOf(conflicts);
        fields = fields == null ? Map.of() : Map.copyOf(fields);
        triggers = triggers == null ? List.of() : List.copyOf(triggers);
    }
}
