package com.virgolandstudio.items.core.stack;

import com.virgolandstudio.items.api.definition.v2.ItemStateSchema;
import com.virgolandstudio.items.api.definition.v2.StackMode;
import com.virgolandstudio.items.api.definition.v2.StackPolicy;
import com.virgolandstudio.items.api.state.StateFieldDefinition;
import com.virgolandstudio.items.api.state.StateType;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Pure v2 stack evaluator. It never delegates stateful merge decisions blindly to Minecraft. */
public final class StackPolicyEvaluator {
    public StackMergeDecision evaluate(
            StackPolicy policy,
            ItemStateSchema schema,
            Map<String, Object> leftState,
            Map<String, Object> rightState
    ) {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(schema, "schema");
        Map<String, Object> left = leftState == null ? Map.of() : leftState;
        Map<String, Object> right = rightState == null ? Map.of() : rightState;

        if (policy.mode() == StackMode.INSTANCE) {
            return StackMergeDecision.separate("INSTANCE policy keeps every object independent");
        }

        Map<String, StateFieldDefinition> fields = index(schema);
        Set<String> keys = new LinkedHashSet<>();
        if (policy.mode() == StackMode.STACKED) {
            keys.addAll(policy.mergeStateKeys());
            if (keys.isEmpty()) return StackMergeDecision.merge("STACKED policy has no state merge keys");
        } else {
            fields.keySet().forEach(keys::add);
            left.keySet().forEach(keys::add);
            right.keySet().forEach(keys::add);
        }

        for (String key : keys) {
            StateFieldDefinition field = fields.get(key);
            Object leftValue = value(left, key, field);
            Object rightValue = value(right, key, field);
            if (!valid(field, leftValue) || !valid(field, rightValue)) {
                return StackMergeDecision.separate("State key '" + key + "' has a value incompatible with its schema");
            }
            if (!Objects.equals(canonical(field, leftValue), canonical(field, rightValue))) {
                return StackMergeDecision.separate("State key '" + key + "' differs");
            }
        }

        return StackMergeDecision.merge(policy.mode() + " state is equivalent");
    }

    private static Map<String, StateFieldDefinition> index(ItemStateSchema schema) {
        var fields = new LinkedHashMap<String, StateFieldDefinition>();
        for (StateFieldDefinition field : schema.fields()) {
            if (fields.putIfAbsent(field.key(), field) != null) {
                throw new IllegalArgumentException("Duplicate state field: " + field.key());
            }
        }
        return fields;
    }

    private static Object value(Map<String, Object> state, String key, StateFieldDefinition field) {
        if (state.containsKey(key)) return state.get(key);
        return field == null ? null : field.defaultValue();
    }

    private static boolean valid(StateFieldDefinition field, Object value) {
        if (field == null || value == null) return true;
        return switch (field.type()) {
            case INTEGER -> value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long;
            case DOUBLE -> value instanceof Number;
            case BOOLEAN -> value instanceof Boolean;
            case STRING -> value instanceof String;
        };
    }

    private static Object canonical(StateFieldDefinition field, Object value) {
        if (field == null || value == null) return value;
        StateType type = field.type();
        return switch (type) {
            case INTEGER -> ((Number) value).longValue();
            case DOUBLE -> ((Number) value).doubleValue();
            case BOOLEAN, STRING -> value;
        };
    }
}
