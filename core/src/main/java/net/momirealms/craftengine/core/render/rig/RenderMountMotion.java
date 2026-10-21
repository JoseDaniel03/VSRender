package net.momirealms.craftengine.core.render.rig;

/** Value-only result of native mount input normalization/rotation. */
public record RenderMountMotion(float x, float y, float z) {
    public static final RenderMountMotion ZERO = new RenderMountMotion(0f, 0f, 0f);

    public RenderMountMotion {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
    }

    public float lengthSquared() { return x * x + y * y + z * z; }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
