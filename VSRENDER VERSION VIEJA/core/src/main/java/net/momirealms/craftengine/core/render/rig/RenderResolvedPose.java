package net.momirealms.craftengine.core.render.rig;

import net.momirealms.craftengine.core.render.animation.RenderVec3;

import java.util.Objects;

/** Immutable rig-part pose resolved in rig/instance space. */
public record RenderResolvedPose(RenderVec3 translation, RenderVec3 scale, RenderQuat rotation) {
    public static final RenderResolvedPose IDENTITY = new RenderResolvedPose(RenderVec3.ZERO, RenderVec3.ONE, RenderQuat.IDENTITY);

    public RenderResolvedPose {
        Objects.requireNonNull(translation, "translation");
        Objects.requireNonNull(scale, "scale");
        Objects.requireNonNull(rotation, "rotation");
    }
}
