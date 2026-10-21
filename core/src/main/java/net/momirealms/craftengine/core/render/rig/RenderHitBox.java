package net.momirealms.craftengine.core.render.rig;

import net.momirealms.craftengine.core.render.animation.RenderVec3;

import java.util.Objects;

/**
 * Provider-independent hitbox geometry attached to a rig part.
 *
 * <p>The center offset is intentionally separate from the centered AABB extents. This mirrors
 * BetterModel's geometry: the original box center follows the resolved bone pose while the
 * centered bounds keep their own dimensions. Physical platform routing belongs outside Core.</p>
 */
public record RenderHitBox(
        RenderVec3 centerOffset,
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ
) {
    public static final RenderHitBox MIN = centered(RenderVec3.ZERO, 0.1f, 0.1f, 0.1f);

    public RenderHitBox {
        Objects.requireNonNull(centerOffset, "centerOffset");
        requireFinite(minX, "minX");
        requireFinite(minY, "minY");
        requireFinite(minZ, "minZ");
        requireFinite(maxX, "maxX");
        requireFinite(maxY, "maxY");
        requireFinite(maxZ, "maxZ");
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("hitbox min values must be <= max values");
        }
        if (!(maxX - minX > 0f) || !(maxY - minY > 0f) || !(maxZ - minZ > 0f)) {
            throw new IllegalArgumentException("hitbox dimensions must be > 0");
        }
    }

    public static RenderHitBox centered(RenderVec3 centerOffset, float width, float height, float depth) {
        Objects.requireNonNull(centerOffset, "centerOffset");
        requirePositiveFinite(width, "width");
        requirePositiveFinite(height, "height");
        requirePositiveFinite(depth, "depth");
        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;
        float halfDepth = depth * 0.5f;
        return new RenderHitBox(
                centerOffset,
                -halfWidth, -halfHeight, -halfDepth,
                halfWidth, halfHeight, halfDepth
        );
    }

    public float width() { return maxX - minX; }
    public float height() { return maxY - minY; }
    public float depth() { return maxZ - minZ; }

    private static void requirePositiveFinite(float value, String name) {
        requireFinite(value, name);
        if (!(value > 0f)) throw new IllegalArgumentException(name + " must be > 0");
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
