package com.virgolandstudio.items.core.accessory;

import com.virgolandstudio.items.api.accessory.AccessoryModifierDefinition;
import com.virgolandstudio.items.api.accessory.AccessoryModifierOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Immutable resolved modifier list for one concrete weapon instance. */
public final class AccessoryModifierSet {
    private static final AccessoryModifierSet EMPTY = new AccessoryModifierSet(List.of());
    private final List<AccessoryModifierDefinition> modifiers;

    public AccessoryModifierSet(List<AccessoryModifierDefinition> modifiers) {
        this.modifiers = modifiers == null || modifiers.isEmpty() ? List.of() : List.copyOf(modifiers);
    }

    public static AccessoryModifierSet empty() { return EMPTY; }
    public boolean isEmpty() { return modifiers.isEmpty(); }
    public List<AccessoryModifierDefinition> all() { return modifiers; }

    /** Numeric resolution is deterministic: SET (last), then all ADDs, then all MULTIPLY operations. */
    public double number(String target, double baseValue) {
        String normalized = normalize(target);
        double seed = baseValue;
        Double set = null;
        double add = 0.0;
        double multiply = 1.0;
        for (AccessoryModifierDefinition modifier : modifiers) {
            if (!modifier.target().equals(normalized) || !(modifier.value() instanceof Number number)) continue;
            double value = number.doubleValue();
            if (!Double.isFinite(value)) continue;
            if (modifier.operation() == AccessoryModifierOperation.SET) set = value;
            else if (modifier.operation() == AccessoryModifierOperation.ADD) add += value;
            else if (modifier.operation() == AccessoryModifierOperation.MULTIPLY) multiply *= value;
        }
        if (set != null) seed = set;
        return (seed + add) * multiply;
    }

    public int integer(String target, int baseValue, int min, int max) {
        double resolved = number(target, baseValue);
        if (!Double.isFinite(resolved)) resolved = baseValue;
        long rounded = Math.round(resolved);
        if (rounded < min) return min;
        if (rounded > max) return max;
        return (int) rounded;
    }

    /** String targets support SET; the last authored SET wins. */
    public String string(String target, String baseValue) {
        String normalized = normalize(target);
        String value = baseValue;
        for (AccessoryModifierDefinition modifier : modifiers) {
            if (!modifier.target().equals(normalized)) continue;
            if (modifier.operation() == AccessoryModifierOperation.SET && modifier.value() instanceof String text && !text.isBlank()) {
                value = text;
            }
        }
        return value;
    }

    public List<AccessoryModifierDefinition> forTarget(String target) {
        String normalized = normalize(target);
        var result = new ArrayList<AccessoryModifierDefinition>();
        for (var modifier : modifiers) if (modifier.target().equals(normalized)) result.add(modifier);
        return List.copyOf(result);
    }

    private static String normalize(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
