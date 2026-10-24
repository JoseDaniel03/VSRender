package com.virgolandstudio.items.core.registry;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.core.compile.CompiledItemDefinition;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic active/retired definition registry.
 *
 * <p>Retired definitions are tombstones for already-existing ItemStacks. They are hidden from
 * normal catalogs and cannot be used for new authoring/give flows through {@link #find}, but the
 * persistence codec can still resolve them through {@link #resolve}. Re-publishing the same id
 * automatically replaces its tombstone.</p>
 */
public final class ItemRegistry {
    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>(new Snapshot(Map.of(), Map.of()));

    /** Active definitions only. */
    public Optional<CompiledItemDefinition> find(NamespacedId id) {
        return Optional.ofNullable(snapshot.get().active().get(id));
    }

    /** Active definition first, then a retired tombstone for old persisted stacks. */
    public Optional<CompiledItemDefinition> resolve(NamespacedId id) {
        Snapshot current = snapshot.get();
        CompiledItemDefinition active = current.active().get(id);
        return Optional.ofNullable(active != null ? active : current.retired().get(id));
    }

    public Optional<CompiledItemDefinition> findRetired(NamespacedId id) {
        return Optional.ofNullable(snapshot.get().retired().get(id));
    }

    public Collection<CompiledItemDefinition> all() {
        return List.copyOf(snapshot.get().active().values());
    }

    public Collection<CompiledItemDefinition> allRetired() {
        return List.copyOf(snapshot.get().retired().values());
    }

    /** Publishes as active and removes any old tombstone with the same id. */
    public void publish(CompiledItemDefinition definition) {
        while (true) {
            Snapshot current = snapshot.get();
            var active = new ConcurrentHashMap<>(current.active());
            var retired = new ConcurrentHashMap<>(current.retired());
            active.put(definition.source().id(), definition);
            retired.remove(definition.source().id());
            Snapshot next = immutable(active, retired);
            if (snapshot.compareAndSet(current, next)) return;
        }
    }

    /** Publishes a tombstone only when no active definition with that id exists. */
    public void publishRetired(CompiledItemDefinition definition) {
        while (true) {
            Snapshot current = snapshot.get();
            NamespacedId id = definition.source().id();
            if (current.active().containsKey(id)) {
                throw new IllegalStateException("Cannot retire definition while active: " + id);
            }
            var retired = new ConcurrentHashMap<>(current.retired());
            retired.put(id, definition);
            Snapshot next = new Snapshot(current.active(), Map.copyOf(retired));
            if (snapshot.compareAndSet(current, next)) return;
        }
    }

    /** Moves an active definition into the retired/tombstone set atomically. */
    public Optional<CompiledItemDefinition> retire(NamespacedId id) {
        while (true) {
            Snapshot current = snapshot.get();
            CompiledItemDefinition existing = current.active().get(id);
            if (existing == null) return Optional.empty();
            var active = new ConcurrentHashMap<>(current.active());
            var retired = new ConcurrentHashMap<>(current.retired());
            active.remove(id);
            retired.put(id, existing);
            if (snapshot.compareAndSet(current, immutable(active, retired))) return Optional.of(existing);
        }
    }

    /** Reactivates a tombstone without recompilation. */
    public Optional<CompiledItemDefinition> reactivate(NamespacedId id) {
        while (true) {
            Snapshot current = snapshot.get();
            if (current.active().containsKey(id)) return Optional.of(current.active().get(id));
            CompiledItemDefinition existing = current.retired().get(id);
            if (existing == null) return Optional.empty();
            var active = new ConcurrentHashMap<>(current.active());
            var retired = new ConcurrentHashMap<>(current.retired());
            retired.remove(id);
            active.put(id, existing);
            if (snapshot.compareAndSet(current, immutable(active, retired))) return Optional.of(existing);
        }
    }

    /** Hard-removes an active definition. Existing callers retain the old semantics. */
    public Optional<CompiledItemDefinition> remove(NamespacedId id) {
        while (true) {
            Snapshot current = snapshot.get();
            CompiledItemDefinition existing = current.active().get(id);
            if (existing == null) return Optional.empty();
            var active = new ConcurrentHashMap<>(current.active());
            active.remove(id);
            Snapshot next = new Snapshot(Map.copyOf(active), current.retired());
            if (snapshot.compareAndSet(current, next)) return Optional.of(existing);
        }
    }

    public Optional<CompiledItemDefinition> removeRetired(NamespacedId id) {
        while (true) {
            Snapshot current = snapshot.get();
            CompiledItemDefinition existing = current.retired().get(id);
            if (existing == null) return Optional.empty();
            var retired = new ConcurrentHashMap<>(current.retired());
            retired.remove(id);
            Snapshot next = new Snapshot(current.active(), Map.copyOf(retired));
            if (snapshot.compareAndSet(current, next)) return Optional.of(existing);
        }
    }

    private static Snapshot immutable(
            Map<NamespacedId, CompiledItemDefinition> active,
            Map<NamespacedId, CompiledItemDefinition> retired
    ) {
        return new Snapshot(Map.copyOf(active), Map.copyOf(retired));
    }

    private record Snapshot(
            Map<NamespacedId, CompiledItemDefinition> active,
            Map<NamespacedId, CompiledItemDefinition> retired
    ) {}
}
