package net.momirealms.craftengine.core.render.animation;

import java.util.Objects;

/**
 * One sampled rig-part state. {@code globalRotation} preserves the source animation's
 * rotation-space semantics instead of flattening them into an ambiguous Euler pose.
 */
public record RenderAnimationSample(RenderBonePose pose, boolean globalRotation) {
    public RenderAnimationSample {
        Objects.requireNonNull(pose, "pose");
    }
}
