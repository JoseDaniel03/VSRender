package com.virgolandstudio.items.core.world;

/** Bounded radial-damage authoring shared by explosives and future area weapons. */
public record CompiledAreaDamage(
        double radius,
        double maxDamage,
        double minDamage,
        int maxTargets,
        boolean includePlayers,
        boolean linearFalloff
) {
    public CompiledAreaDamage {
        if (!Double.isFinite(radius) || radius <= 0.0 || radius > 64.0) {
            throw new IllegalArgumentException("AreaDamage.radius must be within (0,64]");
        }
        if (!Double.isFinite(maxDamage) || maxDamage < 0.0 || maxDamage > 1000.0) {
            throw new IllegalArgumentException("AreaDamage.maxDamage must be within [0,1000]");
        }
        if (!Double.isFinite(minDamage) || minDamage < 0.0 || minDamage > maxDamage) {
            throw new IllegalArgumentException("AreaDamage.minDamage must be within [0,maxDamage]");
        }
        if (maxTargets < 1 || maxTargets > 256) {
            throw new IllegalArgumentException("AreaDamage.maxTargets must be within [1,256]");
        }
    }

    public double damageAt(double distance) {
        if (!Double.isFinite(distance) || distance < 0.0 || distance > radius) return 0.0;
        if (!linearFalloff) return maxDamage;
        double fraction = distance / radius;
        return maxDamage + (minDamage - maxDamage) * fraction;
    }
}
