package net.momirealms.craftengine.core.render.compiled;

import net.momirealms.craftengine.core.render.animation.RenderAnimationFrame;
import net.momirealms.craftengine.core.render.animation.RenderAnimationTrack;
import net.momirealms.craftengine.core.render.animation.RenderInterpolation;

/** Primitive-array runtime representation of one animation track. */
final class IndexedAnimationTrack {
    final int partIndex;
    final float[] time;
    final float[] tx, ty, tz;
    final float[] sx, sy, sz;
    final float[] rx, ry, rz;
    final byte[] interpolation;
    final boolean[] globalRotation;

    private IndexedAnimationTrack(int partIndex, int size) {
        this.partIndex = partIndex;
        time = new float[size];
        tx = new float[size]; ty = new float[size]; tz = new float[size];
        sx = new float[size]; sy = new float[size]; sz = new float[size];
        rx = new float[size]; ry = new float[size]; rz = new float[size];
        interpolation = new byte[size];
        globalRotation = new boolean[size];
    }

    static IndexedAnimationTrack compile(int partIndex, RenderAnimationTrack source) {
        IndexedAnimationTrack out = new IndexedAnimationTrack(partIndex, source.frames().size());
        for (int i = 0; i < out.time.length; i++) {
            RenderAnimationFrame frame = source.frames().get(i);
            out.time[i] = frame.timeSeconds();
            out.tx[i] = frame.pose().translation().x();
            out.ty[i] = frame.pose().translation().y();
            out.tz[i] = frame.pose().translation().z();
            out.sx[i] = frame.pose().scale().x();
            out.sy[i] = frame.pose().scale().y();
            out.sz[i] = frame.pose().scale().z();
            out.rx[i] = frame.pose().rotation().x();
            out.ry[i] = frame.pose().rotation().y();
            out.rz[i] = frame.pose().rotation().z();
            out.interpolation[i] = (byte) (frame.interpolation() == RenderInterpolation.STEP ? 0 : 1);
            out.globalRotation[i] = frame.globalRotation();
        }
        return out;
    }
}
