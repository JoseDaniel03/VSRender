package net.momirealms.craftengine.core.render.runtime;

import net.momirealms.craftengine.core.render.compiled.RenderPoseBuffer;

/** Reusable synchronous view of the parts that must be published for one frame. */
public final class RenderFrameBatch {
    private final int[] partIndices;
    private RenderPoseBuffer poses;
    private int size;
    private int interpolationTicks;
    private RenderLodLevel lod = RenderLodLevel.FULL;

    public RenderFrameBatch(int maxParts) {
        if (maxParts < 0) throw new IllegalArgumentException("maxParts must be >= 0");
        this.partIndices = new int[maxParts];
    }

    public void captureAll(RenderPoseBuffer poses, RenderLodLevel lod) {
        begin(poses, lod);
        int count = poses.partCount();
        for (int i = 0; i < count; i++) partIndices[size++] = i;
    }

    public void captureDirty(RenderPoseBuffer poses, RenderLodLevel lod) {
        begin(poses, lod);
        poses.forEachDirty(i -> partIndices[size++] = i);
    }

    private void begin(RenderPoseBuffer poses, RenderLodLevel lod) {
        if (poses == null) throw new NullPointerException("poses");
        if (lod == null || lod.culled()) throw new IllegalArgumentException("lod must be renderable");
        if (poses.partCount() > partIndices.length) throw new IllegalArgumentException("pose buffer exceeds batch capacity");
        this.poses = poses;
        this.lod = lod;
        this.interpolationTicks = lod.intervalTicks();
        this.size = 0;
    }

    public int size() { return size; }
    public boolean empty() { return size == 0; }
    public int partIndex(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        return partIndices[index];
    }
    public RenderPoseBuffer poses() {
        if (poses == null) throw new IllegalStateException("batch not initialized");
        return poses;
    }
    public int interpolationTicks() { return interpolationTicks; }
    public RenderLodLevel lod() { return lod; }
}
