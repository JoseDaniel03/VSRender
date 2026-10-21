package net.momirealms.craftengine.core.render.compiled;

import java.util.BitSet;

/** Reusable dense local-animation buffer. Identity is implicit for inactive parts. */
public final class RenderAnimationBuffer {
    private final IndexedRenderRig rig;
    final float[] tx, ty, tz;
    final float[] sx, sy, sz;
    final float[] rx, ry, rz;
    final boolean[] globalRotation;
    private final BitSet active;

    public RenderAnimationBuffer(IndexedRenderRig rig) {
        if (rig == null) throw new NullPointerException("rig");
        this.rig = rig;
        int partCount = rig.partCount();
        tx = new float[partCount]; ty = new float[partCount]; tz = new float[partCount];
        sx = new float[partCount]; sy = new float[partCount]; sz = new float[partCount];
        rx = new float[partCount]; ry = new float[partCount]; rz = new float[partCount];
        globalRotation = new boolean[partCount];
        active = new BitSet(partCount);
        for (int i = 0; i < partCount; i++) {
            sx[i] = 1f; sy[i] = 1f; sz[i] = 1f;
        }
    }

    public int partCount() { return tx.length; }
    IndexedRenderRig rig() { return rig; }
    public boolean active(int partIndex) { checkIndex(partIndex); return active.get(partIndex); }
    public boolean globalRotation(int partIndex) { checkIndex(partIndex); return globalRotation[partIndex]; }
    public float translationX(int i) { checkIndex(i); return tx[i]; }
    public float translationY(int i) { checkIndex(i); return ty[i]; }
    public float translationZ(int i) { checkIndex(i); return tz[i]; }
    public float scaleX(int i) { checkIndex(i); return sx[i]; }
    public float scaleY(int i) { checkIndex(i); return sy[i]; }
    public float scaleZ(int i) { checkIndex(i); return sz[i]; }
    public float rotationX(int i) { checkIndex(i); return rx[i]; }
    public float rotationY(int i) { checkIndex(i); return ry[i]; }
    public float rotationZ(int i) { checkIndex(i); return rz[i]; }

    void reset() {
        for (int i = active.nextSetBit(0); i >= 0; i = active.nextSetBit(i + 1)) {
            tx[i] = ty[i] = tz[i] = 0f;
            sx[i] = sy[i] = sz[i] = 1f;
            rx[i] = ry[i] = rz[i] = 0f;
            globalRotation[i] = false;
        }
        active.clear();
    }

    void set(int i,
             float tx, float ty, float tz,
             float sx, float sy, float sz,
             float rx, float ry, float rz,
             boolean globalRotation) {
        this.tx[i] = tx; this.ty[i] = ty; this.tz[i] = tz;
        this.sx[i] = sx; this.sy[i] = sy; this.sz[i] = sz;
        this.rx[i] = rx; this.ry[i] = ry; this.rz[i] = rz;
        this.globalRotation[i] = globalRotation;
        active.set(i);
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= tx.length) throw new IndexOutOfBoundsException(index);
    }
}
