package com.virgolandstudio.items.core.ammo;

import com.virgolandstudio.items.api.NamespacedId;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MagazineRegistry {
    private final ConcurrentHashMap<UUID, MagazineInstance> magazines = new ConcurrentHashMap<>();

    public MagazineInstance create(NamespacedId definitionId, String ammoType, int capacity, int rounds) {
        return create(UUID.randomUUID(), definitionId, ammoType, capacity, rounds);
    }

    public MagazineInstance create(UUID id, NamespacedId definitionId, String ammoType, int capacity, int rounds) {
        var created = new MagazineInstance(id, definitionId, ammoType, capacity, rounds);
        var existing = magazines.putIfAbsent(id, created);
        return existing != null ? existing : created;
    }

    public Optional<MagazineInstance> find(UUID id) { return Optional.ofNullable(magazines.get(id)); }
    public void remove(UUID id) { magazines.remove(id); }
    public int size() { return magazines.size(); }
}
