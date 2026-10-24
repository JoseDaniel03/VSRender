package com.virgolandstudio.items.core.projectile;

import com.virgolandstudio.items.core.math.Vec3;

import java.util.UUID;

public record ProjectileCollision(
        ProjectileCollisionKind kind,
        Vec3 position,
        UUID entityId,
        int blockX,
        int blockY,
        int blockZ,
        String blockMaterialId,
        double blockThickness,
        Vec3 blockExitPosition,
        Vec3 surfaceNormal
) {
    public ProjectileCollision {
        if (kind == null) throw new IllegalArgumentException("kind cannot be null");
        if (position == null) throw new IllegalArgumentException("position cannot be null");
        if (blockThickness < 0.0 || !Double.isFinite(blockThickness)) {
            throw new IllegalArgumentException("blockThickness must be finite and >= 0");
        }
    }

    public static ProjectileCollision entity(Vec3 position, UUID entityId) {
        return new ProjectileCollision(ProjectileCollisionKind.ENTITY, position, entityId, 0, 0, 0, "", 0.0, null, null);
    }

    public static ProjectileCollision block(Vec3 position, int x, int y, int z) {
        return block(position, x, y, z, "", 1.0, position, null);
    }

    public static ProjectileCollision block(
            Vec3 position,
            int x,
            int y,
            int z,
            String materialId,
            double thickness,
            Vec3 exitPosition,
            Vec3 surfaceNormal
    ) {
        return new ProjectileCollision(
                ProjectileCollisionKind.BLOCK, position, null, x, y, z,
                materialId == null ? "" : materialId, thickness,
                exitPosition == null ? position : exitPosition, surfaceNormal
        );
    }
}
