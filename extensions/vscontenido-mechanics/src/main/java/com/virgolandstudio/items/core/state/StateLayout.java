package com.virgolandstudio.items.core.state;

import com.virgolandstudio.items.api.state.StateFieldDefinition;
import com.virgolandstudio.items.core.compile.ValidationException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StateLayout {
    private final List<StateSlot> slots;
    private final Map<String, StateSlot> byKey;

    private StateLayout(List<StateSlot> slots, Map<String, StateSlot> byKey) {
        this.slots = List.copyOf(slots);
        this.byKey = Map.copyOf(byKey);
    }

    public static StateLayout compile(List<StateFieldDefinition> definitions) {
        var errors = new ArrayList<String>();
        var slots = new ArrayList<StateSlot>(definitions.size());
        var byKey = new LinkedHashMap<String, StateSlot>();
        for (StateFieldDefinition field : definitions) {
            if (byKey.containsKey(field.key())) {
                errors.add("Duplicate state field: " + field.key());
                continue;
            }
            var slot = new StateSlot(slots.size(), field.key(), field.type(), normalize(field), field.persistent());
            slots.add(slot);
            byKey.put(field.key(), slot);
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
        return new StateLayout(slots, byKey);
    }

    private static Object normalize(StateFieldDefinition field) {
        Object value = field.defaultValue();
        if (value == null) return null;
        return switch (field.type()) {
            case INTEGER -> ((Number) value).longValue();
            case DOUBLE -> ((Number) value).doubleValue();
            case BOOLEAN, STRING -> value;
        };
    }


    public StateSlot require(String key) {
        StateSlot slot = byKey.get(key);
        if (slot == null) throw new IllegalArgumentException("Unknown state key: " + key);
        return slot;
    }

    public StateSlot find(String key) { return byKey.get(key); }
    public int size() { return slots.size(); }
    public List<StateSlot> slots() { return slots; }
}
