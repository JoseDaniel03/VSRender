package net.momirealms.craftengine.core.render.rig;

import java.util.Objects;

/** Provider-aware rig lookup. Null provider means deterministic legacy probing. */
public record RenderRigReference(String rigId, String providerId, RenderRigType type) {
    public RenderRigReference {
        Objects.requireNonNull(rigId, "rigId");
        Objects.requireNonNull(type, "type");
        if (rigId.isBlank()) throw new IllegalArgumentException("rigId must not be blank");
        if (providerId != null && providerId.isBlank()) throw new IllegalArgumentException("providerId must not be blank");
    }

    public static RenderRigReference legacy(String rigId, RenderRigType type) {
        return new RenderRigReference(rigId, null, type);
    }

    public static RenderRigReference of(String providerId, String rigId, RenderRigType type) {
        return new RenderRigReference(rigId, providerId, type);
    }
}
