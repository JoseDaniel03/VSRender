package net.momirealms.craftengine.core.render.compiled;

import java.util.Objects;

/** Allocation-free parent-before-child pose resolver for the compiled runtime. */
public final class IndexedRigPoseResolver {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private IndexedRigPoseResolver() {
    }

    public static void resolve(IndexedRenderRig rig, RenderAnimationBuffer animation, RenderPoseBuffer out) {
        resolve(rig, animation, out, 0f, 0f, 0f, 1f);
    }

    /**
     * Resolves one pose with an optional owner-head global rotation modifier.
     * The modifier enters/leaves the hierarchy using the same global-modifier cancellation semantics
     * used by BetterModel: a self-only part affects child position through the parent pose but is
     * cancelled from the child's orientation, while a propagated part keeps the modifier in descendants.
     */
    public static void resolve(IndexedRenderRig rig, RenderAnimationBuffer animation, RenderPoseBuffer out,
                               float headQx, float headQy, float headQz, float headQw) {
        Objects.requireNonNull(rig, "rig");
        Objects.requireNonNull(animation, "animation");
        Objects.requireNonNull(out, "out");
        if (animation.rig() != rig || out.rig() != rig) {
            throw new IllegalArgumentException("rig/buffer identity mismatch");
        }
        if (!Float.isFinite(headQx) || !Float.isFinite(headQy) || !Float.isFinite(headQz) || !Float.isFinite(headQw)) {
            throw new IllegalArgumentException("head quaternion must be finite");
        }
        float headInv = invLength(headQx, headQy, headQz, headQw);
        headQx *= headInv; headQy *= headInv; headQz *= headInv; headQw *= headInv;

        out.beginFrame();
        for (int i = 0; i < rig.partCount(); i++) {
            boolean animated = animation.active(i);
            float ltx = rig.restTx(i) + (animated ? animation.tx[i] : 0f);
            float lty = rig.restTy(i) + (animated ? animation.ty[i] : 0f);
            float ltz = rig.restTz(i) + (animated ? animation.tz[i] : 0f);
            float lsx = rig.restSx(i) * (animated ? animation.sx[i] : 1f);
            float lsy = rig.restSy(i) * (animated ? animation.sy[i] : 1f);
            float lsz = rig.restSz(i) * (animated ? animation.sz[i] : 1f);

            float lqx, lqy, lqz, lqw;
            if (!animated || (animation.rx[i] == 0f && animation.ry[i] == 0f && animation.rz[i] == 0f)) {
                lqx = rig.restQx(i); lqy = rig.restQy(i); lqz = rig.restQz(i); lqw = rig.restQw(i);
            } else {
                float hx = (rig.restRx(i) + animation.rx[i]) * DEG_TO_RAD * 0.5f;
                float hy = (rig.restRy(i) + animation.ry[i]) * DEG_TO_RAD * 0.5f;
                float hz = (rig.restRz(i) + animation.rz[i]) * DEG_TO_RAD * 0.5f;
                float sinX = (float) Math.sin(hx), cosX = (float) Math.cos(hx);
                float sinY = (float) Math.sin(hy), cosY = (float) Math.cos(hy);
                float sinZ = (float) Math.sin(hz), cosZ = (float) Math.cos(hz);
                lqx = cosZ * cosY * sinX - sinZ * sinY * cosX;
                lqy = cosZ * sinY * cosX + sinZ * cosY * sinX;
                lqz = sinZ * cosY * cosX - cosZ * sinY * sinX;
                lqw = cosZ * cosY * cosX + sinZ * sinY * sinX;
                float inv = invLength(lqx, lqy, lqz, lqw);
                lqx *= inv; lqy *= inv; lqz *= inv; lqw *= inv;
            }

            int parent = rig.parentIndex(i);
            boolean currentHead = rig.ownerHeadRotation(i);
            if (parent < 0) {
                if (currentHead) {
                    float qx = headQw * lqx + headQx * lqw + headQy * lqz - headQz * lqy;
                    float qy = headQw * lqy - headQx * lqz + headQy * lqw + headQz * lqx;
                    float qz = headQw * lqz + headQx * lqy - headQy * lqx + headQz * lqw;
                    float qw = headQw * lqw - headQx * lqx - headQy * lqy - headQz * lqz;
                    float inv = invLength(qx, qy, qz, qw);
                    lqx = qx * inv; lqy = qy * inv; lqz = qz * inv; lqw = qw * inv;
                }
                out.set(i, ltx, lty, ltz, lsx, lsy, lsz, lqx, lqy, lqz, lqw);
                continue;
            }

            float pqx = out.qx[parent], pqy = out.qy[parent], pqz = out.qz[parent], pqw = out.qw[parent];
            float vx2 = 2f * (pqy * ltz - pqz * lty);
            float vy2 = 2f * (pqz * ltx - pqx * ltz);
            float vz2 = 2f * (pqx * lty - pqy * ltx);
            float rtx = ltx + pqw * vx2 + (pqy * vz2 - pqz * vy2);
            float rty = lty + pqw * vy2 + (pqz * vx2 - pqx * vz2);
            float rtz = ltz + pqw * vz2 + (pqx * vy2 - pqy * vx2);

            float wtx = out.tx[parent] + rtx * out.sx[parent];
            float wty = out.ty[parent] + rty * out.sy[parent];
            float wtz = out.tz[parent] + rtz * out.sz[parent];
            float wsx = out.sx[parent] * lsx;
            float wsy = out.sy[parent] * lsy;
            float wsz = out.sz[parent] * lsz;

            float bqx, bqy, bqz, bqw;
            if (animation.globalRotation[i]) {
                bqx = lqx; bqy = lqy; bqz = lqz; bqw = lqw;
            } else {
                bqx = pqw * lqx + pqx * lqw + pqy * lqz - pqz * lqy;
                bqy = pqw * lqy - pqx * lqz + pqy * lqw + pqz * lqx;
                bqz = pqw * lqz + pqx * lqy - pqy * lqx + pqz * lqw;
                bqw = pqw * lqw - pqx * lqx - pqy * lqy - pqz * lqz;
                float inv = invLength(bqx, bqy, bqz, bqw);
                bqx *= inv; bqy *= inv; bqz *= inv; bqw *= inv;
            }

            boolean parentHead = rig.ownerHeadRotation(parent);
            float wqx = bqx, wqy = bqy, wqz = bqz, wqw = bqw;
            if (currentHead != parentHead) {
                float aqx = currentHead ? headQx : -headQx;
                float aqy = currentHead ? headQy : -headQy;
                float aqz = currentHead ? headQz : -headQz;
                float aqw = headQw;
                float qx = aqw * bqx + aqx * bqw + aqy * bqz - aqz * bqy;
                float qy = aqw * bqy - aqx * bqz + aqy * bqw + aqz * bqx;
                float qz = aqw * bqz + aqx * bqy - aqy * bqx + aqz * bqw;
                float qw = aqw * bqw - aqx * bqx - aqy * bqy - aqz * bqz;
                float inv = invLength(qx, qy, qz, qw);
                wqx = qx * inv; wqy = qy * inv; wqz = qz * inv; wqw = qw * inv;
            }
            out.set(i, wtx, wty, wtz, wsx, wsy, wsz, wqx, wqy, wqz, wqw);
        }
    }

    private static float invLength(float x, float y, float z, float w) {
        double lengthSq = (double) x * x + (double) y * y + (double) z * z + (double) w * w;
        if (!(lengthSq > 0d) || !Double.isFinite(lengthSq)) {
            throw new IllegalArgumentException("quaternion must have finite non-zero length");
        }
        return (float) (1.0 / Math.sqrt(lengthSq));
    }
}
