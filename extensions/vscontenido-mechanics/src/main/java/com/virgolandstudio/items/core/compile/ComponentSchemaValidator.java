package com.virgolandstudio.items.core.compile;

import com.virgolandstudio.items.api.schema.ComponentSchema;
import com.virgolandstudio.items.api.schema.FieldSchema;
import com.virgolandstudio.items.api.schema.FieldType;
import com.virgolandstudio.items.api.schema.UiControl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provider-neutral validation for schema-backed item component configuration.
 *
 * <p>The validator deliberately ignores unknown keys for now. Several established
 * runtime components still accept compatibility/advanced values that pre-date the
 * public Studio schema. Known schema fields are validated without changing that
 * legacy behavior.</p>
 */
public final class ComponentSchemaValidator {
    private ComponentSchemaValidator() {}

    public static List<String> validate(ComponentSchema schema, Map<String, Object> values) {
        List<String> errors = new ArrayList<>();
        Map<String, Object> safeValues = values == null ? Map.of() : values;

        for (FieldSchema field : schema.fields().values()) {
            if (!safeValues.containsKey(field.key())) continue;
            Object value = safeValues.get(field.key());
            if (value == null) continue;

            String prefix = schema.id() + "." + field.key();
            validateType(field, value, prefix, errors);
            validateRange(field, value, prefix, errors);
            validateOptions(field, value, prefix, errors);
        }
        return List.copyOf(errors);
    }

    private static void validateType(FieldSchema field, Object value, String prefix, List<String> errors) {
        if (field.control() == UiControl.MULTI_SELECT) {
            if (!(value instanceof Iterable<?>)) {
                errors.add(prefix + " must be a list/set for MULTI_SELECT");
            }
            return;
        }

        FieldType type = field.type();
        boolean valid = switch (type) {
            case BOOLEAN -> value instanceof Boolean || isBooleanString(value);
            case INTEGER -> isInteger(value);
            case NUMBER, PERCENTAGE, DURATION, DISTANCE, ANGLE, VELOCITY -> isNumber(value);
            case STRING, TEXT, ENUM, MINECRAFT_MATERIAL,
                    ITEM_REFERENCE, MODIFIER_REFERENCE, MODEL_REFERENCE,
                    ANIMATION_REFERENCE, SOUND_REFERENCE -> value instanceof CharSequence;
            case TAGS, BLOCK_SET, ENTITY_SET, LIST -> value instanceof Iterable<?>;
            case MAP -> value instanceof Map<?, ?>;
            case RANGE, VECTOR -> value instanceof Iterable<?> || value instanceof Map<?, ?>;
            // These established field kinds also accept typed API records/classes
            // (ReloadSequenceDefinition, curves, rule sets, etc.). Their dedicated
            // compilers remain authoritative until each contract gets a neutral DTO.
            case MATERIAL_RULES, MATERIAL_GROUP_RULES, RELOAD_SEQUENCE,
                    CURVE, CONDITIONS, ACTIONS -> true;
        };

        if (!valid) errors.add(prefix + " has invalid value type for " + type);
    }

    private static void validateRange(FieldSchema field, Object value, String prefix, List<String> errors) {
        if (field.min() == null && field.max() == null) return;

        if (field.control() == UiControl.MULTI_SELECT || value instanceof Iterable<?>) return;
        Double numeric = asDouble(value);
        if (numeric == null) return;

        if (field.min() != null && numeric < field.min()) {
            errors.add(prefix + " must be >= " + field.min());
        }
        if (field.max() != null && numeric > field.max()) {
            errors.add(prefix + " must be <= " + field.max());
        }
    }

    private static void validateOptions(FieldSchema field, Object value, String prefix, List<String> errors) {
        if (field.options().isEmpty()) return;

        if (field.control() == UiControl.MULTI_SELECT) {
            if (!(value instanceof Iterable<?> iterable)) return;
            for (Object item : iterable) validateOption(field, item, prefix, errors);
            return;
        }
        if (field.type() == FieldType.ENUM) validateOption(field, value, prefix, errors);
    }

    private static void validateOption(FieldSchema field, Object value, String prefix, List<String> errors) {
        if (value == null) return;
        String text = String.valueOf(value);
        if (!field.options().contains(text)) {
            errors.add(prefix + " must be one of " + field.options());
        }
    }

    private static boolean isBooleanString(Object value) {
        if (!(value instanceof CharSequence sequence)) return false;
        String text = sequence.toString().trim();
        return text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false");
    }

    private static boolean isInteger(Object value) {
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) return true;
        if (value instanceof Number number) return Math.rint(number.doubleValue()) == number.doubleValue();
        try {
            Long.parseLong(String.valueOf(value).trim());
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isNumber(Object value) {
        return asDouble(value) != null;
    }

    private static Double asDouble(Object value) {
        if (value instanceof Number number) {
            double numberValue = number.doubleValue();
            return Double.isFinite(numberValue) ? numberValue : null;
        }
        try {
            double numberValue = Double.parseDouble(String.valueOf(value).trim());
            return Double.isFinite(numberValue) ? numberValue : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
