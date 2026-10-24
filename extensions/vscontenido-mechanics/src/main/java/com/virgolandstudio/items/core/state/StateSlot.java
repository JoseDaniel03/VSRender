package com.virgolandstudio.items.core.state;

import com.virgolandstudio.items.api.state.StateType;

public record StateSlot(int index, String key, StateType type, Object defaultValue, boolean persistent) {}
