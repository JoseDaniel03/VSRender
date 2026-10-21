package net.momirealms.craftengine.core.render.model;

import java.util.Locale;
import java.util.Objects;

/** Explicit render-provider model lookup. VsRender never probes unrelated providers as fallback. */
public record RenderModelReference(String modelId, String providerId) {

    public RenderModelReference {
        Objects.requireNonNull(modelId, "modelId");
        Objects.requireNonNull(providerId, "providerId");
        if (modelId.isBlank()) throw new IllegalArgumentException("modelId must not be blank");
        providerId = providerId.trim().toLowerCase(Locale.ROOT);
        if (providerId.isBlank()) throw new IllegalArgumentException("providerId must not be blank");
    }

    public static RenderModelReference of(String providerId, String modelId) {
        return new RenderModelReference(modelId, providerId);
    }
}
