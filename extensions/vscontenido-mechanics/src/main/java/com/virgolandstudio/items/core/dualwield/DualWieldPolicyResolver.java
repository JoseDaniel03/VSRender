package com.virgolandstudio.items.core.dualwield;

import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.api.definition.ItemDefinition;
import com.virgolandstudio.items.core.registry.BuiltinDualWieldSchemas;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Parses the schema-driven dual-wield component without coupling platform runtimes to Studio. */
public final class DualWieldPolicyResolver {
    private static final Set<String> DEFAULT_COMPATIBILITY_TAGS = Set.of("pistol");

    private DualWieldPolicyResolver() {}

    public static DualWieldPolicy resolve(ItemDefinition definition) {
        if (definition == null) return null;
        for (ComponentConfig component : definition.components()) {
            if (!BuiltinDualWieldSchemas.DUAL_WIELD.equals(component.type())) continue;
            return new DualWieldPolicy(
                    tags(component.values().get("compatibilityTags")),
                    enumValue(component.values().get("pairingMode"), DualWieldPolicy.PairingMode.class,
                            DualWieldPolicy.PairingMode.SAME_TAG),
                    enumValue(component.values().get("reloadPolicy"), DualWieldPolicy.ReloadPolicy.class,
                            DualWieldPolicy.ReloadPolicy.SEQUENTIAL),
                    number(component.values().get("spreadMultiplier"), 1.25),
                    number(component.values().get("recoilMultiplier"), 1.15)
            );
        }
        return null;
    }

    private static Set<String> tags(Object raw) {
        if (raw == null) return DEFAULT_COMPATIBILITY_TAGS;
        Set<String> values = new LinkedHashSet<>();
        if (raw instanceof Iterable<?> iterable) {
            for (Object value : iterable) addTag(values, value);
        } else {
            for (String value : String.valueOf(raw).split(",")) addTag(values, value);
        }
        return Set.copyOf(values);
    }

    private static void addTag(Set<String> target, Object raw) {
        if (raw == null) return;
        String value = String.valueOf(raw).trim();
        if (!value.isEmpty()) target.add(value);
    }

    private static double number(Object raw, double fallback) {
        if (!(raw instanceof Number number)) return fallback;
        double value = number.doubleValue();
        return Double.isFinite(value) && value > 0.0 ? value : fallback;
    }

    private static <E extends Enum<E>> E enumValue(Object raw, Class<E> type, E fallback) {
        if (raw == null) return fallback;
        try {
            return Enum.valueOf(type, String.valueOf(raw).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
