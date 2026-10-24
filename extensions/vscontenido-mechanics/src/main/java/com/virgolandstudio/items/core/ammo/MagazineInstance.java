package com.virgolandstudio.items.core.ammo;

import com.virgolandstudio.items.api.NamespacedId;

import java.util.Objects;
import java.util.UUID;

/** Independent detachable-ammo container. It can survive being removed from a weapon. */
public final class MagazineInstance {
    private final UUID id;
    private final NamespacedId definitionId;
    private final String ammoType;
    private final AmmoContainerState storage;

    public MagazineInstance(UUID id, NamespacedId definitionId, String ammoType, int capacity, int rounds) {
        this.id = Objects.requireNonNull(id, "id");
        this.definitionId = Objects.requireNonNull(definitionId, "definitionId");
        this.ammoType = Objects.requireNonNull(ammoType, "ammoType");
        if (ammoType.isBlank()) throw new IllegalArgumentException("ammoType cannot be blank");
        this.storage = AmmoContainerState.count(capacity, rounds);
    }

    public UUID id() { return id; }
    public NamespacedId definitionId() { return definitionId; }
    public String ammoType() { return ammoType; }
    public int capacity() { return storage.capacity(); }
    public int rounds() { return (int) Math.round(storage.available()); }
    public boolean dirty() { return storage.dirty(); }
    public AmmoContainerState storage() { return storage; }

    public boolean takeRound() { return storage.consumeOne(); }

    public int space() { return Math.max(0, capacity() - rounds()); }

    /** Loads up to {@code units} discrete rounds and returns how many were accepted. */
    public int addRounds(int units) {
        if (units < 0) throw new IllegalArgumentException("units must be >= 0");
        int accepted = Math.min(units, space());
        if (accepted > 0) storage.setAmount(rounds() + accepted);
        return accepted;
    }

    public boolean addRound() { return addRounds(1) == 1; }

    /** Removes up to {@code units} rounds and returns how many were removed. */
    public int removeRounds(int units) {
        if (units < 0) throw new IllegalArgumentException("units must be >= 0");
        int removed = Math.min(units, rounds());
        if (removed > 0) storage.setAmount(rounds() - removed);
        return removed;
    }

    public boolean removeRound() { return removeRounds(1) == 1; }

    public void setRounds(int rounds) { storage.setAmount(rounds); }

    public void markClean() { storage.markClean(); }
}
