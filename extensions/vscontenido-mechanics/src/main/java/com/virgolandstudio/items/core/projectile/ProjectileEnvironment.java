package com.virgolandstudio.items.core.projectile;

import com.virgolandstudio.items.core.math.Vec3;

public interface ProjectileEnvironment {
    default void onSpawn(VirtualProjectile projectile) {}
    boolean canTrace(VirtualProjectile projectile, Vec3 from, Vec3 to);
    ProjectileCollision trace(VirtualProjectile projectile, Vec3 from, Vec3 to);
    void onSegment(VirtualProjectile projectile, Vec3 from, Vec3 to);
    void onCollision(VirtualProjectile projectile, ProjectileCollision collision);
    default void onRicochet(VirtualProjectile projectile, ProjectileCollision collision) {}
    void onExpire(VirtualProjectile projectile);

    /** Called after a single projectile fails so the platform can log/observe it without killing the shared ticker. */
    default void onError(VirtualProjectile projectile, RuntimeException error) {}
}
