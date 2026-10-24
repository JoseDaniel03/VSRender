package com.virgolandstudio.items.core.compile;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.behavior.Trigger;
import com.virgolandstudio.items.api.definition.ItemDefinition;
import com.virgolandstudio.items.core.accessory.CompiledAttachmentSlots;
import com.virgolandstudio.items.core.ammo.CompiledAmmoSystem;
import com.virgolandstudio.items.core.behavior.CompiledBehavior;
import com.virgolandstudio.items.core.inventory.CompiledInventoryFootprint;
import com.virgolandstudio.items.core.ranged.CompiledRangedSystem;
import com.virgolandstudio.items.core.reload.ReloadSequence;
import com.virgolandstudio.items.core.resource.CompiledEnergy;
import com.virgolandstudio.items.core.state.StateLayout;
import com.virgolandstudio.items.core.world.CompiledWorldInteractionSystem;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record CompiledItemDefinition(
        int runtimeId,
        ItemDefinition source,
        Set<NamespacedId> componentTypes,
        Map<String, Double> resolvedBaseStats,
        StateLayout stateLayout,
        Map<Trigger, List<CompiledBehavior>> behaviors,
        CompiledAmmoSystem ammoSystem,
        ReloadSequence reloadSequence,
        CompiledRangedSystem rangedSystem,
        CompiledWorldInteractionSystem worldInteractionSystem,
        CompiledEnergy energySystem,
        CompiledInventoryFootprint inventoryFootprint,
        CompiledAttachmentSlots attachmentSlots
) {
    public CompiledItemDefinition(
            int runtimeId,
            ItemDefinition source,
            Set<NamespacedId> componentTypes,
            Map<String, Double> resolvedBaseStats,
            StateLayout stateLayout,
            Map<Trigger, List<CompiledBehavior>> behaviors,
            CompiledAmmoSystem ammoSystem,
            ReloadSequence reloadSequence,
            CompiledRangedSystem rangedSystem
    ) {
        this(runtimeId, source, componentTypes, resolvedBaseStats, stateLayout, behaviors,
                ammoSystem, reloadSequence, rangedSystem, null, null, null, null);
    }

    public CompiledItemDefinition(
            int runtimeId,
            ItemDefinition source,
            Set<NamespacedId> componentTypes,
            Map<String, Double> resolvedBaseStats,
            StateLayout stateLayout,
            Map<Trigger, List<CompiledBehavior>> behaviors,
            CompiledAmmoSystem ammoSystem,
            ReloadSequence reloadSequence,
            CompiledRangedSystem rangedSystem,
            CompiledWorldInteractionSystem worldInteractionSystem
    ) {
        this(runtimeId, source, componentTypes, resolvedBaseStats, stateLayout, behaviors,
                ammoSystem, reloadSequence, rangedSystem, worldInteractionSystem, null, null, null);
    }

    public CompiledItemDefinition(
            int runtimeId,
            ItemDefinition source,
            Set<NamespacedId> componentTypes,
            Map<String, Double> resolvedBaseStats,
            StateLayout stateLayout,
            Map<Trigger, List<CompiledBehavior>> behaviors,
            CompiledAmmoSystem ammoSystem,
            ReloadSequence reloadSequence,
            CompiledRangedSystem rangedSystem,
            CompiledWorldInteractionSystem worldInteractionSystem,
            CompiledEnergy energySystem
    ) {
        this(runtimeId, source, componentTypes, resolvedBaseStats, stateLayout, behaviors,
                ammoSystem, reloadSequence, rangedSystem, worldInteractionSystem, energySystem, null, null);
    }

    public CompiledItemDefinition {
        componentTypes = Set.copyOf(componentTypes);
        resolvedBaseStats = Map.copyOf(resolvedBaseStats);
        behaviors = Map.copyOf(behaviors);
    }

    public double stat(String key, double fallback) {
        return resolvedBaseStats.getOrDefault(key, fallback);
    }
}
