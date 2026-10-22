package net.momirealms.craftengine.core.render.attachment;

import java.util.Objects;

/**
 * Immutable provider-independent pose of a rig part.
 *
 * <p>Rotation components are Euler degrees. Scale is intentionally not represented yet:
 * providers must not invent a scale when their runtime cannot expose a correct world scale.</p>
 */
public record RenderPartTransform(
        RenderTransformSpace space,
        double x,
        double y,
        double z,
        float rotationX,
        float rotationY,
        float rotationZ
) {
    public RenderPartTransform {
        Objects.requireNonNull(space, "space");
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
        requireFinite(rotationX, "rotationX");
        requireFinite(rotationY, "rotationY");
        requireFinite(rotationZ, "rotationZ");
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
