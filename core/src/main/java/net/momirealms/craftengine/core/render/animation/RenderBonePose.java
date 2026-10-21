package net.momirealms.craftengine.core.render.animation;

import java.util.Objects;

/** Immutable local pose for one rig part. Rotation is Euler degrees. */
public record RenderBonePose(
        RenderVec3 translation,
        RenderVec3 scale,
        RenderVec3 rotation
) {
    public static final RenderBonePose IDENTITY = new RenderBonePose(RenderVec3.ZERO, RenderVec3.ONE, RenderVec3.ZERO);

    public RenderBonePose {
        Objects.requireNonNull(translation, "translation");
        Objects.requireNonNull(scale, "scale");
        Objects.requireNonNull(rotation, "rotation");
    }

    public RenderBonePose lerp(RenderBonePose to, float alpha) {
        Objects.requireNonNull(to, "to");
        return new RenderBonePose(
                translation.lerp(to.translation, alpha),
                scale.lerp(to.scale, alpha),
                rotation.lerp(to.rotation, alpha)
        );
    }
}
