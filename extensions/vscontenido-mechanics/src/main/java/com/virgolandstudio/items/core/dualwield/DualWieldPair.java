package com.virgolandstudio.items.core.dualwield;

import java.util.Objects;
import java.util.UUID;

/** Immutable pairing result. Each hand keeps its own ItemInstance UUID and therefore its own state. */
public record DualWieldPair(
        UUID mainHandInstanceId,
        UUID offHandInstanceId,
        DualWieldPolicy mainPolicy,
        DualWieldPolicy offPolicy
) {
    public DualWieldPair {
        Objects.requireNonNull(mainHandInstanceId, "mainHandInstanceId");
        Objects.requireNonNull(offHandInstanceId, "offHandInstanceId");
        Objects.requireNonNull(mainPolicy, "mainPolicy");
        Objects.requireNonNull(offPolicy, "offPolicy");
        if (mainHandInstanceId.equals(offHandInstanceId)) {
            throw new IllegalArgumentException("dual wield requires two distinct item instances");
        }
        if (!mainPolicy.compatibleWith(offPolicy)) {
            throw new IllegalArgumentException("dual wield policies are not compatible");
        }
    }

    /** Pair-level policy is conservative: either item may forbid chained reload of the other hand. */
    public DualWieldPolicy.ReloadPolicy reloadPolicy() {
        return mainPolicy.reloadPolicy() == DualWieldPolicy.ReloadPolicy.ACTIVE_HAND_ONLY
                || offPolicy.reloadPolicy() == DualWieldPolicy.ReloadPolicy.ACTIVE_HAND_ONLY
                ? DualWieldPolicy.ReloadPolicy.ACTIVE_HAND_ONLY
                : DualWieldPolicy.ReloadPolicy.SEQUENTIAL;
    }

    public double spreadMultiplier() {
        return mainPolicy.spreadMultiplier() * offPolicy.spreadMultiplier();
    }

    public double recoilMultiplier() {
        return mainPolicy.recoilMultiplier() * offPolicy.recoilMultiplier();
    }
}
