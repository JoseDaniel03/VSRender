package com.virgolandstudio.items.core.runtime;

import com.virgolandstudio.items.core.compile.CompiledItemDefinition;
import com.virgolandstudio.items.core.registry.BuiltinMaintenanceSchemas;

import java.util.Objects;
import java.util.Set;

/**
 * Provider-neutral maintenance capability discovery for compiled Items.
 *
 * <p>Eligibility is derived from compiled persistent state instead of item family, weapon tags or
 * platform naming. A tool, firearm, machine part or future object can therefore become a
 * maintenance target by composing the same state-producing components.</p>
 */
public final class ItemMaintenanceCapabilities {
    private static final Set<String> TARGET_STATE = Set.of(
            "durability",
            "wear",
            "dirt",
            "rust",
            "lubrication",
            "mechanical_jammed"
    );

    private ItemMaintenanceCapabilities() {}

    public static boolean supportsMaintenance(CompiledItemDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return TARGET_STATE.stream().anyMatch(key -> definition.stateLayout().find(key) != null);
    }

    public static boolean isSupply(CompiledItemDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        return definition.componentTypes().contains(BuiltinMaintenanceSchemas.MAINTENANCE_SUPPLY);
    }
}
