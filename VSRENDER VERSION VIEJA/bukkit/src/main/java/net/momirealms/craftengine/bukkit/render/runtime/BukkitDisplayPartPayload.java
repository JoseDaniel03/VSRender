package net.momirealms.craftengine.bukkit.render.runtime;

import java.util.Objects;

/**
 * Bukkit-side compiled visual payload for one indexed rig part.
 * Core intentionally does not know about the NMS item object stored here.
 */
public final class BukkitDisplayPartPayload {
    private static final BukkitDisplayPartPayload TRANSFORM_ONLY = new BukkitDisplayPartPayload(
            null, (byte) 0,
            0f, 0f, 0f,
            0f, 0f, 0f,
            1f, 1f, 1f
    );

    private final Object minecraftItem;
    private final byte itemTransform;
    private final float positionX, positionY, positionZ;
    private final float offsetX, offsetY, offsetZ;
    private final float scaleX, scaleY, scaleZ;

    public BukkitDisplayPartPayload(Object minecraftItem,
                                    byte itemTransform,
                                    float positionX, float positionY, float positionZ,
                                    float offsetX, float offsetY, float offsetZ,
                                    float scaleX, float scaleY, float scaleZ) {
        if (!Float.isFinite(positionX) || !Float.isFinite(positionY) || !Float.isFinite(positionZ)
                || !Float.isFinite(offsetX) || !Float.isFinite(offsetY) || !Float.isFinite(offsetZ)
                || !Float.isFinite(scaleX) || !Float.isFinite(scaleY) || !Float.isFinite(scaleZ)) {
            throw new IllegalArgumentException("display payload transform must be finite");
        }
        this.minecraftItem = minecraftItem;
        this.itemTransform = itemTransform;
        this.positionX = positionX;
        this.positionY = positionY;
        this.positionZ = positionZ;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
    }

    public static BukkitDisplayPartPayload transformOnly() {
        return TRANSFORM_ONLY;
    }

    public static BukkitDisplayPartPayload item(Object minecraftItem,
                                                byte itemTransform,
                                                float positionX, float positionY, float positionZ,
                                                float offsetX, float offsetY, float offsetZ,
                                                float scaleX, float scaleY, float scaleZ) {
        return new BukkitDisplayPartPayload(
                Objects.requireNonNull(minecraftItem, "minecraftItem"), itemTransform,
                positionX, positionY, positionZ,
                offsetX, offsetY, offsetZ,
                scaleX, scaleY, scaleZ
        );
    }

    public boolean hasItem() { return minecraftItem != null; }
    public Object minecraftItem() { return minecraftItem; }
    public byte itemTransform() { return itemTransform; }
    public float positionX() { return positionX; }
    public float positionY() { return positionY; }
    public float positionZ() { return positionZ; }
    public float offsetX() { return offsetX; }
    public float offsetY() { return offsetY; }
    public float offsetZ() { return offsetZ; }
    public float scaleX() { return scaleX; }
    public float scaleY() { return scaleY; }
    public float scaleZ() { return scaleZ; }
}
