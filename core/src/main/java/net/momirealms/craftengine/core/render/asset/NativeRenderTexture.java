package net.momirealms.craftengine.core.render.asset;

import java.util.Arrays;
import java.util.Objects;

/**
 * Texture referenced by native geometry.
 * Embedded textures own PNG bytes; external textures carry an explicit Minecraft resource location.
 */
public final class NativeRenderTexture {
    private final String id;
    private final String resourceLocation;
    private final byte[] png;

    public NativeRenderTexture(String id, String resourceLocation, byte[] png) {
        this.id = requireId(id, "id");
        if (resourceLocation != null && resourceLocation.isBlank()) resourceLocation = null;
        if (png != null && png.length == 0) throw new IllegalArgumentException("png must not be empty");
        if (resourceLocation == null && png == null) {
            throw new IllegalArgumentException("texture must be embedded or have a resourceLocation");
        }
        this.resourceLocation = resourceLocation;
        this.png = png == null ? null : Arrays.copyOf(png, png.length);
    }

    public static NativeRenderTexture embedded(String id, byte[] png) {
        return new NativeRenderTexture(id, null, png);
    }

    public static NativeRenderTexture external(String id, String resourceLocation) {
        return new NativeRenderTexture(id, Objects.requireNonNull(resourceLocation, "resourceLocation"), null);
    }

    public String id() { return id; }
    public String resourceLocation() { return resourceLocation; }
    public boolean embedded() { return png != null; }
    public byte[] png() { return png == null ? null : Arrays.copyOf(png, png.length); }

    private static String requireId(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof NativeRenderTexture other)) return false;
        return id.equals(other.id)
                && Objects.equals(resourceLocation, other.resourceLocation)
                && Arrays.equals(png, other.png);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, resourceLocation);
        return 31 * result + Arrays.hashCode(png);
    }
}
