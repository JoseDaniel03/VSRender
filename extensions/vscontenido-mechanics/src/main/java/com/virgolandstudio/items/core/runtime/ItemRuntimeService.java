package com.virgolandstudio.items.core.runtime;

import com.virgolandstudio.items.api.state.StateType;
import com.virgolandstudio.items.core.instance.ItemInstance;
import com.virgolandstudio.items.core.instance.ItemInstanceRegistry;
import com.virgolandstudio.items.core.state.StateSlot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.virgolandstudio.items.core.runtime.ItemRuntimeMutationResult.Status.APPLIED;
import static com.virgolandstudio.items.core.runtime.ItemRuntimeMutationResult.Status.CAPACITY_EXCEEDED;
import static com.virgolandstudio.items.core.runtime.ItemRuntimeMutationResult.Status.INSUFFICIENT;
import static com.virgolandstudio.items.core.runtime.ItemRuntimeMutationResult.Status.INVALID_AMOUNT;
import static com.virgolandstudio.items.core.runtime.ItemRuntimeMutationResult.Status.NOT_FOUND;
import static com.virgolandstudio.items.core.runtime.ItemRuntimeMutationResult.Status.NO_CHANGE;
import static com.virgolandstudio.items.core.runtime.ItemRuntimeMutationResult.Status.TYPE_MISMATCH;
import static com.virgolandstudio.items.core.runtime.ItemRuntimeMutationResult.Status.UNKNOWN_KEY;

/**
 * Atomic provider-neutral mutation layer for generic item simulation state.
 *
 * <p>Operations synchronize on the ItemInstance itself. This avoids lost updates
 * between callers using this service and gives integrations one safe place to
 * mutate counters, fluids, temperature, uses and other schema-derived state.
 * Native weapon systems can migrate to the same service incrementally.</p>
 */
public final class ItemRuntimeService {
    private final ItemInstanceRegistry instances;

    public ItemRuntimeService(ItemInstanceRegistry instances) {
        this.instances = Objects.requireNonNull(instances, "instances");
    }

    public Optional<ItemRuntimeSnapshot> snapshot(UUID instanceId) {
        return instances.find(instanceId).map(instance -> {
            synchronized (instance) {
                Map<String, Object> state = new LinkedHashMap<>();
                for (StateSlot slot : instance.state().layout().slots()) {
                    state.put(slot.key(), instance.state().get(slot));
                }
                return new ItemRuntimeSnapshot(
                        instance.id(),
                        instance.definition().source().id(),
                        state
                );
            }
        });
    }

    public ItemRuntimeMutationResult set(UUID instanceId, String key, Object value) {
        ItemInstance instance = instances.find(instanceId).orElse(null);
        if (instance == null) return ItemRuntimeMutationResult.failure(NOT_FOUND, "Unknown item instance: " + instanceId);

        synchronized (instance) {
            StateSlot slot = instance.state().layout().find(key);
            if (slot == null) return ItemRuntimeMutationResult.failure(UNKNOWN_KEY, "Unknown state key: " + key);
            Object previous = instance.state().get(slot);
            try {
                instance.state().set(slot, value);
            } catch (IllegalArgumentException ex) {
                return ItemRuntimeMutationResult.failure(TYPE_MISMATCH, ex.getMessage());
            }
            Object current = instance.state().get(slot);
            return new ItemRuntimeMutationResult(
                    Objects.equals(previous, current) ? NO_CHANGE : APPLIED,
                    previous,
                    current,
                    ""
            );
        }
    }

    public ItemRuntimeMutationResult add(UUID instanceId, String key, double delta) {
        if (!Double.isFinite(delta)) return ItemRuntimeMutationResult.failure(INVALID_AMOUNT, "Delta must be finite");
        ItemInstance instance = instances.find(instanceId).orElse(null);
        if (instance == null) return ItemRuntimeMutationResult.failure(NOT_FOUND, "Unknown item instance: " + instanceId);

        synchronized (instance) {
            StateSlot slot = instance.state().layout().find(key);
            if (slot == null) return ItemRuntimeMutationResult.failure(UNKNOWN_KEY, "Unknown state key: " + key);
            if (!numeric(slot)) return ItemRuntimeMutationResult.failure(TYPE_MISMATCH, "State field is not numeric: " + key);
            if (slot.type() == StateType.INTEGER && !whole(delta)) {
                return ItemRuntimeMutationResult.failure(TYPE_MISMATCH, "Integer state requires a whole-number delta: " + key);
            }
            Object previous = instance.state().get(slot);
            instance.state().add(slot, delta);
            Object current = instance.state().get(slot);
            return new ItemRuntimeMutationResult(
                    Objects.equals(previous, current) ? NO_CHANGE : APPLIED,
                    previous,
                    current,
                    ""
            );
        }
    }

    public ItemRuntimeMutationResult consume(UUID instanceId, String key, double amount) {
        if (!validPositive(amount)) return ItemRuntimeMutationResult.failure(INVALID_AMOUNT, "Amount must be finite and > 0");
        ItemInstance instance = instances.find(instanceId).orElse(null);
        if (instance == null) return ItemRuntimeMutationResult.failure(NOT_FOUND, "Unknown item instance: " + instanceId);

        synchronized (instance) {
            StateSlot slot = instance.state().layout().find(key);
            if (slot == null) return ItemRuntimeMutationResult.failure(UNKNOWN_KEY, "Unknown state key: " + key);
            if (!numeric(slot)) return ItemRuntimeMutationResult.failure(TYPE_MISMATCH, "State field is not numeric: " + key);
            if (slot.type() == StateType.INTEGER && !whole(amount)) {
                return ItemRuntimeMutationResult.failure(TYPE_MISMATCH, "Integer state requires a whole-number amount: " + key);
            }
            double current = numericValue(instance, slot);
            if (current + 1.0e-9 < amount) {
                return new ItemRuntimeMutationResult(INSUFFICIENT, current, current, "Insufficient value in " + key);
            }
            Object previous = instance.state().get(slot);
            setNumeric(instance, slot, current - amount);
            return new ItemRuntimeMutationResult(APPLIED, previous, instance.state().get(slot), "");
        }
    }

    /**
     * Atomically transfers a numeric quantity between two item instances.
     * Useful for fluids, charges, fuel and similar resources. The destination
     * capacity is supplied by the caller because capacity remains component data,
     * not hidden state.
     */
    public ItemRuntimeMutationResult transfer(
            UUID fromInstanceId,
            String fromKey,
            UUID toInstanceId,
            String toKey,
            double amount,
            double destinationCapacity
    ) {
        if (!validPositive(amount)) return ItemRuntimeMutationResult.failure(INVALID_AMOUNT, "Amount must be finite and > 0");
        if (!Double.isFinite(destinationCapacity) || destinationCapacity < 0) {
            return ItemRuntimeMutationResult.failure(INVALID_AMOUNT, "Destination capacity must be finite and >= 0");
        }

        ItemInstance from = instances.find(fromInstanceId).orElse(null);
        ItemInstance to = instances.find(toInstanceId).orElse(null);
        if (from == null || to == null) {
            return ItemRuntimeMutationResult.failure(NOT_FOUND, "Source or destination item instance was not found");
        }

        if (from == to) {
            synchronized (from) {
                return transferLocked(from, fromKey, to, toKey, amount, destinationCapacity);
            }
        }

        ItemInstance first = compare(from.id(), to.id()) <= 0 ? from : to;
        ItemInstance second = first == from ? to : from;
        synchronized (first) {
            synchronized (second) {
                return transferLocked(from, fromKey, to, toKey, amount, destinationCapacity);
            }
        }
    }

    private static ItemRuntimeMutationResult transferLocked(
            ItemInstance from,
            String fromKey,
            ItemInstance to,
            String toKey,
            double amount,
            double destinationCapacity
    ) {
        StateSlot fromSlot = from.state().layout().find(fromKey);
        StateSlot toSlot = to.state().layout().find(toKey);
        if (fromSlot == null || toSlot == null) {
            return ItemRuntimeMutationResult.failure(UNKNOWN_KEY, "Source or destination state key was not found");
        }
        if (!numeric(fromSlot) || !numeric(toSlot)) {
            return ItemRuntimeMutationResult.failure(TYPE_MISMATCH, "Resource transfer requires numeric state fields");
        }
        if ((fromSlot.type() == StateType.INTEGER || toSlot.type() == StateType.INTEGER) && !whole(amount)) {
            return ItemRuntimeMutationResult.failure(TYPE_MISMATCH, "Integer resource transfer requires a whole-number amount");
        }

        double fromValue = numericValue(from, fromSlot);
        double toValue = numericValue(to, toSlot);
        if (fromValue + 1.0e-9 < amount) {
            return new ItemRuntimeMutationResult(INSUFFICIENT, fromValue, fromValue, "Insufficient source resource");
        }
        if (toValue + amount > destinationCapacity + 1.0e-9) {
            return new ItemRuntimeMutationResult(CAPACITY_EXCEEDED, toValue, toValue, "Destination capacity exceeded");
        }

        Object previousSource = from.state().get(fromSlot);
        setNumeric(from, fromSlot, fromValue - amount);
        setNumeric(to, toSlot, toValue + amount);
        return new ItemRuntimeMutationResult(APPLIED, previousSource, from.state().get(fromSlot), "");
    }

    private static boolean numeric(StateSlot slot) {
        return slot.type() == StateType.INTEGER || slot.type() == StateType.DOUBLE;
    }

    private static double numericValue(ItemInstance instance, StateSlot slot) {
        return slot.type() == StateType.INTEGER
                ? instance.state().getLong(slot, 0L)
                : instance.state().getDouble(slot, 0.0);
    }

    private static void setNumeric(ItemInstance instance, StateSlot slot, double value) {
        if (slot.type() == StateType.INTEGER) instance.state().set(slot, (long) value);
        else instance.state().set(slot, value);
    }

    private static boolean validPositive(double amount) {
        return Double.isFinite(amount) && amount > 0;
    }

    private static boolean whole(double value) {
        return Math.rint(value) == value;
    }

    private static int compare(UUID left, UUID right) {
        int msb = Long.compareUnsigned(left.getMostSignificantBits(), right.getMostSignificantBits());
        return msb != 0 ? msb : Long.compareUnsigned(left.getLeastSignificantBits(), right.getLeastSignificantBits());
    }
}
