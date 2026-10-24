package com.virgolandstudio.items.api.state;

import java.util.Objects;

public record StateFieldDefinition(
        String key,
        StateType type,
        Object defaultValue,
        boolean persistent
) {
    public StateFieldDefinition {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");
        if (key.isBlank()) throw new IllegalArgumentException("State key cannot be blank");
        validate(type, defaultValue);
    }

    private static void validate(StateType type, Object value) {
        if (value == null) return;
        boolean valid = switch (type) {
            case INTEGER -> value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long;
            case DOUBLE -> value instanceof Number;
            case BOOLEAN -> value instanceof Boolean;
            case STRING -> value instanceof String;
        };
        if (!valid) throw new IllegalArgumentException("Default value " + value + " does not match state type " + type);
    }
}
