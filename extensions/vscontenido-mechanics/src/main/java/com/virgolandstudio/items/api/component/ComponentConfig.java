package com.virgolandstudio.items.api.component;

import com.virgolandstudio.items.api.NamespacedId;

import java.util.Map;
import java.util.Objects;

public record ComponentConfig(NamespacedId type, Map<String, Object> values) {
    public ComponentConfig {
        Objects.requireNonNull(type, "type");
        values = values == null ? Map.of() : Map.copyOf(values);
    }
}
