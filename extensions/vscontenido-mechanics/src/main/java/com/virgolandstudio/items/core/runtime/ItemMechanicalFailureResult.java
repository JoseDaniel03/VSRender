package com.virgolandstudio.items.core.runtime;

import java.util.UUID;

/** Immutable outcome of one authored mechanical-failure check after a confirmed successful use. */
public record ItemMechanicalFailureResult(
        UUID instanceId,
        double chance,
        double roll,
        boolean rolled,
        boolean failed,
        boolean alreadyJammed,
        ItemMaintenanceSnapshot snapshot
) {
}
