package com.virgolandstudio.items.api.behavior;

import java.util.Map;
import java.util.Objects;

public record ValueConfig(String type, Map<String, Object> values) {
    public ValueConfig {
        Objects.requireNonNull(type, "type");
        values = values == null ? Map.of() : Map.copyOf(values);
    }

    public static ValueConfig constant(Object value) {
        return new ValueConfig("constant", Map.of("value", value));
    }

    public static ValueConfig stat(String key, double fallback) {
        return new ValueConfig("stat", Map.of("key", key, "fallback", fallback));
    }

    public static ValueConfig variable(String key) {
        return new ValueConfig("variable", Map.of("key", key));
    }

    public static ValueConfig state(String key) {
        return new ValueConfig("state", Map.of("key", key));
    }

    public static ValueConfig curve(Object curve, ValueConfig input) {
        return new ValueConfig("curve", Map.of("curve", curve, "input", input));
    }
}
