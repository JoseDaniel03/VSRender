package net.momirealms.craftengine.core.render.animation;

import java.util.Objects;

/** Provider-independent animation resource identity. */
public record RenderAnimationKey(String rigId, String animationId) {
    public RenderAnimationKey {
        Objects.requireNonNull(rigId, "rigId");
        Objects.requireNonNull(animationId, "animationId");
        if (rigId.isBlank()) throw new IllegalArgumentException("rigId must not be blank");
        if (animationId.isBlank()) throw new IllegalArgumentException("animationId must not be blank");
    }
}
