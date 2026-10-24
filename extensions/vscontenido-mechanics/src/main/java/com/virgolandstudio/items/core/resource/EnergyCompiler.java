package com.virgolandstudio.items.core.resource;

import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.api.definition.ItemDefinition;
import com.virgolandstudio.items.api.state.StateType;
import com.virgolandstudio.items.core.compile.ValidationException;
import com.virgolandstudio.items.core.registry.BuiltinSchemas;
import com.virgolandstudio.items.core.state.StateLayout;

import java.util.ArrayList;
import java.util.Map;

public final class EnergyCompiler {
    public CompiledEnergy compile(ItemDefinition definition, StateLayout stateLayout) {
        ComponentConfig energy = definition.components().stream()
                .filter(component -> component.type().equals(BuiltinSchemas.ENERGY))
                .findFirst().orElse(null);
        if (energy == null) return null;

        var errors = new ArrayList<String>();
        String stateKey = string(energy.values(), "stateKey", "energy");
        double capacity = number(energy.values(), "capacity", 100.0);
        double regenPerSecond = number(energy.values(), "regenPerSecond", 0.0);
        int regenDelayTicks = integer(energy.values(), "regenDelayTicks", 0);
        var slot = stateLayout.find(stateKey);
        if (slot == null) {
            errors.add("Energy.stateKey references missing state field: " + stateKey);
        } else if (slot.type() != StateType.INTEGER && slot.type() != StateType.DOUBLE) {
            errors.add("Energy.stateKey must reference INTEGER or DOUBLE state: " + stateKey);
        } else if (slot.defaultValue() instanceof Number number
                && (number.doubleValue() < 0.0 || number.doubleValue() > capacity)) {
            errors.add("Energy default state must be within [0, capacity]: " + stateKey);
        }
        if (!Double.isFinite(capacity) || capacity <= 0.0) errors.add("Energy.capacity must be finite and > 0");
        if (!Double.isFinite(regenPerSecond) || regenPerSecond < 0.0) errors.add("Energy.regenPerSecond must be finite and >= 0");
        if (regenDelayTicks < 0) errors.add("Energy.regenDelayTicks must be >= 0");
        if (!errors.isEmpty()) throw new ValidationException(errors);
        return new CompiledEnergy(slot, capacity, regenPerSecond, regenDelayTicks);
    }

    private static String string(Map<String, Object> values, String key, String fallback) {
        Object raw = values.get(key);
        return raw instanceof String value && !value.isBlank() ? value : fallback;
    }

    private static double number(Map<String, Object> values, String key, double fallback) {
        Object raw = values.get(key);
        return raw instanceof Number number ? number.doubleValue() : fallback;
    }

    private static int integer(Map<String, Object> values, String key, int fallback) {
        Object raw = values.get(key);
        return raw instanceof Number number ? number.intValue() : fallback;
    }
}
