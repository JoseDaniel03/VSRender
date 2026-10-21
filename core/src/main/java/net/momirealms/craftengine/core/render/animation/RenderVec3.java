package net.momirealms.craftengine.core.render.animation;

/** Small immutable vector used by the provider-independent animation runtime. */
public record RenderVec3(float x, float y, float z) {
    public static final RenderVec3 ZERO = new RenderVec3(0f, 0f, 0f);
    public static final RenderVec3 ONE = new RenderVec3(1f, 1f, 1f);

    public RenderVec3 {
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
    }


    public RenderVec3 add(RenderVec3 other) {
        if (other == null) throw new NullPointerException("other");
        return new RenderVec3(x + other.x, y + other.y, z + other.z);
    }

    public RenderVec3 subtract(RenderVec3 other) {
        if (other == null) throw new NullPointerException("other");
        return new RenderVec3(x - other.x, y - other.y, z - other.z);
    }

    public RenderVec3 multiply(RenderVec3 other) {
        if (other == null) throw new NullPointerException("other");
        return new RenderVec3(x * other.x, y * other.y, z * other.z);
    }

    public RenderVec3 lerp(RenderVec3 to, float alpha) {
        if (to == null) throw new NullPointerException("to");
        if (!Float.isFinite(alpha)) throw new IllegalArgumentException("alpha must be finite");
        return new RenderVec3(
                x + (to.x - x) * alpha,
                y + (to.y - y) * alpha,
                z + (to.z - z) * alpha
        );
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
