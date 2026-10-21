package net.momirealms.craftengine.core.render.profile;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Provider-independent player/profile skin reference for rendered models.
 * A profile may be resolvable by UUID/name, carry embedded Minecraft texture data, or both.
 */
public record RenderProfile(
        @Nullable UUID id,
        @Nullable String name,
        @Nullable String rawTextures
) {
    public static final UUID UNKNOWN_ID = new UUID(0L, 0L);

    public RenderProfile {
        if (UNKNOWN_ID.equals(id)) id = null;
        name = normalize(name);
        rawTextures = normalize(rawTextures);
        if (id == null && name == null && rawTextures == null) {
            throw new IllegalArgumentException("profile must contain id, name or rawTextures");
        }
    }

    public static RenderProfile of(@Nullable UUID id, @Nullable String name, @Nullable String rawTextures) {
        return new RenderProfile(id, name, rawTextures);
    }

    public boolean hasEmbeddedTextures() {
        return rawTextures != null;
    }

    public boolean hasLookupIdentity() {
        return id != null || name != null;
    }

    private static @Nullable String normalize(@Nullable String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
