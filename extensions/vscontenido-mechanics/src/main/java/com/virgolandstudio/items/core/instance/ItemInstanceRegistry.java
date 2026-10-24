package com.virgolandstudio.items.core.instance;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.core.compile.CompiledItemDefinition;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Runtime cache for item instances.
 *
 * <p>The registry is deliberately not an ownership database: ItemStacks carry the persistent
 * instance id/state. A cache entry receives an access lease whenever gameplay resolves it and
 * clean idle entries may be evicted. This lets Studio distinguish recently active instances from
 * historical cache entries without pretending the cache can enumerate every stack in the world.</p>
 */
public final class ItemInstanceRegistry {
    private final ConcurrentHashMap<UUID, Entry> instances = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public ItemInstanceRegistry() {
        this(System::currentTimeMillis);
    }

    public ItemInstanceRegistry(LongSupplier clock) {
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    public ItemInstance create(CompiledItemDefinition definition) {
        long now = now();
        var instance = new ItemInstance(UUID.randomUUID(), definition);
        instances.put(instance.id(), new Entry(instance, now, now));
        return instance;
    }

    public ItemInstance getOrCreate(UUID id, CompiledItemDefinition definition) {
        long now = now();
        Entry entry = instances.compute(id, (ignored, existing) -> {
            if (existing != null) {
                existing.lastAccessMillis = now;
                return existing;
            }
            return new Entry(new ItemInstance(id, definition), now, now);
        });
        return entry.instance;
    }

    public ItemInstance create(UUID id, CompiledItemDefinition definition) {
        long now = now();
        var created = new Entry(new ItemInstance(id, definition), now, now);
        var existing = instances.putIfAbsent(id, created);
        Entry result = existing == null ? created : existing;
        result.lastAccessMillis = now;
        return result.instance;
    }

    /** Finds an instance and renews its gameplay access lease. */
    public Optional<ItemInstance> find(UUID id) {
        Entry entry = instances.get(id);
        if (entry == null) return Optional.empty();
        entry.lastAccessMillis = now();
        return Optional.of(entry.instance);
    }

    /** Finds an instance without extending its lease. Intended for diagnostics/maintenance only. */
    public Optional<ItemInstance> peek(UUID id) {
        Entry entry = instances.get(id);
        return entry == null ? Optional.empty() : Optional.of(entry.instance);
    }

    public int size() { return instances.size(); }

    public long countByDefinition(NamespacedId definitionId) {
        if (definitionId == null) return 0L;
        return instances.values().stream()
                .filter(entry -> entry.instance.definition().source().id().equals(definitionId))
                .count();
    }

    public boolean hasDefinition(NamespacedId definitionId) {
        if (definitionId == null) return false;
        return instances.values().stream()
                .anyMatch(entry -> entry.instance.definition().source().id().equals(definitionId));
    }

    /**
     * Diagnostic view of the currently cached/recent instances. This is not a global ownership list;
     * persisted carriers that are not presently cached cannot appear here.
     */
    public List<Snapshot> snapshots() {
        return instances.values().stream()
                .map(entry -> new Snapshot(
                        entry.instance.id(),
                        entry.instance.definition().source().id(),
                        entry.createdAtMillis,
                        entry.lastAccessMillis,
                        entry.instance.state().dirty(),
                        entry.instance.state().persistentSnapshot()
                ))
                .sorted(java.util.Comparator.comparing(snapshot -> snapshot.id().toString()))
                .toList();
    }

    /**
     * Evicts clean cache entries whose access lease expired.
     * Dirty state is never discarded, even when old, because it still needs a platform flush.
     */
    public int evictIdle(long maxIdleMillis) {
        if (maxIdleMillis < 0L) throw new IllegalArgumentException("maxIdleMillis must be >= 0");
        long now = now();
        int removed = 0;
        for (var entry : instances.entrySet()) {
            Entry value = entry.getValue();
            long idle = Math.max(0L, now - value.lastAccessMillis);
            if (idle < maxIdleMillis || value.instance.state().dirty()) continue;
            if (instances.remove(entry.getKey(), value)) removed++;
        }
        return removed;
    }

    public void remove(UUID id) { instances.remove(id); }

    private long now() { return clock.getAsLong(); }

    public record Snapshot(
            UUID id,
            NamespacedId definitionId,
            long createdAtMillis,
            long lastAccessMillis,
            boolean dirty,
            Map<String, Object> state
    ) {
        public Snapshot {
            state = state == null ? Map.of() : Map.copyOf(state);
        }
    }

    private static final class Entry {
        private final ItemInstance instance;
        private final long createdAtMillis;
        private volatile long lastAccessMillis;

        private Entry(ItemInstance instance, long createdAtMillis, long lastAccessMillis) {
            this.instance = instance;
            this.createdAtMillis = createdAtMillis;
            this.lastAccessMillis = lastAccessMillis;
        }
    }
}
