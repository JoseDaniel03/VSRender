package com.virgolandstudio.items.api.behavior;

import java.util.Map;
import java.util.Objects;

public record ActionConfig(String type, Map<String, Object> values) {
    public ActionConfig {
        Objects.requireNonNull(type, "type");
        values = values == null ? Map.of() : Map.copyOf(values);
    }
}
