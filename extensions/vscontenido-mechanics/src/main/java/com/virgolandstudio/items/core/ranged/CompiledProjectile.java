package com.virgolandstudio.items.core.ranged;

public record CompiledProjectile(
        double speed,
        double gravity,
        double drag,
        int maxLifetimeTicks,
        double maxDistance,
        double raySize,
        int maxEntityPenetrations,
        double penetrationVelocityMultiplier
) {
    public CompiledProjectile(double speed, double gravity, double drag, int maxLifetimeTicks, double maxDistance, double raySize) {
        this(speed, gravity, drag, maxLifetimeTicks, maxDistance, raySize, 0, 1.0);
    }

    public CompiledProjectile {
        if (!(speed > 0.0) || !Double.isFinite(speed)) throw new IllegalArgumentException("speed must be finite and > 0");
        if (gravity < 0.0 || !Double.isFinite(gravity)) throw new IllegalArgumentException("gravity must be finite and >= 0");
        if (!(drag > 0.0) || drag > 1.0 || !Double.isFinite(drag)) throw new IllegalArgumentException("drag must be in (0, 1]");
        if (maxLifetimeTicks < 1) throw new IllegalArgumentException("maxLifetimeTicks must be >= 1");
        if (!(maxDistance > 0.0) || !Double.isFinite(maxDistance)) throw new IllegalArgumentException("maxDistance must be finite and > 0");
        if (raySize < 0.0 || !Double.isFinite(raySize)) throw new IllegalArgumentException("raySize must be finite and >= 0");
        if (maxEntityPenetrations < 0 || maxEntityPenetrations > 16) throw new IllegalArgumentException("maxEntityPenetrations must be between 0 and 16");
        if (!(penetrationVelocityMultiplier > 0.0) || penetrationVelocityMultiplier > 1.0 || !Double.isFinite(penetrationVelocityMultiplier)) {
            throw new IllegalArgumentException("penetrationVelocityMultiplier must be in (0, 1]");
        }
    }
}
