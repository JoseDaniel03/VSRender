package net.momirealms.craftengine.core.render.rig;

/** Immutable rider input sampled at the platform boundary. */
public record RenderMountInput(float strafe, float vertical, float forward, boolean jump) {
    public static final RenderMountInput ZERO = new RenderMountInput(0f, 0f, 0f, false);

    public RenderMountInput {
        requireFinite(strafe, "strafe");
        requireFinite(vertical, "vertical");
        requireFinite(forward, "forward");
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
