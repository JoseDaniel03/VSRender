package com.virgolandstudio.items.core.accessory;

import com.virgolandstudio.items.api.NamespacedId;

import java.util.Objects;
import java.util.UUID;

/** Persistent identity of one part installed in one weapon attachment slot. */
public record InstalledAccessory(
        NamespacedId accessoryDefinitionId,
        NamespacedId itemDefinitionId,
        UUID itemInstanceId
) {
    public InstalledAccessory {
        Objects.requireNonNull(accessoryDefinitionId, "accessoryDefinitionId");
        Objects.requireNonNull(itemDefinitionId, "itemDefinitionId");
        Objects.requireNonNull(itemInstanceId, "itemInstanceId");
    }
}
