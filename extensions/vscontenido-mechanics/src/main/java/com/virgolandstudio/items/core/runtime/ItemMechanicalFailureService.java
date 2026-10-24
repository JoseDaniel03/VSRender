package com.virgolandstudio.items.core.runtime;

import com.virgolandstudio.items.core.instance.ItemInstance;
import com.virgolandstudio.items.core.instance.ItemInstanceRegistry;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Generic mechanical-failure evaluator for any stateful item.
 *
 * <p>The service does not decide when an item is used: callers invoke it only after a confirmed
 * successful semantic use. Failure probability comes entirely from the authored maintenance
 * profile, so firearms, tools and future machinery can share the same runtime without item-specific
 * classes.</p>
 */
public final class ItemMechanicalFailureService {
    private final ItemInstanceRegistry instances;
    private final ItemMaintenanceService maintenance;
    private final DoubleSupplier random;

    public ItemMechanicalFailureService(ItemInstanceRegistry instances) {
        this(instances, () -> ThreadLocalRandom.current().nextDouble());
    }

    /** Public deterministic constructor used by tests and simulation tooling. */
    public ItemMechanicalFailureService(ItemInstanceRegistry instances, DoubleSupplier random) {
        this.instances = Objects.requireNonNull(instances, "instances");
        this.maintenance = new ItemMaintenanceService(instances);
        this.random = Objects.requireNonNull(random, "random");
    }

    /**
     * Evaluates one failure check from the item's current maintenance snapshot.
     * Already-jammed items never roll again and therefore never double-count the same failure.
     */
    public Optional<ItemMechanicalFailureResult> evaluate(UUID instanceId) {
        ItemInstance instance = instances.find(instanceId).orElse(null);
        if (instance == null) return Optional.empty();

        synchronized (instance) {
            ItemMaintenanceSnapshot before = maintenance.snapshot(instanceId).orElseThrow();
            double chance = clamp(before.mechanicalFailureChance(), 0.0, 1.0);
            if (before.jammed()) {
                return Optional.of(new ItemMechanicalFailureResult(
                        instanceId, chance, Double.NaN, false, false, true, before
                ));
            }
            if (chance <= 0.0) {
                return Optional.of(new ItemMechanicalFailureResult(
                        instanceId, 0.0, Double.NaN, false, false, false, before
                ));
            }

            double roll = random.getAsDouble();
            if (!Double.isFinite(roll) || roll < 0.0 || roll >= 1.0) {
                throw new IllegalStateException("Mechanical failure random supplier must return a finite value in [0, 1)");
            }
            if (roll >= chance) {
                return Optional.of(new ItemMechanicalFailureResult(
                        instanceId, chance, roll, true, false, false, before
                ));
            }

            ItemMaintenanceSnapshot after = maintenance.setJammed(instanceId, true).orElseThrow();
            return Optional.of(new ItemMechanicalFailureResult(
                    instanceId, chance, roll, true, true, false, after
            ));
        }
    }

    /** Clears only the jam flag. Cleaning, repair and lubrication remain separate operations. */
    public Optional<ItemMaintenanceSnapshot> clearJam(UUID instanceId) {
        return maintenance.setJammed(instanceId, false);
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }
}
