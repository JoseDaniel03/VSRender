package com.virgolandstudio.items.core.ammo;

import com.virgolandstudio.items.api.ammo.AmmoContainerKind;
import com.virgolandstudio.items.api.ammo.AmmoStorageMode;
import com.virgolandstudio.items.api.ammo.FeedMechanismKind;
import com.virgolandstudio.items.api.ammo.ReloadMechanismKind;

import java.util.Objects;

/** Immutable hot-runtime description of an item's ammunition architecture. */
public record CompiledAmmoSystem(
        String ammoType,
        AmmoContainerKind containerKind,
        AmmoStorageMode storageMode,
        int capacity,
        boolean detachable,
        FeedMechanismKind feedMechanism,
        int chamberCapacity,
        ReloadMechanismKind reloadMechanism
) {
    public CompiledAmmoSystem {
        Objects.requireNonNull(ammoType, "ammoType");
        Objects.requireNonNull(containerKind, "containerKind");
        Objects.requireNonNull(storageMode, "storageMode");
        Objects.requireNonNull(feedMechanism, "feedMechanism");
        Objects.requireNonNull(reloadMechanism, "reloadMechanism");
        if (ammoType.isBlank()) throw new IllegalArgumentException("ammoType cannot be blank");
        if (capacity < 1) throw new IllegalArgumentException("capacity must be >= 1");
        if (chamberCapacity < 0) throw new IllegalArgumentException("chamberCapacity must be >= 0");
    }
}
