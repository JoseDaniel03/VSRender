package com.virgolandstudio.items.core.accessory;

import com.virgolandstudio.items.api.accessory.AccessoryModifierDefinition;
import com.virgolandstudio.items.core.instance.ItemInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Resolves installed accessory definitions into one runtime modifier set. */
public final class AccessoryModifierResolver {
    private static final int MAX_CACHE_ENTRIES = 512;

    private final AccessoryRegistry registry;
    private final Map<UUID, CacheEntry> cache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, CacheEntry> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };

    public AccessoryModifierResolver(AccessoryRegistry registry) {
        this.registry = java.util.Objects.requireNonNull(registry, "registry");
    }

    public AccessoryModifierSet resolve(ItemInstance instance) {
        if (instance == null || instance.definition().stateLayout().find(AttachmentLoadout.STATE_KEY) == null) {
            return AccessoryModifierSet.empty();
        }
        Object raw = instance.state().get(AttachmentLoadout.STATE_KEY);
        if (!(raw instanceof String encoded) || encoded.isBlank()) {
            cache.remove(instance.id());
            return AccessoryModifierSet.empty();
        }

        CacheEntry cached = cache.get(instance.id());
        if (cached != null && cached.encoded().equals(encoded)) return cached.modifiers();

        AccessoryModifierSet resolved = resolveEncoded(encoded);
        cache.put(instance.id(), new CacheEntry(encoded, resolved));
        return resolved;
    }

    public void invalidate(UUID instanceId) {
        if (instanceId != null) cache.remove(instanceId);
    }

    public void clearCache() { cache.clear(); }
    public int cacheSize() { return cache.size(); }

    private AccessoryModifierSet resolveEncoded(String encoded) {
        AttachmentLoadout loadout;
        try {
            loadout = AttachmentLoadoutCodec.decode(encoded);
        } catch (IllegalArgumentException ignored) {
            return AccessoryModifierSet.empty();
        }
        if (loadout.isEmpty()) return AccessoryModifierSet.empty();

        var entries = new ArrayList<>(loadout.snapshot().entrySet());
        entries.sort(Comparator.comparing(java.util.Map.Entry::getKey));
        var modifiers = new ArrayList<AccessoryModifierDefinition>();
        for (var entry : entries) {
            registry.find(entry.getValue().accessoryDefinitionId())
                    .ifPresent(definition -> modifiers.addAll(definition.modifiers()));
        }
        return modifiers.isEmpty() ? AccessoryModifierSet.empty() : new AccessoryModifierSet(modifiers);
    }

    private record CacheEntry(String encoded, AccessoryModifierSet modifiers) {}
}
