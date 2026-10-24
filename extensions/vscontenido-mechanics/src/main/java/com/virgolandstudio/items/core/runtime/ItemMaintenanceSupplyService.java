package com.virgolandstudio.items.core.runtime;

import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.core.instance.ItemInstance;
import com.virgolandstudio.items.core.registry.BuiltinMaintenanceSchemas;
import com.virgolandstudio.items.core.state.StateSlot;

import java.util.Objects;

/**
 * Atomic Item -> Item maintenance transaction.
 *
 * <p>The supply is ordinary authored data through {@code virgoland:maintenance_supply}. The target
 * owns its independent maintenance dimensions and this service merely applies the declared deltas.
 * If the supply also exposes {@code uses_remaining}, one authored operation consumes the configured
 * amount only after the target can actually change. Reusable tools may omit Uses entirely.</p>
 */
public final class ItemMaintenanceSupplyService {
    private ItemMaintenanceSupplyService() {}

    public static Result apply(ItemInstance target, ItemInstance supply, long nowMillis) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(supply, "supply");
        if (nowMillis < 0) throw new IllegalArgumentException("nowMillis must be >= 0");
        if (target.id().equals(supply.id())) return Result.rejected("same_instance", -1);

        ItemInstance first = target.id().compareTo(supply.id()) <= 0 ? target : supply;
        ItemInstance second = first == target ? supply : target;
        synchronized (first) {
            synchronized (second) {
                return applyLocked(target, supply, nowMillis);
            }
        }
    }

    private static Result applyLocked(ItemInstance target, ItemInstance supply, long nowMillis) {
        ComponentConfig profile = supplyProfile(supply);
        if (profile == null) return Result.rejected("not_maintenance_supply", usesRemaining(supply));

        double dirt = nonNegative(profile, "dirtRemoval", 0);
        double rust = nonNegative(profile, "rustRemoval", 0);
        double lubrication = nonNegative(profile, "lubricationAdded", 0);
        double repair = nonNegative(profile, "durabilityRepair", 0);
        boolean clearJam = bool(profile, "clearJam", false);
        long usesPerOperation = Math.max(0, whole(profile, "usesPerOperation", 1));

        StateSlot uses = supply.state().layout().find("uses_remaining");
        long remaining = uses == null ? -1 : Math.max(0, supply.state().getLong(uses, 0));
        if (uses != null && remaining < usesPerOperation) {
            return Result.rejected("supply_exhausted", remaining);
        }

        ItemMaintenanceSnapshot before = ItemMaintenanceService.snapshot(target);
        boolean useful = dirt > 0 && before.dirt() > 0
                || rust > 0 && before.rust() > 0
                || lubrication > 0 && before.lubrication() + 1.0e-9 < before.lubricationMax()
                || repair > 0 && before.durability() + 1.0e-9 < before.durabilityMax()
                || clearJam && before.jammed();
        if (!useful) return new Result(false, "no_effect", before, before, remaining);

        ItemMaintenanceSnapshot after = ItemMaintenanceService.service(
                target, dirt, rust, lubrication, repair, clearJam, nowMillis
        );
        if (uses != null && usesPerOperation > 0) {
            remaining = Math.max(0, remaining - usesPerOperation);
            supply.state().set(uses, remaining);
        }
        return new Result(true, "applied", before, after, remaining);
    }

    private static ComponentConfig supplyProfile(ItemInstance supply) {
        for (ComponentConfig component : supply.definition().source().components()) {
            if (component.type().equals(BuiltinMaintenanceSchemas.MAINTENANCE_SUPPLY)) return component;
        }
        return null;
    }

    private static long usesRemaining(ItemInstance supply) {
        StateSlot uses = supply.state().layout().find("uses_remaining");
        return uses == null ? -1 : Math.max(0, supply.state().getLong(uses, 0));
    }

    private static double nonNegative(ComponentConfig component, String key, double fallback) {
        Object raw = component.values().get(key);
        double value;
        if (raw instanceof Number number) value = number.doubleValue();
        else {
            try {
                value = raw == null ? fallback : Double.parseDouble(String.valueOf(raw));
            } catch (NumberFormatException ignored) {
                value = fallback;
            }
        }
        if (!Double.isFinite(value)) return Math.max(0, fallback);
        return Math.max(0, value);
    }

    private static long whole(ComponentConfig component, String key, long fallback) {
        Object raw = component.values().get(key);
        if (raw instanceof Number number) return number.longValue();
        try {
            return raw == null ? fallback : Long.parseLong(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean bool(ComponentConfig component, String key, boolean fallback) {
        Object raw = component.values().get(key);
        if (raw == null) return fallback;
        if (raw instanceof Boolean value) return value;
        return Boolean.parseBoolean(String.valueOf(raw));
    }

    public record Result(
            boolean applied,
            String reason,
            ItemMaintenanceSnapshot before,
            ItemMaintenanceSnapshot after,
            long supplyUsesRemaining
    ) {
        private static Result rejected(String reason, long remaining) {
            return new Result(false, reason, null, null, remaining);
        }
    }
}
