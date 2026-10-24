package com.virgolandstudio.items.core.ranged;

public record CompiledRangedSystem(
        CompiledFireControl fireControl,
        CompiledProjectile projectile,
        CompiledSpread spread,
        CompiledRecoil recoil,
        CompiledImpactResponse impactResponse,
        CompiledTrail trail,
        CompiledZoom zoom
) {
    public CompiledRangedSystem(CompiledFireControl fireControl, CompiledProjectile projectile, CompiledSpread spread, CompiledRecoil recoil) {
        this(fireControl, projectile, spread, recoil, null, null, null);
    }

    public CompiledRangedSystem(CompiledFireControl fireControl, CompiledProjectile projectile, CompiledSpread spread, CompiledRecoil recoil, CompiledImpactResponse impactResponse) {
        this(fireControl, projectile, spread, recoil, impactResponse, null, null);
    }

    public CompiledRangedSystem(CompiledFireControl fireControl, CompiledProjectile projectile, CompiledSpread spread, CompiledRecoil recoil, CompiledImpactResponse impactResponse, CompiledTrail trail) {
        this(fireControl, projectile, spread, recoil, impactResponse, trail, null);
    }

    public CompiledRangedSystem {
        if (spread != null && projectile == null) throw new IllegalArgumentException("Spread requires Projectile");
        if (recoil != null && fireControl == null) throw new IllegalArgumentException("Recoil requires FireControl");
        if (impactResponse != null && projectile == null) throw new IllegalArgumentException("ImpactResponse requires Projectile");
        if (trail != null && projectile == null) throw new IllegalArgumentException("Trail requires Projectile");
        if (zoom != null && fireControl == null) throw new IllegalArgumentException("Zoom requires FireControl");
    }
}
