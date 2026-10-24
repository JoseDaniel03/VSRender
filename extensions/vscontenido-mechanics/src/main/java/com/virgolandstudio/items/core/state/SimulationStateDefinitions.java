package com.virgolandstudio.items.core.state;

import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.api.definition.ItemDefinition;
import com.virgolandstudio.items.api.state.StateFieldDefinition;
import com.virgolandstudio.items.api.state.StateType;
import com.virgolandstudio.items.core.registry.BuiltinSimulationSchemas;

import java.util.List;

/**
 * Adds conventional persistent state slots required by generic simulator components.
 * Explicit state declarations always win, so existing authored definitions remain
 * source-compatible and can override these conventions when needed.
 */
public final class SimulationStateDefinitions {
    private SimulationStateDefinitions() {}

    public static void contribute(ItemDefinition definition, List<StateFieldDefinition> states) {
        for (ComponentConfig component : definition.components()) {
            if (component.type().equals(BuiltinSimulationSchemas.STATE)) {
                addIfMissing(states,
                        stringValue(component, "key", "state"),
                        StateType.STRING,
                        stringValue(component, "initial", "default"),
                        boolValue(component, "persistent", true));
            } else if (component.type().equals(BuiltinSimulationSchemas.COUNTER)) {
                addIfMissing(states,
                        stringValue(component, "key", "count"),
                        StateType.INTEGER,
                        longValue(component, "initial", 0L),
                        true);
            } else if (component.type().equals(BuiltinSimulationSchemas.USES)) {
                addIfMissing(states, "uses_remaining", StateType.INTEGER,
                        longValue(component, "initial", longValue(component, "capacity", 1L)), true);
            } else if (component.type().equals(BuiltinSimulationSchemas.DURABILITY)) {
                addIfMissing(states, "durability", StateType.DOUBLE,
                        doubleValue(component, "initial", doubleValue(component, "max", 100.0)), true);
            } else if (component.type().equals(BuiltinSimulationSchemas.FLUID_STORAGE)) {
                addIfMissing(states, "fluid_type", StateType.STRING,
                        stringValue(component, "fluid", ""), true);
                addIfMissing(states, "fluid_amount", StateType.DOUBLE,
                        doubleValue(component, "initialAmount", 0.0), true);
            } else if (component.type().equals(BuiltinSimulationSchemas.TEMPERATURE)) {
                addIfMissing(states, "temperature_c", StateType.DOUBLE,
                        doubleValue(component, "initialC", 20.0), true);
            } else if (component.type().equals(BuiltinSimulationSchemas.TIMED_PROCESS)) {
                boolean persistent = boolValue(component, "persistAcrossRestart", true);
                addIfMissing(states, "process_active", StateType.BOOLEAN, false, persistent);
                addIfMissing(states, "process_id", StateType.STRING, "", persistent);
                addIfMissing(states, "process_started_ms", StateType.INTEGER, 0L, persistent);
                addIfMissing(states, "process_deadline_ms", StateType.INTEGER, 0L, persistent);
                addIfMissing(states, "process_duration_ms", StateType.INTEGER, 0L, persistent);
            } else if (component.type().equals(BuiltinSimulationSchemas.SMOKEABLE)) {
                addIfMissing(states, "smoke_lit", StateType.BOOLEAN, false, true);
                addIfMissing(states, "smoke_uses_remaining", StateType.INTEGER,
                        longValue(component, "uses", 8L), true);
            } else if (component.type().equals(BuiltinSimulationSchemas.FILM)) {
                addIfMissing(states, "film_exposed", StateType.INTEGER,
                        longValue(component, "exposed", 0L), true);
                addIfMissing(states, "film_developed", StateType.BOOLEAN,
                        boolValue(component, "developed", false), true);
            } else if (component.type().equals(BuiltinSimulationSchemas.TICKET)
                    && boolValue(component, "singleUse", true)) {
                addIfMissing(states, "ticket_consumed", StateType.BOOLEAN, false, true);
            }
        }
    }

    private static void addIfMissing(
            List<StateFieldDefinition> states,
            String key,
            StateType type,
            Object defaultValue,
            boolean persistent
    ) {
        if (key == null || key.isBlank()) return;
        if (states.stream().anyMatch(existing -> existing.key().equals(key))) return;
        states.add(new StateFieldDefinition(key, type, defaultValue, persistent));
    }

    private static Object value(ComponentConfig component, String key) {
        return component.values().get(key);
    }

    private static String stringValue(ComponentConfig component, String key, String fallback) {
        Object value = value(component, key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static boolean boolValue(ComponentConfig component, String key, boolean fallback) {
        Object value = value(component, key);
        if (value == null) return fallback;
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static long longValue(ComponentConfig component, String key, long fallback) {
        Object value = value(component, key);
        if (value == null) return fallback;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double doubleValue(ComponentConfig component, String key, double fallback) {
        Object value = value(component, key);
        if (value == null) return fallback;
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
