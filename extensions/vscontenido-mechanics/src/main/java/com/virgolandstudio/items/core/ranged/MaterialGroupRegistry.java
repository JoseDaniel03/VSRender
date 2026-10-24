package com.virgolandstudio.items.core.ranged;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Global reusable material groups. Platform adapters may populate these from server tags/registries. */
public final class MaterialGroupRegistry {
    private final Map<String, Set<String>> groups = new LinkedHashMap<>();

    public void register(String groupId, Collection<String> materialIds) {
        String normalizedGroup = normalizeGroupId(groupId);
        var normalizedMaterials = new LinkedHashSet<String>();
        for (String materialId : materialIds) normalizedMaterials.add(normalizeMaterialId(materialId));
        if (normalizedMaterials.isEmpty()) throw new IllegalArgumentException("Material group " + normalizedGroup + " cannot be empty");
        if (groups.putIfAbsent(normalizedGroup, Set.copyOf(normalizedMaterials)) != null) {
            throw new IllegalArgumentException("Duplicate material group: " + normalizedGroup);
        }
    }

    public Optional<Set<String>> find(String groupId) {
        return Optional.ofNullable(groups.get(normalizeGroupId(groupId)));
    }

    public Map<String, Set<String>> all() { return Map.copyOf(groups); }

    public static String normalizeGroupId(String raw) {
        if (raw == null) throw new IllegalArgumentException("groupId cannot be null");
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) throw new IllegalArgumentException("groupId cannot be blank");
        return value.indexOf(':') >= 0 ? value : "virgoland:" + value;
    }

    public static String normalizeMaterialId(String raw) {
        if (raw == null) throw new IllegalArgumentException("materialId cannot be null");
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) throw new IllegalArgumentException("materialId cannot be blank");
        return value.indexOf(':') >= 0 ? value : "minecraft:" + value;
    }
}
