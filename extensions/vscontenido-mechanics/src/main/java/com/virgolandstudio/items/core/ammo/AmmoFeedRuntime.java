package com.virgolandstudio.items.core.ammo;

import com.virgolandstudio.items.api.ammo.FeedMechanismKind;

import java.util.Objects;

/**
 * Pure-core ammunition/feed state machine. No Bukkit types, inventory access or schedulers.
 * It intentionally models only feed/fire semantics; reload timing remains in ReloadRuntime.
 */
public final class AmmoFeedRuntime {
    private final CompiledAmmoSystem spec;
    private final AmmoContainerState container;
    private int chambered;

    public AmmoFeedRuntime(CompiledAmmoSystem spec, AmmoContainerState container, int chambered) {
        this.spec = Objects.requireNonNull(spec, "spec");
        this.container = Objects.requireNonNull(container, "container");
        if (container.mode() != spec.storageMode()) {
            throw new IllegalArgumentException("container storage mode does not match compiled ammo spec");
        }
        if (container.capacity() != spec.capacity()
                && spec.containerKind() != com.virgolandstudio.items.api.ammo.AmmoContainerKind.DETACHABLE_MAGAZINE) {
            throw new IllegalArgumentException("embedded container capacity does not match compiled ammo spec");
        }
        if (chambered < 0 || chambered > spec.chamberCapacity()) throw new IllegalArgumentException("chambered out of range");
        this.chambered = chambered;
    }

    public CompiledAmmoSystem spec() { return spec; }
    public AmmoContainerState container() { return container; }
    public int chambered() { return chambered; }

    /** Consume the ready-to-fire resource only. Feeding the next round is explicit. */
    public boolean fire() {
        return switch (spec.feedMechanism()) {
            case AUTO_CHAMBER, MANUAL_CHAMBER -> fireSeparateChamber();
            case ROTATING -> fireRotatingSlot();
            case DIRECT -> fireDirectSlot();
            case ENERGY_DIRECT -> container.consumeOne();
        };
    }

    /** Move one unit into a separate chamber where the feed mode supports it. */
    public boolean feedOne() {
        if (spec.feedMechanism() != FeedMechanismKind.AUTO_CHAMBER
                && spec.feedMechanism() != FeedMechanismKind.MANUAL_CHAMBER) return false;
        if (chambered >= spec.chamberCapacity()) return false;
        if (!container.consumeOne()) return false;
        chambered++;
        return true;
    }

    /**
     * Convenience for self-loading actions. If a loaded container was just inserted into an empty
     * weapon, prime the first round before firing; after a successful shot, feed the next one.
     */
    public boolean fireAndCycle() {
        if (spec.feedMechanism() == FeedMechanismKind.AUTO_CHAMBER && chambered <= 0) feedOne();
        if (!fire()) return false;
        if (spec.feedMechanism() == FeedMechanismKind.AUTO_CHAMBER) feedOne();
        return true;
    }

    public boolean loadOneIntoContainer() {
        return container.loadOne();
    }

    public boolean loadSlot(int index) {
        return container.loadSlot(index);
    }

    public void clearSlot(int index) {
        container.clearSlot(index);
    }

    private boolean fireSeparateChamber() {
        if (chambered <= 0) return false;
        chambered--;
        return true;
    }

    private boolean fireRotatingSlot() {
        boolean fired = container.consumeCurrentSlot();
        container.advanceCursor();
        return fired;
    }

    private boolean fireDirectSlot() {
        if (container.consumeCurrentSlot()) return true;
        for (int i = 1; i < container.capacity(); i++) {
            container.advanceCursor();
            if (container.consumeCurrentSlot()) return true;
        }
        return false;
    }
}
