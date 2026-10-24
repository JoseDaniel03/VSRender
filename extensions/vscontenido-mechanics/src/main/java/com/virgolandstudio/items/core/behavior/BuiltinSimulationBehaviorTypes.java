package com.virgolandstudio.items.core.behavior;

import com.virgolandstudio.items.api.behavior.ValueConfig;
import com.virgolandstudio.items.api.state.StateType;
import com.virgolandstudio.items.core.state.StateSlot;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small generic behavior vocabulary for simulator state.
 *
 * <p>These primitives intentionally operate only on ItemInstance state. Platform
 * effects and cross-module work stay behind bridges/integrations. BehaviorEngine
 * serializes a complete condition/action execution on the instance monitor, so a
 * state_available -> consume_state sequence cannot race another execution of the
 * same instance.</p>
 */
public final class BuiltinSimulationBehaviorTypes {
    private BuiltinSimulationBehaviorTypes() {}

    public static void registerAll(BehaviorCompiler compiler) {
        compiler.registerCondition("state_available", (config, c) -> {
            String key = string(config.values(), "key");
            CompiledValue amount = c.compileValue(valueConfig(config.values().getOrDefault("amount", 1.0)));
            return context -> {
                StateSlot slot = context.requireInstance().state().layout().require(key);
                if (!numeric(slot)) return false;
                double required = amount.resolveDouble(context, 1.0);
                if (!Double.isFinite(required) || required < 0.0) return false;
                double current = number(context.requireInstance().state().get(slot));
                context.put("state.available.key", key);
                context.put("state.available.current", current);
                context.put("state.available.required", required);
                return current + 1.0e-9 >= required;
            };
        });

        compiler.registerCondition("state_between", (config, c) -> {
            String key = string(config.values(), "key");
            CompiledValue min = c.compileValue(valueConfig(config.values().getOrDefault("min", Double.NEGATIVE_INFINITY)));
            CompiledValue max = c.compileValue(valueConfig(config.values().getOrDefault("max", Double.POSITIVE_INFINITY)));
            return context -> {
                StateSlot slot = context.requireInstance().state().layout().require(key);
                if (!numeric(slot)) return false;
                double value = number(context.requireInstance().state().get(slot));
                double lower = min.resolveDouble(context, Double.NEGATIVE_INFINITY);
                double upper = max.resolveDouble(context, Double.POSITIVE_INFINITY);
                return value >= lower && value <= upper;
            };
        });

        compiler.registerAction("consume_state", (config, c) -> {
            String key = string(config.values(), "key");
            CompiledValue amount = c.compileValue(valueConfig(config.values().getOrDefault("amount", 1.0)));
            boolean cancelIfInsufficient = bool(config.values(), "cancelIfInsufficient", true);
            return context -> {
                StateSlot slot = context.requireInstance().state().layout().require(key);
                if (!numeric(slot)) throw new IllegalArgumentException("consume_state requires numeric state: " + key);
                double requested = amount.resolveDouble(context, 1.0);
                if (!Double.isFinite(requested) || requested < 0.0) {
                    throw new IllegalArgumentException("consume_state amount must be finite and >= 0");
                }
                if (slot.type() == StateType.INTEGER && !whole(requested)) {
                    throw new IllegalArgumentException("consume_state amount must be whole for INTEGER state: " + key);
                }
                double current = number(context.requireInstance().state().get(slot));
                context.put("state.consume.key", key);
                context.put("state.consume.requested", requested);
                if (current + 1.0e-9 < requested) {
                    context.put("state.consume.accepted", false);
                    context.put("state.consume.remaining", current);
                    return cancelIfInsufficient ? ActionResult.CANCEL_TRIGGER : ActionResult.CONTINUE;
                }
                setNumber(context, slot, current - requested);
                context.put("state.consume.accepted", true);
                context.put("state.consume.remaining", number(context.requireInstance().state().get(slot)));
                return ActionResult.CONTINUE;
            };
        });

        compiler.registerAction("toggle_state", (config, c) -> {
            String key = string(config.values(), "key");
            return context -> {
                StateSlot slot = context.requireInstance().state().layout().require(key);
                if (slot.type() != StateType.BOOLEAN) {
                    throw new IllegalArgumentException("toggle_state requires BOOLEAN state: " + key);
                }
                boolean next = !context.requireInstance().state().getBoolean(slot, false);
                context.requireInstance().state().set(slot, next);
                context.put("state.toggle.key", key);
                context.put("state.toggle.value", next);
                return ActionResult.CONTINUE;
            };
        });

        compiler.registerAction("clamp_state", (config, c) -> {
            String key = string(config.values(), "key");
            CompiledValue min = c.compileValue(valueConfig(config.values().getOrDefault("min", Double.NEGATIVE_INFINITY)));
            CompiledValue max = c.compileValue(valueConfig(config.values().getOrDefault("max", Double.POSITIVE_INFINITY)));
            return context -> {
                StateSlot slot = context.requireInstance().state().layout().require(key);
                if (!numeric(slot)) throw new IllegalArgumentException("clamp_state requires numeric state: " + key);
                double lower = min.resolveDouble(context, Double.NEGATIVE_INFINITY);
                double upper = max.resolveDouble(context, Double.POSITIVE_INFINITY);
                if (lower > upper) throw new IllegalArgumentException("clamp_state min cannot exceed max");
                double previous = number(context.requireInstance().state().get(slot));
                double next = Math.max(lower, Math.min(upper, previous));
                if (slot.type() == StateType.INTEGER) next = Math.rint(next);
                setNumber(context, slot, next);
                context.put("state.clamp.key", key);
                context.put("state.clamp.previous", previous);
                context.put("state.clamp.value", next);
                return ActionResult.CONTINUE;
            };
        });

        BuiltinConsumableBehaviorTypes.registerAll(compiler);

        // Maintenance is simulator-owned too: platform adapters only choose which trigger executes it.
        BuiltinMaintenanceBehaviorTypes.registerAll(compiler);
    }

    private static void setNumber(com.virgolandstudio.items.core.runtime.ExecutionContext context, StateSlot slot, double value) {
        if (slot.type() == StateType.INTEGER) context.requireInstance().state().set(slot, (long) value);
        else context.requireInstance().state().set(slot, value);
    }

    private static boolean numeric(StateSlot slot) {
        return slot.type() == StateType.INTEGER || slot.type() == StateType.DOUBLE;
    }

    private static double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private static boolean whole(double value) {
        return Math.rint(value) == value;
    }

    private static String string(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null || String.valueOf(value).isBlank()) throw new IllegalArgumentException(key + " is required");
        return String.valueOf(value);
    }

    private static boolean bool(Map<String, Object> values, String key, boolean fallback) {
        Object value = values.get(key);
        if (value == null) return fallback;
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static ValueConfig valueConfig(Object raw) {
        if (raw instanceof ValueConfig config) return config;
        if (raw instanceof Map<?, ?> map && map.get("type") != null) {
            var values = new LinkedHashMap<String, Object>();
            Object nested = map.get("values");
            if (nested instanceof Map<?, ?> nestedMap) {
                for (var entry : nestedMap.entrySet()) if (entry.getKey() != null) values.put(String.valueOf(entry.getKey()), entry.getValue());
            } else {
                for (var entry : map.entrySet()) {
                    if (entry.getKey() != null && !"type".equals(String.valueOf(entry.getKey()))) {
                        values.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
            }
            return new ValueConfig(String.valueOf(map.get("type")), values);
        }
        return ValueConfig.constant(raw);
    }
}
