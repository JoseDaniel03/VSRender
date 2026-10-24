package com.virgolandstudio.items.core.state;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.api.definition.ItemDefinition;
import com.virgolandstudio.items.api.state.StateFieldDefinition;
import com.virgolandstudio.items.api.state.StateType;
import com.virgolandstudio.items.core.registry.BuiltinMaintenanceSchemas;

import java.util.List;
import java.util.Map;

/** Adds persistent state slots for opt-in maintenance/degradation components. */
public final class MaintenanceStateDefinitions {
    private static final NamespacedId WEAR = NamespacedId.parse("virgoland:wear");
    private static final NamespacedId MAINTENANCE = NamespacedId.parse("virgoland:maintenance");

    private MaintenanceStateDefinitions() {}

    public static void contribute(ItemDefinition definition, List<StateFieldDefinition> states) {
        for (ComponentConfig component : definition.components()) {
            if (component.type().equals(WEAR)) {
                addIfMissing(states, "wear", StateType.DOUBLE, nestedDouble(component, "initial", 0.0), true);
            } else if (component.type().equals(BuiltinMaintenanceSchemas.DIRT)) {
                addIfMissing(states, "dirt", StateType.DOUBLE, directDouble(component, "initial", 0.0), true);
            } else if (component.type().equals(BuiltinMaintenanceSchemas.RUST)) {
                addIfMissing(states, "rust", StateType.DOUBLE, directDouble(component, "initial", 0.0), true);
            } else if (component.type().equals(BuiltinMaintenanceSchemas.LUBRICATION)) {
                addIfMissing(states, "lubrication", StateType.DOUBLE, directDouble(component, "initial", 100.0), true);
            } else if (component.type().equals(BuiltinMaintenanceSchemas.MECHANICAL_FAILURE)) {
                addIfMissing(states, "mechanical_jammed", StateType.BOOLEAN, false, true);
                addIfMissing(states, "mechanical_failure_count", StateType.INTEGER, 0L, true);
            } else if (component.type().equals(MAINTENANCE)) {
                addIfMissing(states, "maintenance_count", StateType.INTEGER, 0L, true);
                addIfMissing(states, "last_maintenance_ms", StateType.INTEGER, 0L, true);
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
        if (states.stream().anyMatch(existing -> existing.key().equals(key))) return;
        states.add(new StateFieldDefinition(key, type, defaultValue, persistent));
    }

    private static double directDouble(ComponentConfig component, String key, double fallback) {
        return number(component.values().get(key), fallback);
    }

    private static double nestedDouble(ComponentConfig component, String key, double fallback) {
        Object rawConfig = component.values().get("config");
        if (rawConfig instanceof Map<?, ?> map) return number(map.get(key), fallback);
        return number(component.values().get(key), fallback);
    }

    private static double number(Object raw, double fallback) {
        if (raw instanceof Number number) return number.doubleValue();
        if (raw == null) return fallback;
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
