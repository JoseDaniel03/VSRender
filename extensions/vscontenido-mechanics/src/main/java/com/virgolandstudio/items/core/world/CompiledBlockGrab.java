package com.virgolandstudio.items.core.world;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public record CompiledBlockGrab(
        double maxDistance,
        boolean allowBlockEntities,
        Set<String> allowedMaterials,
        Set<String> deniedMaterials
) {
    public CompiledBlockGrab(double maxDistance, boolean allowBlockEntities) {
        this(maxDistance, allowBlockEntities, Set.of(), Set.of());
    }

    public CompiledBlockGrab {
        if (!Double.isFinite(maxDistance) || maxDistance <= 0.0 || maxDistance > 32.0) {
            throw new IllegalArgumentException("BlockGrab.maxDistance must be in (0, 32]");
        }
        allowedMaterials = normalizedCopy(allowedMaterials, "allowedMaterials");
        deniedMaterials = normalizedCopy(deniedMaterials, "deniedMaterials");
        var overlap = new LinkedHashSet<>(allowedMaterials);
        overlap.retainAll(deniedMaterials);
        if (!overlap.isEmpty()) {
            throw new IllegalArgumentException("BlockGrab material cannot be both allowed and denied: " + overlap.iterator().next());
        }
    }

    /** Deny rules win; an empty allowlist means that every otherwise-safe material is eligible. */
    public boolean permits(String materialId) {
        String normalized = normalizeMaterial(materialId);
        return !deniedMaterials.contains(normalized)
                && (allowedMaterials.isEmpty() || allowedMaterials.contains(normalized));
    }

    private static Set<String> normalizedCopy(Set<String> values, String field) {
        if (values == null || values.isEmpty()) return Set.of();
        var normalized = new LinkedHashSet<String>();
        for (String value : values) {
            try {
                normalized.add(normalizeMaterial(value));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("BlockGrab." + field + " contains " + ex.getMessage());
            }
        }
        return Set.copyOf(normalized);
    }

    private static String normalizeMaterial(String materialId) {
        if (materialId == null || materialId.isBlank()) {
            throw new IllegalArgumentException("a blank material id");
        }
        String normalized = materialId.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) normalized = "minecraft:" + normalized;
        if (!normalized.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("invalid material id '" + materialId + "'");
        }
        return normalized;
    }
}
