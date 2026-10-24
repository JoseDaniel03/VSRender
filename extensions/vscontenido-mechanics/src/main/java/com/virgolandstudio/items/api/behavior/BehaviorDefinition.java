package com.virgolandstudio.items.api.behavior;

import java.util.List;
import java.util.Objects;

public record BehaviorDefinition(
        Trigger trigger,
        List<ConditionConfig> conditions,
        List<ActionConfig> actions
) {
    public BehaviorDefinition {
        Objects.requireNonNull(trigger, "trigger");
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
