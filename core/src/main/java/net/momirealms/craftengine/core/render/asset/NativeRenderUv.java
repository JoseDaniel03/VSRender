package net.momirealms.craftengine.core.render.asset;

/** Blockbench UV rectangle in project-resolution coordinates. */
public record NativeRenderUv(float u1, float v1, float u2, float v2) {
    public NativeRenderUv {
        requireFinite(u1, "u1");
        requireFinite(v1, "v1");
        requireFinite(u2, "u2");
        requireFinite(v2, "v2");
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
