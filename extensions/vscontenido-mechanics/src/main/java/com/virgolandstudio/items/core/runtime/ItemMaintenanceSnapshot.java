package com.virgolandstudio.items.core.runtime;

import com.virgolandstudio.items.api.NamespacedId;

import java.util.UUID;

/** Immutable maintenance view derived from independent persistent item state. */
public record ItemMaintenanceSnapshot(
        UUID instanceId,
        NamespacedId definitionId,
        double durability,
        double durabilityMax,
        double wear,
        double wearMax,
        double dirt,
        double dirtMax,
        double rust,
        double rustMax,
        double lubrication,
        double lubricationMax,
        double condition,
        boolean operational,
        boolean jammed,
        long failureCount,
        long maintenanceCount,
        long lastMaintenanceMillis,
        double mechanicalFailureChance
) {}
