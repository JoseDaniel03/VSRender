package com.virgolandstudio.items.core.runtime;

import com.virgolandstudio.items.api.NamespacedId;
import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.core.instance.ItemInstance;
import com.virgolandstudio.items.core.instance.ItemInstanceRegistry;
import com.virgolandstudio.items.core.registry.BuiltinMaintenanceSchemas;
import com.virgolandstudio.items.core.registry.BuiltinSimulationSchemas;
import com.virgolandstudio.items.core.state.StateSlot;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.DoubleSupplier;

/**
 * Applies authored degradation and mechanical-failure risk only after a confirmed successful use.
 * Neither dry-fire nor rejected semantic actions should call this service.
 */
public final class ItemDegradationService {
    private static final NamespacedId WEAR = NamespacedId.parse("virgoland:wear");

    private final ItemInstanceRegistry instances;
    private final ItemMechanicalFailureService failures;

    public ItemDegradationService(ItemInstanceRegistry instances) {
        this.instances = Objects.requireNonNull(instances, "instances");
        this.failures = new ItemMechanicalFailureService(instances);
    }

    /** Deterministic constructor for tests/simulation; production uses ThreadLocalRandom. */
    public ItemDegradationService(ItemInstanceRegistry instances, DoubleSupplier failureRandom) {
        this.instances = Objects.requireNonNull(instances, "instances");
        this.failures = new ItemMechanicalFailureService(instances, failureRandom);
    }

    /**
     * Returns true when the item owns a Degradation component and that profile was applied.
     * Missing semantic state is ignored, so authors can opt into only the dimensions they want.
     * Mechanical failure is evaluated after every confirmed successful use even when Degradation
     * is absent, because a Mechanical Failure profile may intentionally own only a base chance.
     */
    public boolean applySuccessfulUse(UUID instanceId) {
        ItemInstance instance = instances.find(instanceId).orElse(null);
        if (instance == null) return false;

        boolean appliedProfile = false;
        synchronized (instance) {
            ComponentConfig profile = component(instance, BuiltinMaintenanceSchemas.DEGRADATION);
            if (profile != null) {
                addClamped(instance, "wear", directNumber(profile, "wearPerUse", 0), 0, maximum(instance, WEAR, 100));
                addClamped(instance, "dirt", directNumber(profile, "dirtPerUse", 0), 0,
                        maximum(instance, BuiltinMaintenanceSchemas.DIRT, 100));
                addClamped(instance, "rust", directNumber(profile, "rustPerUse", 0), 0,
                        maximum(instance, BuiltinMaintenanceSchemas.RUST, 100));
                addClamped(instance, "lubrication", -directNumber(profile, "lubricationLossPerUse", 0), 0,
                        maximum(instance, BuiltinMaintenanceSchemas.LUBRICATION, 100));
                addClamped(instance, "durability", -directNumber(profile, "durabilityDamagePerUse", 0), 0,
                        maximum(instance, BuiltinSimulationSchemas.DURABILITY, 100));
                appliedProfile = true;
            }
        }

        // Evaluate after degradation so the failure chance reflects the just-updated condition.
        failures.evaluate(instanceId);
        return appliedProfile;
    }

    private static void addClamped(ItemInstance instance, String key, double delta, double min, double max) {
        if (!Double.isFinite(delta) || delta == 0) return;
        StateSlot slot = instance.state().layout().find(key);
        if (slot == null) return;
        double current = instance.state().getDouble(slot, 0);
        instance.state().set(slot, clamp(current + delta, min, max));
    }

    private static double maximum(ItemInstance instance, NamespacedId componentId, double fallback) {
        ComponentConfig component = component(instance, componentId);
        if (component == null) return fallback;
        if (componentId.equals(WEAR)) return Math.max(1.0e-9, nestedNumber(component, "max", fallback));
        return Math.max(1.0e-9, directNumber(component, "max", fallback));
    }

    private static ComponentConfig component(ItemInstance instance, NamespacedId id) {
        for (ComponentConfig component : instance.definition().source().components()) {
            if (component.type().equals(id)) return component;
        }
        return null;
    }

    private static double directNumber(ComponentConfig component, String key, double fallback) {
        return number(component.values().get(key), fallback);
    }

    private static double nestedNumber(ComponentConfig component, String key, double fallback) {
        Object config = component.values().get("config");
        if (config instanceof Map<?, ?> map) return number(map.get(key), fallback);
        return number(component.values().get(key), fallback);
    }

    private static double number(Object raw, double fallback) {
        if (raw instanceof Number number) return Math.max(0, number.doubleValue());
        if (raw == null) return fallback;
        try {
            return Math.max(0, Double.parseDouble(String.valueOf(raw)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }
}
