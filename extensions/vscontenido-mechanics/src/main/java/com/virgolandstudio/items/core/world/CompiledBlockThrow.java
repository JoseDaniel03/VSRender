package com.virgolandstudio.items.core.world;

public record CompiledBlockThrow(
        double speed,
        boolean hurtEntities,
        double damagePerBlock,
        int maxDamage,
        double collisionRadius,
        int maxLifetimeTicks,
        int maxEntityHits
) {
    public CompiledBlockThrow(double speed, boolean hurtEntities, double damagePerBlock, int maxDamage) {
        this(speed, hurtEntities, damagePerBlock, maxDamage, 0.65, 120, 3);
    }

    public CompiledBlockThrow {
        if (!Double.isFinite(speed) || speed <= 0.0 || speed > 8.0) {
            throw new IllegalArgumentException("BlockThrow.speed must be in (0, 8]");
        }
        if (!Double.isFinite(damagePerBlock) || damagePerBlock < 0.0 || damagePerBlock > 100.0) {
            throw new IllegalArgumentException("BlockThrow.damagePerBlock must be in [0, 100]");
        }
        if (maxDamage < 0 || maxDamage > 1000) {
            throw new IllegalArgumentException("BlockThrow.maxDamage must be in [0, 1000]");
        }
        if (!Double.isFinite(collisionRadius) || collisionRadius < 0.0 || collisionRadius > 4.0) {
            throw new IllegalArgumentException("BlockThrow.collisionRadius must be in [0, 4]");
        }
        if (maxLifetimeTicks < 1 || maxLifetimeTicks > 1200) {
            throw new IllegalArgumentException("BlockThrow.maxLifetimeTicks must be in [1, 1200]");
        }
        if (maxEntityHits < 0 || maxEntityHits > 32) {
            throw new IllegalArgumentException("BlockThrow.maxEntityHits must be in [0, 32]");
        }
    }
}
