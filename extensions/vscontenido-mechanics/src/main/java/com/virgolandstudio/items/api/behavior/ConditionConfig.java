package com.virgolandstudio.items.api.behavior;

import java.util.Map;
import java.util.Objects;

public record ConditionConfig(String type, Map<String, Object> values) {
    public ConditionConfig {
        Objects.requireNonNull(type, "type");
        values = values == null ? Map.of() : Map.copyOf(values);
    }
}
