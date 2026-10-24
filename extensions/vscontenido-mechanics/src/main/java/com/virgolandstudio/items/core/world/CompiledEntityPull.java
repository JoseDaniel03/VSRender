package com.virgolandstudio.items.core.world;

public record CompiledEntityPull(
        double maxDistance,
        double raySize,
        double strength,
        double verticalLift,
        boolean includePlayers
) {
    public CompiledEntityPull {
        if (!Double.isFinite(maxDistance) || maxDistance <= 0.0 || maxDistance > 64.0) {
            throw new IllegalArgumentException("EntityPull.maxDistance must be in (0, 64]");
        }
        if (!Double.isFinite(raySize) || raySize < 0.0 || raySize > 4.0) {
            throw new IllegalArgumentException("EntityPull.raySize must be in [0, 4]");
        }
        if (!Double.isFinite(strength) || strength <= 0.0 || strength > 8.0) {
            throw new IllegalArgumentException("EntityPull.strength must be in (0, 8]");
        }
        if (!Double.isFinite(verticalLift) || verticalLift < -4.0 || verticalLift > 4.0) {
            throw new IllegalArgumentException("EntityPull.verticalLift must be in [-4, 4]");
        }
    }
}
