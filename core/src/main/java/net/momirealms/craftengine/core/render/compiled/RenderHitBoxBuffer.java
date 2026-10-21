package net.momirealms.craftengine.core.render.compiled;

import java.util.BitSet;
import java.util.function.IntConsumer;

/**
 * Reusable dense buffer containing resolved rig-space centers for hitbox-bearing parts.
 * Physical world anchoring, owner scale and interaction entities belong to the platform layer.
 */
public final class RenderHitBoxBuffer {
    private static final float EPSILON = 1e-5f;

    private final IndexedRenderRig rig;
    private final float[] centerX;
    private final float[] centerY;
    private final float[] centerZ;
    private final boolean[] initialized;
    private final BitSet dirty;

    public RenderHitBoxBuffer(IndexedRenderRig rig) {
        if (rig == null) throw new NullPointerException("rig");
        this.rig = rig;
        int parts = rig.partCount();
        centerX = new float[parts];
        centerY = new float[parts];
        centerZ = new float[parts];
        initialized = new boolean[parts];
        dirty = new BitSet(parts);
    }

    public int hitBoxCount() { return rig.hitBoxIndices().length; }
    public boolean hasHitBox(int part) { return rig.hasHitBox(part); }
    public int[] hitBoxIndices() { return rig.hitBoxIndices(); }
    public boolean dirty(int part) { checkHitBox(part); return dirty.get(part); }
    public int dirtyCount() { return dirty.cardinality(); }
    public BitSet dirtyParts() { return (BitSet) dirty.clone(); }
    public void forEachDirty(IntConsumer consumer) {
        if (consumer == null) throw new NullPointerException("consumer");
        for (int i = dirty.nextSetBit(0); i >= 0; i = dirty.nextSetBit(i + 1)) consumer.accept(i);
    }

    public float centerX(int part) { checkHitBox(part); return centerX[part]; }
    public float centerY(int part) { checkHitBox(part); return centerY[part]; }
    public float centerZ(int part) { checkHitBox(part); return centerZ[part]; }

    public float minX(int part) { checkHitBox(part); return rig.hitBoxMinX(part); }
    public float minY(int part) { checkHitBox(part); return rig.hitBoxMinY(part); }
    public float minZ(int part) { checkHitBox(part); return rig.hitBoxMinZ(part); }
    public float maxX(int part) { checkHitBox(part); return rig.hitBoxMaxX(part); }
    public float maxY(int part) { checkHitBox(part); return rig.hitBoxMaxY(part); }
    public float maxZ(int part) { checkHitBox(part); return rig.hitBoxMaxZ(part); }
    public float width(int part) { return maxX(part) - minX(part); }
    public float height(int part) { return maxY(part) - minY(part); }
    public float depth(int part) { return maxZ(part) - minZ(part); }

    IndexedRenderRig rig() { return rig; }

    void beginFrame() {
        dirty.clear();
    }

    void setCenter(int part, float x, float y, float z) {
        boolean changed = !initialized[part]
                || differs(centerX[part], x)
                || differs(centerY[part], y)
                || differs(centerZ[part], z);
        centerX[part] = x;
        centerY[part] = y;
        centerZ[part] = z;
        initialized[part] = true;
        if (changed) dirty.set(part);
    }

    private static boolean differs(float a, float b) {
        return Math.abs(a - b) > EPSILON;
    }

    private void checkHitBox(int part) {
        if (!rig.hasHitBox(part)) throw new IllegalStateException("part has no hitbox: " + rig.partId(part));
    }
}
