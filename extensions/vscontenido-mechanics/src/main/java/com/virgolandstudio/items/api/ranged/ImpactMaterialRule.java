package com.virgolandstudio.items.api.ranged;

import java.util.Locale;
import java.util.Objects;

/** Authored exact-material block impact rule. Material IDs use namespaced form (for example minecraft:glass). */
public record ImpactMaterialRule(
        String materialId,
        double resistancePerBlock,
        double velocityRetention,
        boolean penetrable,
        boolean ricochetEnabled,
        double ricochetMinAngleDegrees,
        double ricochetVelocityRetention
) {
    public ImpactMaterialRule(String materialId, double resistancePerBlock, double velocityRetention, boolean penetrable) {
        this(materialId, resistancePerBlock, velocityRetention, penetrable, false, 70.0, 0.65);
    }

    public ImpactMaterialRule {
        materialId = normalizeMaterialId(materialId);
        if (resistancePerBlock < 0.0 || !Double.isFinite(resistancePerBlock)) {
            throw new IllegalArgumentException("resistancePerBlock must be finite and >= 0");
        }
        validateRetention(velocityRetention, "velocityRetention");
        if (ricochetMinAngleDegrees < 0.0 || ricochetMinAngleDegrees > 90.0 || !Double.isFinite(ricochetMinAngleDegrees)) {
            throw new IllegalArgumentException("ricochetMinAngleDegrees must be finite and in [0, 90]");
        }
        validateRetention(ricochetVelocityRetention, "ricochetVelocityRetention");
    }

    private static void validateRetention(double value, String name) {
        if (!(value > 0.0) || value > 1.0 || !Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite and in (0, 1]");
        }
    }

    private static String normalizeMaterialId(String raw) {
        String value = Objects.requireNonNull(raw, "materialId").trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) throw new IllegalArgumentException("materialId cannot be blank");
        return value.indexOf(':') >= 0 ? value : "minecraft:" + value;
    }
}
