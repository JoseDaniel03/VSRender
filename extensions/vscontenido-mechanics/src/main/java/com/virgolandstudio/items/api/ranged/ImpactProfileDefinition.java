package com.virgolandstudio.items.api.ranged;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Reusable authored impact profile. Item-local ImpactResponse values may override it at compile time. */
public record ImpactProfileDefinition(
        String id,
        double blockPenetrationPower,
        int maxBlockPenetrations,
        int maxRicochets,
        double defaultResistancePerBlock,
        double defaultVelocityRetention,
        boolean defaultPenetrable,
        boolean defaultRicochetEnabled,
        double defaultRicochetMinAngleDegrees,
        double defaultRicochetVelocityRetention,
        List<ImpactMaterialRule> rules,
        List<ImpactMaterialGroupRule> groupRules
) {
    public ImpactProfileDefinition {
        id = normalizeId(id);
        if (blockPenetrationPower < 0.0 || !Double.isFinite(blockPenetrationPower)) {
            throw new IllegalArgumentException("blockPenetrationPower must be finite and >= 0");
        }
        if (maxBlockPenetrations < 0 || maxBlockPenetrations > 16) {
            throw new IllegalArgumentException("maxBlockPenetrations must be between 0 and 16");
        }
        if (maxRicochets < 0 || maxRicochets > 16) {
            throw new IllegalArgumentException("maxRicochets must be between 0 and 16");
        }
        if (defaultResistancePerBlock < 0.0 || !Double.isFinite(defaultResistancePerBlock)) {
            throw new IllegalArgumentException("defaultResistancePerBlock must be finite and >= 0");
        }
        validateRetention(defaultVelocityRetention, "defaultVelocityRetention");
        if (defaultRicochetMinAngleDegrees < 0.0 || defaultRicochetMinAngleDegrees > 90.0 || !Double.isFinite(defaultRicochetMinAngleDegrees)) {
            throw new IllegalArgumentException("defaultRicochetMinAngleDegrees must be finite and in [0, 90]");
        }
        validateRetention(defaultRicochetVelocityRetention, "defaultRicochetVelocityRetention");
        rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
        groupRules = List.copyOf(Objects.requireNonNull(groupRules, "groupRules"));
    }

    private static void validateRetention(double value, String name) {
        if (!(value > 0.0) || value > 1.0 || !Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite and in (0, 1]");
        }
    }

    private static String normalizeId(String raw) {
        String value = Objects.requireNonNull(raw, "id").trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) throw new IllegalArgumentException("id cannot be blank");
        return value.indexOf(':') >= 0 ? value : "virgoland:" + value;
    }
}
