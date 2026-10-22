package net.momirealms.craftengine.core.render.compiled;

import net.momirealms.craftengine.core.render.animation.RenderVec3;
import net.momirealms.craftengine.core.render.rig.RenderQuat;
import net.momirealms.craftengine.core.render.rig.RenderResolvedPose;

import java.util.BitSet;
import java.util.function.IntConsumer;

/** Reusable dense resolved-pose buffer with per-frame dirty tracking. */
public final class RenderPoseBuffer {
    private static final float EPSILON = 1e-5f;

    private final IndexedRenderRig rig;
    final float[] tx, ty, tz;
    final float[] sx, sy, sz;
    final float[] qx, qy, qz, qw;
    private final boolean[] initialized;
    private final BitSet dirty;

    public RenderPoseBuffer(IndexedRenderRig rig) {
        if (rig == null) throw new NullPointerException("rig");
        this.rig = rig;
        int partCount = rig.partCount();
        tx = new float[partCount]; ty = new float[partCount]; tz = new float[partCount];
        sx = new float[partCount]; sy = new float[partCount]; sz = new float[partCount];
        qx = new float[partCount]; qy = new float[partCount]; qz = new float[partCount]; qw = new float[partCount];
        initialized = new boolean[partCount];
        dirty = new BitSet(partCount);
    }

    public int partCount() { return tx.length; }
    IndexedRenderRig rig() { return rig; }
    public boolean dirty(int index) { checkIndex(index); return dirty.get(index); }
    public int dirtyCount() { return dirty.cardinality(); }
    public BitSet dirtyParts() { return (BitSet) dirty.clone(); }
    public void forEachDirty(IntConsumer consumer) {
        if (consumer == null) throw new NullPointerException("consumer");
        for (int i = dirty.nextSetBit(0); i >= 0; i = dirty.nextSetBit(i + 1)) consumer.accept(i);
    }

    public float translationX(int i) { checkIndex(i); return tx[i]; }
    public float translationY(int i) { checkIndex(i); return ty[i]; }
    public float translationZ(int i) { checkIndex(i); return tz[i]; }
    public float scaleX(int i) { checkIndex(i); return sx[i]; }
    public float scaleY(int i) { checkIndex(i); return sy[i]; }
    public float scaleZ(int i) { checkIndex(i); return sz[i]; }
    public float rotationX(int i) { checkIndex(i); return qx[i]; }
    public float rotationY(int i) { checkIndex(i); return qy[i]; }
    public float rotationZ(int i) { checkIndex(i); return qz[i]; }
    public float rotationW(int i) { checkIndex(i); return qw[i]; }

    /** Debug/inspection view; render hot paths should prefer the primitive accessors. */
    public RenderResolvedPose pose(int index) {
        checkIndex(index);
        if (!initialized[index]) throw new IllegalStateException("pose not initialized: " + index);
        return new RenderResolvedPose(
                new RenderVec3(tx[index], ty[index], tz[index]),
                new RenderVec3(sx[index], sy[index], sz[index]),
                new RenderQuat(qx[index], qy[index], qz[index], qw[index])
        );
    }

    void beginFrame() {
        dirty.clear();
    }

    void set(int i,
             float tx, float ty, float tz,
             float sx, float sy, float sz,
             float qx, float qy, float qz, float qw) {
        boolean changed = !initialized[i]
                || differs(this.tx[i], tx) || differs(this.ty[i], ty) || differs(this.tz[i], tz)
                || differs(this.sx[i], sx) || differs(this.sy[i], sy) || differs(this.sz[i], sz)
                || quaternionDiffers(this.qx[i], this.qy[i], this.qz[i], this.qw[i], qx, qy, qz, qw);
        this.tx[i] = tx; this.ty[i] = ty; this.tz[i] = tz;
        this.sx[i] = sx; this.sy[i] = sy; this.sz[i] = sz;
        this.qx[i] = qx; this.qy[i] = qy; this.qz[i] = qz; this.qw[i] = qw;
        initialized[i] = true;
        if (changed) dirty.set(i);
    }

    private static boolean differs(float a, float b) {
        return Math.abs(a - b) > EPSILON;
    }

    private static boolean quaternionDiffers(float ax, float ay, float az, float aw,
                                             float bx, float by, float bz, float bw) {
        // q and -q encode the same rotation; compare absolute dot product.
        float dot = Math.abs(ax * bx + ay * by + az * bz + aw * bw);
        return 1f - dot > EPSILON;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= tx.length) throw new IndexOutOfBoundsException(index);
    }
}
