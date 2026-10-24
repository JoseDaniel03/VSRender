package com.virgolandstudio.items.core.ranged;

import com.virgolandstudio.items.api.ranged.ImpactProfileDefinition;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Small startup registry of reusable impact profiles. No lookup occurs on projectile hot paths after compilation. */
public final class ImpactProfileRegistry {
    private final Map<String, ImpactProfileDefinition> profiles = new LinkedHashMap<>();

    public void register(ImpactProfileDefinition profile) {
        String id = normalize(profile.id());
        if (profiles.putIfAbsent(id, profile) != null) {
            throw new IllegalArgumentException("Impact profile already registered: " + id);
        }
    }

    public Optional<ImpactProfileDefinition> find(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return Optional.ofNullable(profiles.get(normalize(id)));
    }

    public Map<String, ImpactProfileDefinition> all() { return Map.copyOf(profiles); }

    public static String normalize(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return value.indexOf(':') >= 0 ? value : "virgoland:" + value;
    }
}
