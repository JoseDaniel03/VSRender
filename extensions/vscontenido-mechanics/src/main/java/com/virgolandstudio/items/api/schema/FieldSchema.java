package com.virgolandstudio.items.api.schema;

import java.util.List;
import java.util.Objects;

public record FieldSchema(
        String key,
        String label,
        FieldType type,
        Object defaultValue,
        Double min,
        Double max,
        Double step,
        String unit,
        UiControl control,
        String group,
        List<String> options
) {
    public FieldSchema {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(type, "type");
        control = control == null ? UiControl.AUTO : control;
        options = options == null ? List.of() : List.copyOf(options);
    }

    public static FieldSchema number(String key, String label, double defaultValue, double min, double max, double step, String unit, UiControl control, String group) {
        return new FieldSchema(key, label, FieldType.NUMBER, defaultValue, min, max, step, unit, control, group, List.of());
    }
}
