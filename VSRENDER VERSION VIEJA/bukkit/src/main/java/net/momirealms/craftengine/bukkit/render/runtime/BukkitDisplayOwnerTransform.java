package net.momirealms.craftengine.bukkit.render.runtime;

/**
 * Provider-independent-at-runtime owner transform applied after rig resolution at the Bukkit packet edge.
 * BetterModel parity: (pose translation + rotated visual offset) * scale + visual position + owner offset.
 */
public record BukkitDisplayOwnerTransform(float scale, float offsetX, float offsetY, float offsetZ) {
    public static final BukkitDisplayOwnerTransform IDENTITY = new BukkitDisplayOwnerTransform(1f, 0f, 0f, 0f);

    public BukkitDisplayOwnerTransform {
        if (!Float.isFinite(scale) || scale <= 0f
                || !Float.isFinite(offsetX) || !Float.isFinite(offsetY) || !Float.isFinite(offsetZ)) {
            throw new IllegalArgumentException("owner display transform must be finite and scale > 0");
        }
    }
}
