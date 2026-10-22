package net.momirealms.craftengine.core.render.asset;

import java.util.Objects;

/** One textured face of a native cube. */
public record NativeRenderFace(
        String textureId,
        NativeRenderUv uv,
        int rotation,
        String cullFace,
        int tintIndex
) {
    public NativeRenderFace {
        Objects.requireNonNull(textureId, "textureId");
        if (textureId.isBlank()) throw new IllegalArgumentException("textureId must not be blank");
        Objects.requireNonNull(uv, "uv");
        if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
            throw new IllegalArgumentException("face rotation must be 0/90/180/270");
        }
        if (cullFace != null && cullFace.isBlank()) cullFace = null;
        if (tintIndex < -1) throw new IllegalArgumentException("tintIndex must be >= -1");
    }
}
