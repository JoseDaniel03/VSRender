package com.virgolandstudio.items.core.ranged;

public record CompiledMaterialImpact(
        double resistancePerBlock,
        double velocityRetention,
        boolean penetrable,
        boolean ricochetEnabled,
        double ricochetMinAngleDegrees,
        double ricochetVelocityRetention
) {
    public CompiledMaterialImpact(double resistancePerBlock, double velocityRetention, boolean penetrable) {
        this(resistancePerBlock, velocityRetention, penetrable, false, 70.0, 0.65);
    }

    public CompiledMaterialImpact {
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
}
