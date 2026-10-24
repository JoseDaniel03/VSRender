package com.virgolandstudio.items.core.runtime;

import java.util.UUID;

/** Persistent-use view of a consumable Item instance. A negative remaining value means unbounded. */
public record ItemConsumableSnapshot(
        UUID instanceId,
        ItemConsumableProfile profile,
        long usesRemaining,
        boolean depleted
) {}
