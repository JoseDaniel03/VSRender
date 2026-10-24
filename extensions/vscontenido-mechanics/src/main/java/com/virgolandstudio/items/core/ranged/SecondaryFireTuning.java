package com.virgolandstudio.items.core.ranged;

import com.virgolandstudio.items.api.definition.ItemDefinition;
import com.virgolandstudio.items.core.registry.BuiltinSchemas;

/**
 * Authored handling modifiers that apply only while the secondary fire profile is active.
 * Cadence stays in {@link CompiledFireControl}; these values shape the accepted shot itself.
 */
public record SecondaryFireTuning(double spreadMultiplier, double recoilMultiplier) {
    public static final SecondaryFireTuning DEFAULT = new SecondaryFireTuning(1.0, 1.0);

    public SecondaryFireTuning {
        spreadMultiplier = sanitize(spreadMultiplier);
        recoilMultiplier = sanitize(recoilMultiplier);
    }

    public static SecondaryFireTuning from(ItemDefinition definition) {
        if (definition == null) return DEFAULT;
        for (var component : definition.components()) {
            if (!BuiltinSchemas.FIRE_CONTROL.equals(component.type())) continue;
            return new SecondaryFireTuning(
                    number(component.values().get("secondarySpreadMultiplier"), 1.0),
                    number(component.values().get("secondaryRecoilMultiplier"), 1.0)
            );
        }
        return DEFAULT;
    }

    private static double number(Object raw, double fallback) {
        if (!(raw instanceof Number number)) return fallback;
        double value = number.doubleValue();
        return Double.isFinite(value) ? value : fallback;
    }

    private static double sanitize(double value) {
        return Double.isFinite(value) && value >= 0.0 ? value : 1.0;
    }
}
