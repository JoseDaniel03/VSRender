package com.virgolandstudio.items.core.runtime;

import com.virgolandstudio.items.api.state.StateType;
import com.virgolandstudio.items.core.instance.ItemInstance;
import com.virgolandstudio.items.core.instance.ItemInstanceRegistry;
import com.virgolandstudio.items.core.state.StateSlot;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Timestamp-backed generic process runtime.
 *
 * <p>No global ticker is required: elapsed time is derived from a persisted
 * deadline whenever the process is inspected or completed. This makes simple
 * cooking, developing, charging and crafting-like processes restart-friendly
 * without writing progress every tick.</p>
 */
public final class ItemTimedProcessService {
    public enum StartResult {
        STARTED,
        ALREADY_RUNNING,
        NOT_FOUND,
        NOT_SUPPORTED,
        INVALID_DURATION
    }

    private final ItemInstanceRegistry instances;
    private final LongSupplier clock;

    public ItemTimedProcessService(ItemInstanceRegistry instances) {
        this(instances, System::currentTimeMillis);
    }

    public ItemTimedProcessService(ItemInstanceRegistry instances, LongSupplier clock) {
        this.instances = Objects.requireNonNull(instances, "instances");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public StartResult start(UUID instanceId, String processId, long durationMillis) {
        if (durationMillis < 0 || processId == null || processId.isBlank()) return StartResult.INVALID_DURATION;
        ItemInstance instance = instances.find(instanceId).orElse(null);
        if (instance == null) return StartResult.NOT_FOUND;

        synchronized (instance) {
            Slots slots = slots(instance);
            if (slots == null) return StartResult.NOT_SUPPORTED;
            if (instance.state().getBoolean(slots.active(), false)) return StartResult.ALREADY_RUNNING;

            long now = clock.getAsLong();
            long deadline;
            try {
                deadline = Math.addExact(now, durationMillis);
            } catch (ArithmeticException ex) {
                return StartResult.INVALID_DURATION;
            }

            instance.state().set(slots.processId(), processId);
            instance.state().set(slots.deadline(), deadline);
            instance.state().set(slots.active(), true);
            return StartResult.STARTED;
        }
    }

    public Optional<ItemTimedProcessSnapshot> inspect(UUID instanceId) {
        ItemInstance instance = instances.find(instanceId).orElse(null);
        if (instance == null) return Optional.empty();

        synchronized (instance) {
            Slots slots = slots(instance);
            if (slots == null) return Optional.empty();
            return Optional.of(snapshotLocked(instance, slots, clock.getAsLong()));
        }
    }

    /** Marks a due process complete. Returns true only for the transition DUE -> IDLE. */
    public boolean completeIfDue(UUID instanceId) {
        ItemInstance instance = instances.find(instanceId).orElse(null);
        if (instance == null) return false;

        synchronized (instance) {
            Slots slots = slots(instance);
            if (slots == null || !instance.state().getBoolean(slots.active(), false)) return false;
            long deadline = instance.state().getLong(slots.deadline(), 0L);
            if (clock.getAsLong() < deadline) return false;
            clear(instance, slots);
            return true;
        }
    }

    public boolean cancel(UUID instanceId) {
        ItemInstance instance = instances.find(instanceId).orElse(null);
        if (instance == null) return false;

        synchronized (instance) {
            Slots slots = slots(instance);
            if (slots == null || !instance.state().getBoolean(slots.active(), false)) return false;
            clear(instance, slots);
            return true;
        }
    }

    private static ItemTimedProcessSnapshot snapshotLocked(ItemInstance instance, Slots slots, long now) {
        boolean active = instance.state().getBoolean(slots.active(), false);
        String processId = String.valueOf(instance.state().get(slots.processId()));
        long deadline = instance.state().getLong(slots.deadline(), 0L);
        if (!active) {
            return new ItemTimedProcessSnapshot(instance.id(), processId, ItemTimedProcessSnapshot.Status.IDLE, 0L, 0L);
        }
        long remaining = Math.max(0L, deadline - now);
        ItemTimedProcessSnapshot.Status status = now >= deadline
                ? ItemTimedProcessSnapshot.Status.DUE
                : ItemTimedProcessSnapshot.Status.RUNNING;
        return new ItemTimedProcessSnapshot(instance.id(), processId, status, deadline, remaining);
    }

    private static void clear(ItemInstance instance, Slots slots) {
        instance.state().set(slots.active(), false);
        instance.state().set(slots.processId(), "");
        instance.state().set(slots.deadline(), 0L);
    }

    private static Slots slots(ItemInstance instance) {
        StateSlot active = instance.state().layout().find("process_active");
        StateSlot processId = instance.state().layout().find("process_id");
        StateSlot deadline = instance.state().layout().find("process_deadline_ms");
        if (active == null || processId == null || deadline == null) return null;
        if (active.type() != StateType.BOOLEAN || processId.type() != StateType.STRING || deadline.type() != StateType.INTEGER) return null;
        return new Slots(active, processId, deadline);
    }

    private record Slots(StateSlot active, StateSlot processId, StateSlot deadline) {}
}
