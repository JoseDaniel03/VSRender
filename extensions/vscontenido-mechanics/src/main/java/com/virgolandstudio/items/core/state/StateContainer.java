package com.virgolandstudio.items.core.state;

import com.virgolandstudio.items.api.state.StateType;

import java.util.LinkedHashMap;
import java.util.Map;

public final class StateContainer {
    private final StateLayout layout;
    private final Object[] values;
    private boolean dirty;

    public StateContainer(StateLayout layout) {
        this.layout = layout;
        this.values = new Object[layout.size()];
        for (StateSlot slot : layout.slots()) values[slot.index()] = slot.defaultValue();
    }

    public Object get(StateSlot slot) { return values[slot.index()]; }
    public Object get(String key) { return get(layout.require(key)); }

    public long getLong(StateSlot slot, long fallback) {
        Object value = get(slot);
        return value instanceof Number number ? number.longValue() : fallback;
    }

    public double getDouble(StateSlot slot, double fallback) {
        Object value = get(slot);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    public boolean getBoolean(StateSlot slot, boolean fallback) {
        Object value = get(slot);
        return value instanceof Boolean bool ? bool : fallback;
    }

    public void set(StateSlot slot, Object value) {
        Object normalized = normalize(slot.type(), value);
        Object previous = values[slot.index()];
        if (java.util.Objects.equals(previous, normalized)) return;
        values[slot.index()] = normalized;
        if (slot.persistent()) dirty = true;
    }

    public void set(String key, Object value) { set(layout.require(key), value); }

    public void add(StateSlot slot, double delta) {
        switch (slot.type()) {
            case INTEGER -> set(slot, getLong(slot, 0L) + (long) delta);
            case DOUBLE -> set(slot, getDouble(slot, 0.0) + delta);
            default -> throw new IllegalArgumentException("State field is not numeric: " + slot.key());
        }
    }

    public boolean dirty() { return dirty; }
    public void markClean() { dirty = false; }
    public StateLayout layout() { return layout; }

    public Map<String, Object> persistentSnapshot() {
        var out = new LinkedHashMap<String, Object>();
        for (StateSlot slot : layout.slots()) if (slot.persistent()) out.put(slot.key(), values[slot.index()]);
        return java.util.Collections.unmodifiableMap(out);
    }

    /** Strict restore used when the caller expects the persisted layout to match exactly enough for every known key. */
    public void restore(Map<String, Object> snapshot) {
        for (var entry : snapshot.entrySet()) {
            StateSlot slot = layout.find(entry.getKey());
            if (slot != null && slot.persistent()) set(slot, entry.getValue());
        }
        dirty = false;
    }

    /**
     * Restores a snapshot written by an older definition into this layout.
     *
     * <p>Fields that were removed are ignored, newly added fields keep their authored defaults and
     * incompatible type changes reset only that field instead of making the whole item unreadable.
     * The returned report tells the Paper codec whether the PDC payload should be rewritten in the
     * current layout.</p>
     */
    public StateRestoreReport restoreCompatible(Map<String, Object> snapshot) {
        int restored = 0;
        int missingInLayout = 0;
        int incompatible = 0;

        for (var entry : snapshot.entrySet()) {
            StateSlot slot = layout.find(entry.getKey());
            if (slot == null || !slot.persistent()) {
                missingInLayout++;
                continue;
            }
            try {
                set(slot, entry.getValue());
                restored++;
            } catch (IllegalArgumentException ignored) {
                incompatible++;
            }
        }

        int persistentSlots = 0;
        int presentSlots = 0;
        for (StateSlot slot : layout.slots()) {
            if (!slot.persistent()) continue;
            persistentSlots++;
            if (snapshot.containsKey(slot.key())) presentSlots++;
        }
        int defaulted = Math.max(0, persistentSlots - presentSlots);
        dirty = false;
        return new StateRestoreReport(restored, missingInLayout, incompatible, defaulted);
    }

    private static Object normalize(StateType type, Object value) {
        if (value == null) return null;
        return switch (type) {
            case INTEGER -> {
                if (!(value instanceof Number number)) throw typeError(type, value);
                yield number.longValue();
            }
            case DOUBLE -> {
                if (!(value instanceof Number number)) throw typeError(type, value);
                yield number.doubleValue();
            }
            case BOOLEAN -> {
                if (!(value instanceof Boolean)) throw typeError(type, value);
                yield value;
            }
            case STRING -> {
                if (!(value instanceof String)) throw typeError(type, value);
                yield value;
            }
        };
    }

    private static IllegalArgumentException typeError(StateType type, Object value) {
        return new IllegalArgumentException("Value " + value + " is not compatible with " + type);
    }
}
