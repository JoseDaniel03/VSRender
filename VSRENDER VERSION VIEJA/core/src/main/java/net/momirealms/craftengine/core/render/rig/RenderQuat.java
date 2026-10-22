package net.momirealms.craftengine.core.render.rig;

import net.momirealms.craftengine.core.render.animation.RenderVec3;

import java.util.Objects;

/** Immutable quaternion used by the provider-independent native rig runtime. */
public record RenderQuat(float x, float y, float z, float w) {
    public static final RenderQuat IDENTITY = new RenderQuat(0f, 0f, 0f, 1f);
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    public RenderQuat {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
        requireFinite(w, "w");
    }

    /** Matches BetterModel/JOML rotateZYX(z, y, x) semantics. */
    public static RenderQuat fromEulerZYXDegrees(RenderVec3 eulerDegrees) {
        Objects.requireNonNull(eulerDegrees, "eulerDegrees");
        float hx = eulerDegrees.x() * DEG_TO_RAD * 0.5f;
        float hy = eulerDegrees.y() * DEG_TO_RAD * 0.5f;
        float hz = eulerDegrees.z() * DEG_TO_RAD * 0.5f;
        RenderQuat qx = new RenderQuat((float) Math.sin(hx), 0f, 0f, (float) Math.cos(hx));
        RenderQuat qy = new RenderQuat(0f, (float) Math.sin(hy), 0f, (float) Math.cos(hy));
        RenderQuat qz = new RenderQuat(0f, 0f, (float) Math.sin(hz), (float) Math.cos(hz));
        return qz.multiply(qy).multiply(qx).normalized();
    }

    /** Hamilton product {@code this * right}. */
    public RenderQuat multiply(RenderQuat right) {
        Objects.requireNonNull(right, "right");
        return new RenderQuat(
                w * right.x + x * right.w + y * right.z - z * right.y,
                w * right.y - x * right.z + y * right.w + z * right.x,
                w * right.z + x * right.y - y * right.x + z * right.w,
                w * right.w - x * right.x - y * right.y - z * right.z
        );
    }

    public RenderQuat normalized() {
        double len = Math.sqrt((double) x * x + (double) y * y + (double) z * z + (double) w * w);
        if (len == 0d) throw new IllegalStateException("cannot normalize zero quaternion");
        float inv = (float) (1d / len);
        return new RenderQuat(x * inv, y * inv, z * inv, w * inv);
    }

    public RenderVec3 rotate(RenderVec3 vector) {
        Objects.requireNonNull(vector, "vector");
        // Optimized q * v * q^-1 for a normalized quaternion.
        float tx = 2f * (y * vector.z() - z * vector.y());
        float ty = 2f * (z * vector.x() - x * vector.z());
        float tz = 2f * (x * vector.y() - y * vector.x());
        return new RenderVec3(
                vector.x() + w * tx + (y * tz - z * ty),
                vector.y() + w * ty + (z * tx - x * tz),
                vector.z() + w * tz + (x * ty - y * tx)
        );
    }

    /**
     * Extracts Euler degrees matching {@link #fromEulerZYXDegrees(RenderVec3)}.
     * The returned vector stores rotations around X, Y and Z respectively.
     */
    public RenderVec3 toEulerZYXDegrees() {
        RenderQuat q = normalized();
        double sinY = 2d * (q.w * q.y - q.z * q.x);
        sinY = Math.max(-1d, Math.min(1d, sinY));
        double y = Math.asin(sinY);
        double x = Math.atan2(
                2d * (q.w * q.x + q.y * q.z),
                1d - 2d * (q.x * q.x + q.y * q.y)
        );
        double z = Math.atan2(
                2d * (q.w * q.z + q.x * q.y),
                1d - 2d * (q.y * q.y + q.z * q.z)
        );
        float radToDeg = (float) (180d / Math.PI);
        return new RenderVec3((float) x * radToDeg, (float) y * radToDeg, (float) z * radToDeg);
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
