package net.momirealms.craftengine.core.render.animation;

import java.util.Objects;

/** One pose sample on an animation track. Interpolation describes the segment leaving this frame. */
public record RenderAnimationFrame(
        float timeSeconds,
        RenderBonePose pose,
        RenderInterpolation interpolation,
        boolean globalRotation
) {
    public RenderAnimationFrame {
        if (!Float.isFinite(timeSeconds) || timeSeconds < 0f) {
            throw new IllegalArgumentException("timeSeconds must be finite and >= 0");
        }
        Objects.requireNonNull(pose, "pose");
        Objects.requireNonNull(interpolation, "interpolation");
    }

    public RenderAnimationFrame(float timeSeconds, RenderBonePose pose, RenderInterpolation interpolation) {
        this(timeSeconds, pose, interpolation, false);
    }
}
