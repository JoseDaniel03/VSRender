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
import java.util.Optional;
import java.util.UUID;

/**
 * Atomic maintenance/degradation service shared by firearms, melee tools and future machinery.
 *
 * <p>Durability, wear, dirt, rust and lubrication remain independent. Cleaning therefore cannot
 * silently repair structural damage, repairing cannot silently clean an item, and lubrication is
 * not treated as durability. There is no global ticker; callers mutate state only when gameplay
 * events actually happen.</p>
 *
 * <p>The UUID entry points are convenient for platform services. The ItemInstance overloads are
 * intentionally public as well so generic compiled behaviors can operate on the instance already
 * owned by an {@link ExecutionContext} without needing a registry or a platform bridge.</p>
 */
public final class ItemMaintenanceService {
    private static final NamespacedId WEAR = NamespacedId.parse("virgoland:wear");
    private static final NamespacedId MAINTENANCE = NamespacedId.parse("virgoland:maintenance");

    private final ItemInstanceRegistry instances;

    public ItemMaintenanceService(ItemInstanceRegistry instances) {
        this.instances = Objects.requireNonNull(instances, "instances");
    }

    public Optional<ItemMaintenanceSnapshot> snapshot(UUID instanceId) {
        return instances.find(instanceId).map(ItemMaintenanceService::snapshot);
    }

    /** Snapshot an already-resolved instance. Safe to call from BehaviorEngine-owned execution. */
    public static ItemMaintenanceSnapshot snapshot(ItemInstance instance) {
        Objects.requireNonNull(instance, "instance");
        synchronized (instance) {
            return snapshotLocked(instance);
        }
    }

    /** Applies normal usage degradation as one atomic mutation. All deltas are positive magnitudes. */
    public Optional<ItemMaintenanceSnapshot> applyUse(
            UUID instanceId,
            double wearAdded,
            double dirtAdded,
            double lubricationLost
    ) {
        ItemInstance instance = instances.find(instanceId).orElse(null);
        return instance == null ? Optional.empty() : Optional.of(applyUse(instance, wearAdded, dirtAdded, lubricationLost));
    }

    public static ItemMaintenanceSnapshot applyUse(
            ItemInstance instance,
            double wearAdded,
            double dirtAdded,
            double lubricationLost
    ) {
        requireNonNegativeFinite("wearAdded", wearAdded);
        requireNonNegativeFinite("dirtAdded", dirtAdded);
        requireNonNegativeFinite("lubricationLost", lubricationLost);
        return mutate(instance, target -> {
            addClamped(target, "wear", wearAdded, 0, maximum(target, WEAR, 100));
            addClamped(target, "dirt", dirtAdded, 0, maximum(target, BuiltinMaintenanceSchemas.DIRT, 100));
            addClamped(target, "lubrication", -lubricationLost, 0,
                    maximum(target, BuiltinMaintenanceSchemas.LUBRICATION, 100));
        });
    }

    /** Structural damage only. It deliberately does not add dirt, rust or wear. */
    public Optional<ItemMaintenanceSnapshot> damage(UUID instanceId, double amount) {
        ItemInstance instance = instances.find(instanceId).orElse(null);
        return instance == null ? Optional.empty() : Optional.of(damage(instance, amount));
    }

    public static ItemMaintenanceSnapshot damage(ItemInstance instance, double amount) {
        requireNonNegativeFinite("amount", amount);
        return mutate(instance, target -> addClamped(
                target,
                "durability",
                -amount,
                0,
                maximum(target, BuiltinSimulationSchemas.DURABILITY, 100)
        ));
    }

    /** Structural repair only. Dirt/rust/lubrication remain untouched. */
    public Optional<ItemMaintenanceSnapshot> repair(UUID instanceId, double amount) {
        ItemInstance instance = instances.find(instanceId).orElse(null);
        return instance == null ? Optional.empty() : Optional.of(repair(instance, amount));
    }

    public static ItemMaintenanceSnapshot repair(ItemInstance instance, double amount) {
        requireNonNegativeFinite("amount", amount);
        return mutate(instance, target -> addClamped(
                target,
                "durability",
                amount,
                0,
                maximum(target, BuiltinSimulationSchemas.DURABILITY, 100)
        ));
    }

    /** Cleaning according to the item's authored Cleanable profile. Rust removal defaults to zero. */
    public Optional<ItemMaintenanceSnapshot> clean(UUID instanceId) {
        ItemInstance instance = instances.find(instanceId).orElse(null);
        return instance == null ? Optional.empty() : Optional.of(clean(instance));
    }

    public static ItemMaintenanceSnapshot clean(ItemInstance instance) {
        Objects.requireNonNull(instance, "instance");
        synchronized (instance) {
            ComponentConfig cleanable = component(instance, BuiltinMaintenanceSchemas.CLEANABLE);
            if (cleanable == null) return snapshotLocked(instance);
            double dirt = directNumber(cleanable, "dirtPerOperation", 25);
            double rust = directNumber(cleanable, "rustPerOperation", 0);
            addClamped(instance, "dirt", -Math.max(0, dirt), 0,
                    maximum(instance, BuiltinMaintenanceSchemas.DIRT, 100));
            addClamped(instance, "rust", -Math.max(0, rust), 0,
                    maximum(instance, BuiltinMaintenanceSchemas.RUST, 100));
            return snapshotLocked(instance);
        }
    }

    /** Explicit cleaning amounts; useful for authored actions and repair benches. */
    public Optional<ItemMaintenanceSnapshot> clean(UUID instanceId, double dirtRemoved, double rustRemoved) {
        ItemInstance instance = instances.find(instanceId).orElse(null);
        return instance == null ? Optional.empty() : Optional.of(clean(instance, dirtRemoved, rustRemoved));
    }

    public static ItemMaintenanceSnapshot clean(ItemInstance instance, double dirtRemoved, double rustRemoved) {
        requireNonNegativeFinite("dirtRemoved", dirtRemoved);
        requireNonNegativeFinite("rustRemoved", rustRemoved);
        return mutate(instance, target -> {
            addClamped(target, "dirt", -dirtRemoved, 0, maximum(target, BuiltinMaintenanceSchemas.DIRT, 100));
            addClamped(target, "rust", -rustRemoved, 0, maximum(target, BuiltinMaintenanceSchemas.RUST, 100));
        });
    }

    public Optional<ItemMaintenanceSnapshot> lubricate(UUID instanceId, double amount) {
        ItemInstance instance = instances.find(instanceId).orElse(null);
        return instance == null ? Optional.empty() : Optional.of(lubricate(instance, amount));
    }

    public static ItemMaintenanceSnapshot lubricate(ItemInstance instance, double amount) {
        requireNonNegativeFinite("amount", amount);
        return mutate(instance, target -> addClamped(
                target,
                "lubrication",
                amount,
                0,
                maximum(target, BuiltinMaintenanceSchemas.LUBRICATION, 100)
        ));
    }

    public Optional<ItemMaintenanceSnapshot> corrode(UUID instanceId, double amount) {
        ItemInstance instance = instances.find(instanceId).orElse(null);
        return instance == null ? Optional.empty() : Optional.of(corrode(instance, amount));
    }

    public static ItemMaintenanceSnapshot corrode(ItemInstance instance, double amount) {
        requireNonNegativeFinite("amount", amount);
        return mutate(instance, target -> addClamped(
                target,
                "rust",
                amount,
                0,
                maximum(target, BuiltinMaintenanceSchemas.RUST, 100)
        ));
    }

    public Optional<ItemMaintenanceSnapshot> setJammed(UUID instanceId, boolean jammed) {
        ItemInstance instance = instances.find(instanceId).orElse(null);
        return instance == null ? Optional.empty() : Optional.of(setJammed(instance, jammed));
    }

    public static ItemMaintenanceSnapshot setJammed(ItemInstance instance, boolean jammed) {
        return mutate(instance, target -> {
            StateSlot slot = target.state().layout().find("mechanical_jammed");
            if (slot == null) return;
            boolean previous = target.state().getBoolean(slot, false);
            target.state().set(slot, jammed);
            if (!previous && jammed) incrementLong(target, "mechanical_failure_count");
        });
    }

    /** Records completion of an authored maintenance operation without implicitly repairing anything. */
    public Optional<ItemMaintenanceSnapshot> recordMaintenance(UUID instanceId, long nowMillis) {
        ItemInstance instance = instances.find(instanceId).orElse(null);
        return instance == null ? Optional.empty() : Optional.of(recordMaintenance(instance, nowMillis));
    }

    public static ItemMaintenanceSnapshot recordMaintenance(ItemInstance instance, long nowMillis) {
        if (nowMillis < 0) throw new IllegalArgumentException("nowMillis must be >= 0");
        return mutate(instance, target -> {
            incrementLong(target, "maintenance_count");
            StateSlot last = target.state().layout().find("last_maintenance_ms");
            if (last != null) target.state().set(last, nowMillis);
        });
    }

    /**
     * Combined authored service operation. Every amount stays independent and optional; this is a
     * convenience transaction rather than a hidden "repair everything" rule.
     */
    public static ItemMaintenanceSnapshot service(
            ItemInstance instance,
            double dirtRemoved,
            double rustRemoved,
            double lubricationAdded,
            double durabilityRepaired,
            boolean clearJam,
            long nowMillis
    ) {
        requireNonNegativeFinite("dirtRemoved", dirtRemoved);
        requireNonNegativeFinite("rustRemoved", rustRemoved);
        requireNonNegativeFinite("lubricationAdded", lubricationAdded);
        requireNonNegativeFinite("durabilityRepaired", durabilityRepaired);
        if (nowMillis < 0) throw new IllegalArgumentException("nowMillis must be >= 0");
        return mutate(instance, target -> {
            addClamped(target, "dirt", -dirtRemoved, 0, maximum(target, BuiltinMaintenanceSchemas.DIRT, 100));
            addClamped(target, "rust", -rustRemoved, 0, maximum(target, BuiltinMaintenanceSchemas.RUST, 100));
            addClamped(target, "lubrication", lubricationAdded, 0,
                    maximum(target, BuiltinMaintenanceSchemas.LUBRICATION, 100));
            addClamped(target, "durability", durabilityRepaired, 0,
                    maximum(target, BuiltinSimulationSchemas.DURABILITY, 100));
            if (clearJam) {
                StateSlot jam = target.state().layout().find("mechanical_jammed");
                if (jam != null) target.state().set(jam, false);
            }
            incrementLong(target, "maintenance_count");
            StateSlot last = target.state().layout().find("last_maintenance_ms");
            if (last != null) target.state().set(last, nowMillis);
        });
    }

    private static ItemMaintenanceSnapshot mutate(ItemInstance instance, Mutation mutation) {
        Objects.requireNonNull(instance, "instance");
        synchronized (instance) {
            mutation.apply(instance);
            return snapshotLocked(instance);
        }
    }

    private static ItemMaintenanceSnapshot snapshotLocked(ItemInstance instance) {
        double durabilityMax = maximum(instance, BuiltinSimulationSchemas.DURABILITY, 100);
        double wearMax = maximum(instance, WEAR, 100);
        double dirtMax = maximum(instance, BuiltinMaintenanceSchemas.DIRT, 100);
        double rustMax = maximum(instance, BuiltinMaintenanceSchemas.RUST, 100);
        double lubricationMax = maximum(instance, BuiltinMaintenanceSchemas.LUBRICATION, 100);

        double durability = stateNumber(instance, "durability", durabilityMax);
        double wear = stateNumber(instance, "wear", 0);
        double dirt = stateNumber(instance, "dirt", 0);
        double rust = stateNumber(instance, "rust", 0);
        double lubrication = stateNumber(instance, "lubrication", lubricationMax);

        double condition = conditionScore(
                instance,
                ratioHealth(durability, durabilityMax),
                inverseHealth(wear, wearMax),
                inverseHealth(dirt, dirtMax),
                inverseHealth(rust, rustMax),
                ratioHealth(lubrication, lubricationMax)
        );
        boolean jammed = stateBoolean(instance, "mechanical_jammed", false);
        long failureCount = stateLong(instance, "mechanical_failure_count", 0);
        long maintenanceCount = stateLong(instance, "maintenance_count", 0);
        long lastMaintenance = stateLong(instance, "last_maintenance_ms", 0);

        ComponentConfig conditionProfile = component(instance, BuiltinMaintenanceSchemas.CONDITION);
        double minimumOperational = conditionProfile == null ? 0 : directNumber(conditionProfile, "minimumOperational", 5);
        boolean durabilityOperational = !hasState(instance, "durability") || durability > 1.0e-9;
        boolean operational = durabilityOperational && !jammed && condition + 1.0e-9 >= minimumOperational;
        double failureChance = mechanicalFailureChance(instance, wear, wearMax, dirt, dirtMax, rust, rustMax,
                lubrication, lubricationMax, condition);

        return new ItemMaintenanceSnapshot(
                instance.id(),
                instance.definition().source().id(),
                durability, durabilityMax,
                wear, wearMax,
                dirt, dirtMax,
                rust, rustMax,
                lubrication, lubricationMax,
                condition,
                operational,
                jammed,
                failureCount,
                maintenanceCount,
                lastMaintenance,
                failureChance
        );
    }

    private static double conditionScore(
            ItemInstance instance,
            double durabilityHealth,
            double wearHealth,
            double dirtHealth,
            double rustHealth,
            double lubricationHealth
    ) {
        ComponentConfig profile = component(instance, BuiltinMaintenanceSchemas.CONDITION);
        if (profile == null) {
            if (hasState(instance, "durability")) return durabilityHealth * 100.0;
            return 100.0;
        }

        double weighted = 0;
        double totalWeight = 0;
        totalWeight = addMetric(instance, profile, "durability", "durabilityWeight", 0.45, durabilityHealth, totalWeight);
        weighted += metricContribution(instance, profile, "durability", "durabilityWeight", 0.45, durabilityHealth);
        totalWeight = addMetric(instance, profile, "wear", "wearWeight", 0.20, wearHealth, totalWeight);
        weighted += metricContribution(instance, profile, "wear", "wearWeight", 0.20, wearHealth);
        totalWeight = addMetric(instance, profile, "dirt", "dirtWeight", 0.15, dirtHealth, totalWeight);
        weighted += metricContribution(instance, profile, "dirt", "dirtWeight", 0.15, dirtHealth);
        totalWeight = addMetric(instance, profile, "rust", "rustWeight", 0.10, rustHealth, totalWeight);
        weighted += metricContribution(instance, profile, "rust", "rustWeight", 0.10, rustHealth);
        totalWeight = addMetric(instance, profile, "lubrication", "lubricationWeight", 0.10, lubricationHealth, totalWeight);
        weighted += metricContribution(instance, profile, "lubrication", "lubricationWeight", 0.10, lubricationHealth);
        return totalWeight <= 1.0e-12 ? 100.0 : clamp(weighted / totalWeight * 100.0, 0, 100);
    }

    private static double addMetric(
            ItemInstance instance,
            ComponentConfig profile,
            String stateKey,
            String weightKey,
            double fallback,
            double ignoredHealth,
            double total
    ) {
        if (!hasState(instance, stateKey)) return total;
        return total + Math.max(0, directNumber(profile, weightKey, fallback));
    }

    private static double metricContribution(
            ItemInstance instance,
            ComponentConfig profile,
            String stateKey,
            String weightKey,
            double fallback,
            double health
    ) {
        if (!hasState(instance, stateKey)) return 0;
        return Math.max(0, directNumber(profile, weightKey, fallback)) * clamp(health, 0, 1);
    }

    private static double mechanicalFailureChance(
            ItemInstance instance,
            double wear,
            double wearMax,
            double dirt,
            double dirtMax,
            double rust,
            double rustMax,
            double lubrication,
            double lubricationMax,
            double condition
    ) {
        ComponentConfig profile = component(instance, BuiltinMaintenanceSchemas.MECHANICAL_FAILURE);
        if (profile == null) return 0;
        double chance = Math.max(0, directNumber(profile, "baseChance", 0.001));
        chance += Math.max(0, directNumber(profile, "wearFactor", 0.08)) * ratio(wear, wearMax);
        chance += Math.max(0, directNumber(profile, "dirtFactor", 0.12)) * ratio(dirt, dirtMax);
        chance += Math.max(0, directNumber(profile, "rustFactor", 0.15)) * ratio(rust, rustMax);
        chance += Math.max(0, directNumber(profile, "lowLubricationFactor", 0.10)) * (1.0 - ratio(lubrication, lubricationMax));
        chance += Math.max(0, directNumber(profile, "lowConditionFactor", 0.12)) * (1.0 - clamp(condition / 100.0, 0, 1));
        double maxChance = clamp(directNumber(profile, "maxChance", 0.65), 0, 1);
        return clamp(chance, 0, maxChance);
    }

    private static void addClamped(ItemInstance instance, String key, double delta, double min, double max) {
        StateSlot slot = instance.state().layout().find(key);
        if (slot == null || !Double.isFinite(delta)) return;
        double current = instance.state().getDouble(slot, 0);
        instance.state().set(slot, clamp(current + delta, min, max));
    }

    private static void incrementLong(ItemInstance instance, String key) {
        StateSlot slot = instance.state().layout().find(key);
        if (slot == null) return;
        instance.state().set(slot, instance.state().getLong(slot, 0) + 1L);
    }

    private static boolean hasState(ItemInstance instance, String key) {
        return instance.state().layout().find(key) != null;
    }

    private static double stateNumber(ItemInstance instance, String key, double fallback) {
        StateSlot slot = instance.state().layout().find(key);
        return slot == null ? fallback : instance.state().getDouble(slot, fallback);
    }

    private static long stateLong(ItemInstance instance, String key, long fallback) {
        StateSlot slot = instance.state().layout().find(key);
        return slot == null ? fallback : instance.state().getLong(slot, fallback);
    }

    private static boolean stateBoolean(ItemInstance instance, String key, boolean fallback) {
        StateSlot slot = instance.state().layout().find(key);
        return slot == null ? fallback : instance.state().getBoolean(slot, fallback);
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
        if (raw instanceof Number number) return number.doubleValue();
        if (raw == null) return fallback;
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double ratioHealth(double current, double max) {
        return ratio(current, max);
    }

    private static double inverseHealth(double current, double max) {
        return 1.0 - ratio(current, max);
    }

    private static double ratio(double current, double max) {
        if (!Double.isFinite(current) || !Double.isFinite(max) || max <= 0) return 0;
        return clamp(current / max, 0, 1);
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static void requireNonNegativeFinite(String name, double value) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and >= 0");
        }
    }

    @FunctionalInterface
    private interface Mutation {
        void apply(ItemInstance instance);
    }
}
