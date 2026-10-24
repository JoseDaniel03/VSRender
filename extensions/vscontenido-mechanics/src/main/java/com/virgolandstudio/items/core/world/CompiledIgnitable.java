package com.virgolandstudio.items.core.world;

/** Authored fuse and throw settings for an item that must be lit with a tool. */
public record CompiledIgnitable(int fuseTicks, String ignitionMaterial, double throwSpeed) {
    public CompiledIgnitable {
        if (fuseTicks < 1 || fuseTicks > 1200) throw new IllegalArgumentException("Ignitable.fuseTicks must be within [1,1200]");
        if (ignitionMaterial == null || ignitionMaterial.isBlank()) {
            throw new IllegalArgumentException("Ignitable.ignitionMaterial is required");
        }
        ignitionMaterial = normalizeMaterial(ignitionMaterial);
        if (!Double.isFinite(throwSpeed) || throwSpeed < 0.0 || throwSpeed > 8.0) {
            throw new IllegalArgumentException("Ignitable.throwSpeed must be within [0,8]");
        }
    }

    private static String normalizeMaterial(String raw) {
        String normalized = raw.trim().toUpperCase(java.util.Locale.ROOT);
        int colon = normalized.indexOf(':');
        return colon >= 0 ? normalized.substring(colon + 1) : normalized;
    }
}
