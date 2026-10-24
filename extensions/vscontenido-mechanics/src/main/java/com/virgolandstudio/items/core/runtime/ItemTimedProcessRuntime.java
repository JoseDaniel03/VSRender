package com.virgolandstudio.items.core.runtime;

import com.virgolandstudio.items.api.component.ComponentConfig;
import com.virgolandstudio.items.core.instance.ItemInstance;
import com.virgolandstudio.items.core.registry.BuiltinSimulationSchemas;
import com.virgolandstudio.items.core.state.StateSlot;

import java.util.Objects;

/**
 * Timestamp-based timed process runtime for one item instance.
 *
 * <p>No autonomous scan is required: callers reconcile the process when the concrete ItemStack is
 * resolved/used. This is important because the authoritative persisted state lives on the ItemStack
 * PDC, not in the in-memory instance cache.</p>
 */
public final class ItemTimedProcessRuntime {
    private ItemTimedProcessRuntime() {}

    public static Snapshot snapshot(ItemInstance instance, long nowMillis) {
        Objects.requireNonNull(instance, "instance");
        synchronized (instance) {
            return snapshotLocked(instance, nowMillis);
        }
    }

    public static Snapshot start(ItemInstance instance, long nowMillis) {
        return start(instance, null, null, nowMillis);
    }

    public static Snapshot start(
            ItemInstance instance,
            String processIdOverride,
            Long durationMillisOverride,
            long nowMillis
    ) {
        Objects.requireNonNull(instance, "instance");
        synchronized (instance) {
            Contract contract = requireContract(instance);
            String processId = processIdOverride == null || processIdOverride.isBlank()
                    ? contract.processId()
                    : processIdOverride.trim();
            long durationMillis = durationMillisOverride == null
                    ? contract.durationMillis()
                    : Math.max(0L, durationMillisOverride);
            long deadline = saturatedAdd(nowMillis, durationMillis);

            set(instance, "process_active", true);
            set(instance, "process_id", processId);
            set(instance, "process_started_ms", nowMillis);
            set(instance, "process_duration_ms", durationMillis);
            set(instance, "process_deadline_ms", deadline);
            return snapshotLocked(instance, nowMillis);
        }
    }

    /** Reconciles elapsed wall-clock time. completedNow is true only on the transition to complete. */
    public static AdvanceResult advance(ItemInstance instance, long nowMillis) {
        Objects.requireNonNull(instance, "instance");
        synchronized (instance) {
            requireContract(instance);
            Snapshot before = snapshotLocked(instance, nowMillis);
            if (!before.active() || nowMillis < before.deadlineMillis()) {
                return new AdvanceResult(before, false);
            }
            set(instance, "process_active", false);
            Snapshot after = snapshotLocked(instance, nowMillis);
            return new AdvanceResult(after, true);
        }
    }

    /** Cancels the process and clears timing so a cancelled process cannot later look completed. */
    public static Snapshot cancel(ItemInstance instance, long nowMillis) {
        Objects.requireNonNull(instance, "instance");
        synchronized (instance) {
            requireContract(instance);
            set(instance, "process_active", false);
            set(instance, "process_started_ms", 0L);
            set(instance, "process_duration_ms", 0L);
            set(instance, "process_deadline_ms", 0L);
            return snapshotLocked(instance, nowMillis);
        }
    }

    private static Snapshot snapshotLocked(ItemInstance instance, long nowMillis) {
        requireContract(instance);
        boolean active = bool(instance, "process_active", false);
        String processId = string(instance, "process_id", "");
        long started = integer(instance, "process_started_ms", 0L);
        long duration = Math.max(0L, integer(instance, "process_duration_ms", 0L));
        long deadline = integer(instance, "process_deadline_ms", 0L);
        double progress = progress(active, started, deadline, duration, nowMillis);
        return new Snapshot(active, processId, started, deadline, duration, progress);
    }

    private static double progress(boolean active, long started, long deadline, long duration, long nowMillis) {
        if (started == 0L && deadline == 0L && duration == 0L) return 0.0;
        if (duration <= 0L) return nowMillis >= deadline ? 1.0 : 0.0;
        if (!active && nowMillis >= deadline) return 1.0;
        double elapsed = Math.max(0.0, (double) nowMillis - (double) started);
        return Math.max(0.0, Math.min(1.0, elapsed / (double) duration));
    }

    private static Contract requireContract(ItemInstance instance) {
        ComponentConfig component = instance.definition().source().components().stream()
                .filter(value -> value.type().equals(BuiltinSimulationSchemas.TIMED_PROCESS))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Item requires virgoland:timed_process"));
        String processId = string(component, "processId", "default");
        double seconds = number(component, "durationSeconds", 5.0);
        if (!Double.isFinite(seconds) || seconds < 0.0) {
            throw new IllegalStateException("virgoland:timed_process.durationSeconds must be finite and >= 0");
        }
        long durationMillis = seconds >= Long.MAX_VALUE / 1000.0
                ? Long.MAX_VALUE
                : Math.max(0L, Math.round(seconds * 1000.0));
        return new Contract(processId, durationMillis);
    }

    private static void set(ItemInstance instance, String key, Object value) {
        StateSlot slot = instance.state().layout().find(key);
        if (slot == null) throw new IllegalStateException("Timed process state is missing: " + key);
        instance.state().set(slot, value);
    }

    private static boolean bool(ItemInstance instance, String key, boolean fallback) {
        StateSlot slot = instance.state().layout().find(key);
        return slot == null ? fallback : instance.state().getBoolean(slot, fallback);
    }

    private static long integer(ItemInstance instance, String key, long fallback) {
        StateSlot slot = instance.state().layout().find(key);
        return slot == null ? fallback : instance.state().getLong(slot, fallback);
    }

    private static String string(ItemInstance instance, String key, String fallback) {
        StateSlot slot = instance.state().layout().find(key);
        if (slot == null) return fallback;
        Object value = instance.state().get(slot);
        return value instanceof String text ? text : fallback;
    }

    private static String string(ComponentConfig component, String key, String fallback) {
        Object value = component.values().get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value).trim();
    }

    private static double number(ComponentConfig component, String key, double fallback) {
        Object value = component.values().get(key);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static long saturatedAdd(long left, long right) {
        if (right <= 0L) return left;
        if (left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    private record Contract(String processId, long durationMillis) {}

    public record Snapshot(
            boolean active,
            String processId,
            long startedAtMillis,
            long deadlineMillis,
            long durationMillis,
            double progress
    ) {}

    public record AdvanceResult(Snapshot snapshot, boolean completedNow) {}
}
