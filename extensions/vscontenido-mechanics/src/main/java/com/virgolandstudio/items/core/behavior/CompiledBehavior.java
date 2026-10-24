package com.virgolandstudio.items.core.behavior;

import com.virgolandstudio.items.api.behavior.Trigger;

import java.util.List;

public record CompiledBehavior(
        Trigger trigger,
        List<CompiledCondition> conditions,
        List<CompiledAction> actions
) {
    public CompiledBehavior {
        conditions = List.copyOf(conditions);
        actions = List.copyOf(actions);
    }
}
