package com.virgolandstudio.items.api.ranged;

import java.util.Locale;
import java.util.Objects;

/** Authored impact rule targeting a reusable material group (for example virgoland:glass). */
public record ImpactMaterialGroupRule(
        String groupId,
        double resistancePerBlock,
        double velocityRetention,
        boolean penetrable,
        boolean ricochetEnabled,
        double ricochetMinAngleDegrees,
        double ricochetVelocityRetention
) {
    public ImpactMaterialGroupRule(String groupId, double resistancePerBlock, double velocityRetention, boolean penetrable) {
        this(groupId, resistancePerBlock, velocityRetention, penetrable, false, 70.0, 0.65);
    }

    public ImpactMaterialGroupRule {
        groupId = normalizeGroupId(groupId);
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

    private static String normalizeGroupId(String raw) {
        String value = Objects.requireNonNull(raw, "groupId").trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) throw new IllegalArgumentException("groupId cannot be blank");
        return value.indexOf(':') >= 0 ? value : "virgoland:" + value;
    }
}
