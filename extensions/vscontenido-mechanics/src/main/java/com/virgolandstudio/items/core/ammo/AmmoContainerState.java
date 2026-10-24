package com.virgolandstudio.items.core.ammo;

import com.virgolandstudio.items.api.ammo.AmmoStorageMode;

import java.util.Arrays;
import java.util.Objects;

/**
 * Platform-independent mutable storage state for embedded ammo containers.
 * COUNT and RESOURCE remain compact; SLOTS allocates only when the definition explicitly needs addressable chambers.
 */
public final class AmmoContainerState {
    private final AmmoStorageMode mode;
    private final int capacity;
    private double amount;
    private final AmmoSlotState[] slots;
    private int cursor;
    private boolean dirty;

    private AmmoContainerState(AmmoStorageMode mode, int capacity, double initial) {
        this.mode = Objects.requireNonNull(mode, "mode");
        if (capacity < 1) throw new IllegalArgumentException("capacity must be >= 1");
        this.capacity = capacity;
        this.slots = mode == AmmoStorageMode.SLOTS ? new AmmoSlotState[capacity] : null;
        if (slots != null) {
            Arrays.fill(slots, AmmoSlotState.EMPTY);
            int live = clampDiscrete(initial);
            for (int i = 0; i < live; i++) slots[i] = AmmoSlotState.LIVE;
        } else {
            setAmountInternal(initial);
        }
    }

    public static AmmoContainerState create(CompiledAmmoSystem spec, double initial) {
        return new AmmoContainerState(spec.storageMode(), spec.capacity(), initial);
    }

    public static AmmoContainerState count(int capacity, int initial) {
        return new AmmoContainerState(AmmoStorageMode.COUNT, capacity, initial);
    }

    public static AmmoContainerState resource(int capacity, double initial) {
        return new AmmoContainerState(AmmoStorageMode.RESOURCE, capacity, initial);
    }

    public static AmmoContainerState slots(int capacity, int initialLive) {
        return new AmmoContainerState(AmmoStorageMode.SLOTS, capacity, initialLive);
    }

    public static AmmoContainerState restore(
            CompiledAmmoSystem spec,
            double amount,
            AmmoSlotState[] slotStates,
            int cursor
    ) {
        var state = new AmmoContainerState(spec.storageMode(), spec.capacity(), 0.0);
        if (spec.storageMode() == AmmoStorageMode.SLOTS) {
            if (slotStates == null || slotStates.length != spec.capacity()) {
                throw new IllegalArgumentException("slot snapshot does not match capacity");
            }
            for (int i = 0; i < slotStates.length; i++) {
                state.slots[i] = Objects.requireNonNull(slotStates[i], "slot state");
            }
            state.setCursorInternal(cursor);
        } else {
            state.setAmountInternal(amount);
        }
        state.dirty = false;
        return state;
    }

    public AmmoStorageMode mode() { return mode; }
    public int capacity() { return capacity; }
    public int cursor() { return cursor; }
    public boolean dirty() { return dirty; }
    public void markClean() { dirty = false; }

    /** Number of live/usable units. RESOURCE may be fractional; discrete modes are integral. */
    public double available() {
        if (mode != AmmoStorageMode.SLOTS) return amount;
        int count = 0;
        for (AmmoSlotState slot : slots) if (slot == AmmoSlotState.LIVE) count++;
        return count;
    }

    public boolean consumeOne() {
        return switch (mode) {
            case COUNT -> consumeAmount(1.0);
            case RESOURCE -> consumeAmount(1.0);
            case SLOTS -> consumeCurrentSlot();
        };
    }

    public boolean consumeAmount(double units) {
        if (!Double.isFinite(units) || units <= 0.0) throw new IllegalArgumentException("units must be finite and > 0");
        if (mode == AmmoStorageMode.SLOTS) throw new IllegalStateException("Use slot operations for SLOTS storage");
        if (amount + 1.0e-9 < units) return false;
        amount -= units;
        if (amount < 1.0e-9) amount = 0.0;
        dirty = true;
        return true;
    }

    public boolean loadOne() {
        return switch (mode) {
            case COUNT -> addAmount(1.0);
            case RESOURCE -> addAmount(1.0);
            case SLOTS -> loadFirstAvailableSlot();
        };
    }

    public boolean addAmount(double units) {
        if (!Double.isFinite(units) || units <= 0.0) throw new IllegalArgumentException("units must be finite and > 0");
        if (mode == AmmoStorageMode.SLOTS) throw new IllegalStateException("Use slot operations for SLOTS storage");
        if (amount + units > capacity + 1.0e-9) return false;
        amount += units;
        dirty = true;
        return true;
    }

    public void setAmount(double value) {
        if (mode == AmmoStorageMode.SLOTS) throw new IllegalStateException("Use slot operations for SLOTS storage");
        double before = amount;
        setAmountInternal(value);
        if (Double.compare(before, amount) != 0) dirty = true;
    }

    public AmmoSlotState slot(int index) {
        requireSlots();
        return slots[checkIndex(index)];
    }

    public boolean loadSlot(int index) {
        requireSlots();
        int checked = checkIndex(index);
        if (slots[checked] == AmmoSlotState.LIVE) return false;
        slots[checked] = AmmoSlotState.LIVE;
        dirty = true;
        return true;
    }

    public void clearSlot(int index) {
        requireSlots();
        int checked = checkIndex(index);
        if (slots[checked] == AmmoSlotState.EMPTY) return;
        slots[checked] = AmmoSlotState.EMPTY;
        dirty = true;
    }

    public boolean consumeCurrentSlot() {
        requireSlots();
        if (slots[cursor] != AmmoSlotState.LIVE) return false;
        slots[cursor] = AmmoSlotState.SPENT;
        dirty = true;
        return true;
    }

    public void advanceCursor() {
        requireSlots();
        int next = (cursor + 1) % capacity;
        if (next != cursor) {
            cursor = next;
            dirty = true;
        }
    }

    public void setCursor(int cursor) {
        requireSlots();
        if (this.cursor == cursor) return;
        setCursorInternal(cursor);
        dirty = true;
    }

    public AmmoSlotState[] slotsSnapshot() {
        requireSlots();
        return slots.clone();
    }

    private boolean loadFirstAvailableSlot() {
        for (int i = 0; i < capacity; i++) {
            if (slots[i] != AmmoSlotState.LIVE) {
                slots[i] = AmmoSlotState.LIVE;
                dirty = true;
                return true;
            }
        }
        return false;
    }

    private void setAmountInternal(double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > capacity) {
            throw new IllegalArgumentException("amount out of range");
        }
        if (mode == AmmoStorageMode.COUNT && Math.rint(value) != value) {
            throw new IllegalArgumentException("COUNT storage requires an integer amount");
        }
        amount = value;
    }

    private int clampDiscrete(double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > capacity || Math.rint(value) != value) {
            throw new IllegalArgumentException("slot live count out of range");
        }
        return (int) value;
    }

    private void requireSlots() {
        if (mode != AmmoStorageMode.SLOTS) throw new IllegalStateException("Container does not use SLOTS storage");
    }

    private int checkIndex(int index) {
        if (index < 0 || index >= capacity) throw new IndexOutOfBoundsException(index);
        return index;
    }

    private void setCursorInternal(int cursor) {
        if (cursor < 0 || cursor >= capacity) throw new IllegalArgumentException("cursor out of range");
        this.cursor = cursor;
    }
}
