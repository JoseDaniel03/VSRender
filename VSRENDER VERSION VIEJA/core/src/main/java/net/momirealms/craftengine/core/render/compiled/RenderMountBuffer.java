package net.momirealms.craftengine.core.render.compiled;

import net.momirealms.craftengine.core.render.rig.RenderMountPolicy;

import java.util.BitSet;
import java.util.function.IntConsumer;

/**
 * Reusable dense buffer containing resolved rig-space anchor positions for mountable parts.
 * Platform code owns carrier entities, passengers and movement; this class only exposes pose data.
 */
public final class RenderMountBuffer {
    private static final float EPSILON = 1e-5f;

    private final IndexedRenderRig rig;
    private final float[] x, y, z;
    private final boolean[] initialized;
    private final BitSet dirty;

    public RenderMountBuffer(IndexedRenderRig rig) {
        if (rig == null) throw new NullPointerException("rig");
        this.rig = rig;
        int parts = rig.partCount();
        this.x = new float[parts];
        this.y = new float[parts];
        this.z = new float[parts];
        this.initialized = new boolean[parts];
        this.dirty = new BitSet(parts);
    }

    public int mountCount() { return rig.mountIndices().length; }
    public int[] mountIndices() { return rig.mountIndices(); }
    public boolean mountable(int part) { checkMount(part); return true; }
    public RenderMountPolicy policy(int part) { checkMount(part); return rig.mountPolicy(part); }
    public float x(int part) { checkMount(part); return x[part]; }
    public float y(int part) { checkMount(part); return y[part]; }
    public float z(int part) { checkMount(part); return z[part]; }
    public boolean dirty(int part) { checkMount(part); return dirty.get(part); }
    public int dirtyCount() { return dirty.cardinality(); }
    public BitSet dirtyParts() { return (BitSet) dirty.clone(); }

    public void forEachDirty(IntConsumer consumer) {
        if (consumer == null) throw new NullPointerException("consumer");
        for (int i = dirty.nextSetBit(0); i >= 0; i = dirty.nextSetBit(i + 1)) consumer.accept(i);
    }

    IndexedRenderRig rig() { return rig; }

    void beginFrame() { dirty.clear(); }

    void set(int part, float x, float y, float z) {
        boolean changed = !initialized[part]
                || differs(this.x[part], x)
                || differs(this.y[part], y)
                || differs(this.z[part], z);
        this.x[part] = x;
        this.y[part] = y;
        this.z[part] = z;
        this.initialized[part] = true;
        if (changed) dirty.set(part);
    }

    private static boolean differs(float a, float b) {
        return Math.abs(a - b) > EPSILON;
    }

    private void checkMount(int part) {
        RenderMountPolicy policy = rig.mountPolicy(part);
        if (!policy.canMount()) throw new IllegalStateException("part is not mountable: " + rig.partId(part));
    }
}
