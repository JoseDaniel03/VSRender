package com.virgolandstudio.items.core.compile;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.behavior.BehaviorDefinition;
import com.virgolandstudio.items.api.behavior.Trigger;
import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.api.definition.ItemDefinition;
import com.virgolandstudio.items.api.schema.ComponentSchema;
import com.virgolandstudio.items.api.state.StateFieldDefinition;
import com.virgolandstudio.items.api.state.StateType;
import com.virgolandstudio.items.core.accessory.CompiledAttachmentSlots;
import com.virgolandstudio.items.core.ammo.AmmoSystemCompiler;
import com.virgolandstudio.items.core.ammo.CompiledAmmoSystem;
import com.virgolandstudio.items.core.behavior.BehaviorCompiler;
import com.virgolandstudio.items.core.behavior.BuiltinSimulationBehaviorTypes;
import com.virgolandstudio.items.core.behavior.BuiltinTimedProcessBehaviorTypes;
import com.virgolandstudio.items.core.behavior.BuiltinVisualBehaviorTypes;
import com.virgolandstudio.items.core.behavior.CompiledBehavior;
import com.virgolandstudio.items.core.inventory.CompiledInventoryFootprint;
import com.virgolandstudio.items.core.ranged.CompiledFireControl;
import com.virgolandstudio.items.core.ranged.CompiledRangedSystem;
import com.virgolandstudio.items.core.ranged.ImpactProfileRegistry;
import com.virgolandstudio.items.core.ranged.MaterialGroupRegistry;
import com.virgolandstudio.items.core.ranged.RangedSystemCompiler;
import com.virgolandstudio.items.core.registry.BuiltinDualWieldSchemas;
import com.virgolandstudio.items.core.registry.BuiltinMaintenanceSchemas;
import com.virgolandstudio.items.core.registry.BuiltinSchemas;
import com.virgolandstudio.items.core.registry.BuiltinSimulationSchemas;
import com.virgolandstudio.items.core.registry.BuiltinVisualSchemas;
import com.virgolandstudio.items.core.registry.ComponentSchemaRegistry;
import com.virgolandstudio.items.core.reload.ReloadSequence;
import com.virgolandstudio.items.core.reload.ReloadSequenceCompiler;
import com.virgolandstudio.items.core.resource.CompiledEnergy;
import com.virgolandstudio.items.core.resource.EnergyCompiler;
import com.virgolandstudio.items.core.state.MaintenanceStateDefinitions;
import com.virgolandstudio.items.core.state.SimulationStateDefinitions;
import com.virgolandstudio.items.core.state.StateLayout;
import com.virgolandstudio.items.core.state.StateSlot;
import com.virgolandstudio.items.core.world.CompiledWorldInteractionSystem;
import com.virgolandstudio.items.core.world.WorldInteractionCompiler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Compiles validated API item definitions into immutable runtime data. */
public final class DefinitionCompiler {
    private final ComponentSchemaRegistry schemas;
    private final BehaviorCompiler behaviors;
    private final AtomicInteger nextRuntimeId = new AtomicInteger(1);
    private final ReloadSequenceCompiler reloadSequences;
    private final AmmoSystemCompiler ammoSystems;
    private final RangedSystemCompiler rangedSystems;
    private final WorldInteractionCompiler worldInteractions;
    private final EnergyCompiler energySystems;

    public DefinitionCompiler(ComponentSchemaRegistry schemas, BehaviorCompiler behaviors) {
        this(schemas, behaviors, new MaterialGroupRegistry(), new ImpactProfileRegistry());
    }

    public DefinitionCompiler(ComponentSchemaRegistry schemas, BehaviorCompiler behaviors, MaterialGroupRegistry materialGroups) {
        this(schemas, behaviors, materialGroups, new ImpactProfileRegistry());
    }

    public DefinitionCompiler(
            ComponentSchemaRegistry schemas,
            BehaviorCompiler behaviors,
            MaterialGroupRegistry materialGroups,
            ImpactProfileRegistry impactProfiles
    ) {
        this.schemas = schemas;
        this.behaviors = behaviors;
        if (schemas.find(BuiltinSimulationSchemas.STATE).isEmpty()) {
            BuiltinSimulationSchemas.registerAll(schemas);
        }
        BuiltinMaintenanceSchemas.registerAll(schemas);
        BuiltinVisualSchemas.registerAll(schemas);
        BuiltinDualWieldSchemas.registerAll(schemas);
        BuiltinSimulationBehaviorTypes.registerAll(behaviors);
        BuiltinTimedProcessBehaviorTypes.registerAll(behaviors);
        BuiltinVisualBehaviorTypes.registerAll(behaviors);
        this.reloadSequences = new ReloadSequenceCompiler(behaviors);
        this.ammoSystems = new AmmoSystemCompiler();
        this.rangedSystems = new RangedSystemCompiler(materialGroups, impactProfiles);
        this.worldInteractions = new WorldInteractionCompiler();
        this.energySystems = new EnergyCompiler();
    }

    public CompiledItemDefinition compile(ItemDefinition definition) {
        List<String> errors = new ArrayList<>();
        Set<NamespacedId> componentTypes = new HashSet<>();

        for (ComponentConfig component : definition.components()) {
            if (!componentTypes.add(component.type())) {
                errors.add("Duplicate component: " + component.type());
                continue;
            }
            ComponentSchema schema = schemas.find(component.type()).orElse(null);
            if (schema == null) {
                errors.add("Unknown component: " + component.type());
                continue;
            }
            errors.addAll(ComponentSchemaValidator.validate(schema, component.values()));
            for (NamespacedId required : schema.requires()) {
                boolean present = definition.components().stream().anyMatch(c -> c.type().equals(required));
                if (!present) errors.add(component.type() + " requires " + required);
            }
            for (NamespacedId conflict : schema.conflicts()) {
                boolean present = definition.components().stream().anyMatch(c -> c.type().equals(conflict));
                if (present) errors.add(component.type() + " conflicts with " + conflict);
            }
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);

        List<StateFieldDefinition> stateDefinitions = new ArrayList<>(definition.state());
        if (componentTypes.contains(BuiltinSchemas.ATTACHMENT_SLOTS)
                && stateDefinitions.stream().noneMatch(field -> field.key().equals("attachment_loadout"))) {
            stateDefinitions.add(new StateFieldDefinition("attachment_loadout", StateType.STRING, "", true));
        }
        SimulationStateDefinitions.contribute(definition, stateDefinitions);
        MaintenanceStateDefinitions.contribute(definition, stateDefinitions);
        StateLayout stateLayout = StateLayout.compile(stateDefinitions);
        CompiledEnergy energy = energySystems.compile(definition, stateLayout);

        EnumMap<Trigger, List<CompiledBehavior>> compiledBehaviors = new EnumMap<>(Trigger.class);
        for (BehaviorDefinition behaviorDefinition : definition.behaviors()) {
            CompiledBehavior compiled = behaviors.compile(behaviorDefinition);
            compiledBehaviors.computeIfAbsent(behaviorDefinition.trigger(), ignored -> new ArrayList<>()).add(compiled);
        }
        compiledBehaviors.replaceAll((trigger, list) -> List.copyOf(list));

        CompiledAmmoSystem ammo = ammoSystems.compile(definition);
        CompiledRangedSystem ranged = rangedSystems.compile(definition);
        if (ranged != null && ranged.fireControl() != null && ranged.fireControl().availableModes().size() > 1) {
            CompiledFireControl fireControl = ranged.fireControl();
            String modeStateKey = fireControl.modeStateKey();
            StateSlot modeSlot = stateLayout.find(modeStateKey);
            if (modeSlot == null) {
                throw new ValidationException(List.of("FireControl.modeStateKey references missing state field: " + modeStateKey));
            }
            if (modeSlot.type() != StateType.STRING) {
                throw new ValidationException(List.of("FireControl.modeStateKey must reference STRING state: " + modeStateKey));
            }
        }

        CompiledWorldInteractionSystem worldInteraction = worldInteractions.compile(definition);

        CompiledInventoryFootprint inventoryFootprint = null;
        for (ComponentConfig component : definition.components()) {
            if (!component.type().equals(BuiltinSchemas.INVENTORY_FOOTPRINT)) continue;
            int slots = intValue(component.values().get("slots"), 1);
            boolean mustBeConsecutive = boolValue(component.values().get("mustBeConsecutive"), true);
            boolean allowHotbar = boolValue(component.values().get("allowHotbar"), true);
            inventoryFootprint = new CompiledInventoryFootprint(slots, mustBeConsecutive, allowHotbar);
            break;
        }

        ReloadSequence reloadSequence = null;
        for (ComponentConfig component : definition.components()) {
            if (!component.type().equals(BuiltinSchemas.RELOAD)) continue;
            Object sequence = component.values().get("sequence");
            if (sequence != null) reloadSequence = reloadSequences.compile(sequence);
            break;
        }

        CompiledAttachmentSlots attachmentSlots = null;
        for (ComponentConfig component : definition.components()) {
            if (!component.type().equals(BuiltinSchemas.ATTACHMENT_SLOTS)) continue;
            Object rawSlots = component.values().get("slots");
            List<String> slots = new ArrayList<>();
            if (rawSlots instanceof Iterable<?> iterable) {
                for (Object value : iterable) if (value != null) slots.add(String.valueOf(value));
            } else if (rawSlots != null) {
                slots.add(String.valueOf(rawSlots));
            }
            try {
                attachmentSlots = new CompiledAttachmentSlots(slots);
            } catch (IllegalArgumentException ex) {
                throw new ValidationException(List.of(ex.getMessage()));
            }
            break;
        }

        return new CompiledItemDefinition(
                nextRuntimeId.getAndIncrement(),
                definition,
                Set.copyOf(componentTypes),
                definition.baseStats(),
                stateLayout,
                Map.copyOf(compiledBehaviors),
                ammo,
                reloadSequence,
                ranged,
                worldInteraction,
                energy,
                inventoryFootprint,
                attachmentSlots
        );
    }

    private static int intValue(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number number) return number.intValue();
        return Integer.parseInt(value.toString());
    }

    private static boolean boolValue(Object value, boolean fallback) {
        if (value == null) return fallback;
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(value.toString());
    }
}
